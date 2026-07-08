package com.pgsa.trailers.service.routing.providers;

import com.pgsa.trailers.service.routing.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@Order(999) // Lowest priority - last resort
public class FallbackRoutingProvider implements RoutingProvider {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double ROAD_FACTOR = 1.3; // 30% more than straight line for short distances
    
    @Override
    public String name() {
        return "fallback";
    }

    @Override
    public boolean supports(String vehicleType) {
        return true; // Always available
    }

    @Override
    public RoutingResult calculate(Coordinates origin, 
                                   Coordinates destination, 
                                   String vehicleType,
                                   Map<String, Object> context) {

        String originLabel = (String) context.getOrDefault("originLabel", "origin");
        String destLabel = (String) context.getOrDefault("destLabel", "destination");
        
        log.info("🔄 Using fallback routing for {} -> {} (vehicle: {})", 
            originLabel, destLabel, vehicleType);

        // Calculate straight-line distance using Haversine formula
        double straightDistance = haversineDistance(
            origin.getLat(), origin.getLng(),
            destination.getLat(), destination.getLng()
        );

        // Apply road factor to estimate actual driving distance
        // For short distances, road factor is higher (more turns, local roads)
        double roadFactor = straightDistance < 50 ? 1.5 : ROAD_FACTOR;
        double roadDistance = straightDistance * roadFactor;
        
        // Average speed based on vehicle type and distance
        double avgSpeed = getAverageSpeed(vehicleType, roadDistance);
        double duration = roadDistance / avgSpeed;

        log.info("✅ Fallback route calculated: {} km straight, {} km road, {} hours", 
            String.format("%.1f", straightDistance),
            String.format("%.1f", roadDistance),
            String.format("%.1f", duration));

        return new RoutingResult(
            origin,
            destination,
            BigDecimal.valueOf(roadDistance),
            BigDecimal.valueOf(duration),
            name(),
            null // No geometry for fallback
        );
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    private double getAverageSpeed(String vehicleType, double distanceKm) {
        // Base speed by vehicle type
        double baseSpeed;
        if (vehicleType == null) {
            baseSpeed = 60.0;
        } else {
            String type = vehicleType.toUpperCase();
            if (type.contains("TRUCK") || type.contains("HGV") || type.contains("HEAVY")) {
                baseSpeed = 60.0;
            } else if (type.contains("VAN") || type.contains("MEDIUM")) {
                baseSpeed = 70.0;
            } else if (type.contains("CAR") || type.contains("LIGHT")) {
                baseSpeed = 80.0;
            } else {
                baseSpeed = 65.0;
            }
        }
        
        // Adjust for distance (shorter trips have lower average speed due to city driving)
        if (distanceKm < 50) {
            return baseSpeed * 0.6; // City driving, lower average speed
        } else if (distanceKm < 200) {
            return baseSpeed * 0.8;
        } else {
            return baseSpeed; // Highway driving
        }
    }
}
