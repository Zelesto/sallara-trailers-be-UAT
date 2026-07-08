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
    private static final double ROAD_FACTOR = 1.4; // 40% more than straight line
    
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

        double distance = haversineDistance(
            origin.getLat(), origin.getLng(),
            destination.getLat(), destination.getLng()
        );

        // Apply road factor to estimate actual driving distance
        double roadDistance = distance * ROAD_FACTOR;
        
        // Average speed based on vehicle type
        double avgSpeed = getAverageSpeed(vehicleType);
        double duration = roadDistance / avgSpeed;

        log.info("✅ Fallback route calculated: {} km, {} hours", 
            Math.round(roadDistance), Math.round(duration));

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

    private double getAverageSpeed(String vehicleType) {
        if (vehicleType == null) return 60.0;
        
        String type = vehicleType.toUpperCase();
        
        // Map vehicle types to average speeds (km/h)
        if (type.contains("TRUCK") || type.contains("HGV") || type.contains("HEAVY")) {
            return 60.0; // Trucks average 60 km/h
        } else if (type.contains("VAN") || type.contains("MEDIUM")) {
            return 70.0; // Vans average 70 km/h
        } else if (type.contains("CAR") || type.contains("LIGHT")) {
            return 80.0; // Cars average 80 km/h
        } else {
            return 65.0; // Default
        }
    }
}
