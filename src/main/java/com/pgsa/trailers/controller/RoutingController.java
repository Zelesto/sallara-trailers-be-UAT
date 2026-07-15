package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.RouteRequest;
import com.pgsa.trailers.dto.RouteResponse;
import com.pgsa.trailers.service.routing.RoutingEngine;
import com.pgsa.trailers.service.routing.RoutingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/routing")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RoutingController {

    private final RoutingEngine routingEngine;

    /**
     * Calculate route between two locations (addresses)
     */
    @PostMapping("/route")
    public ResponseEntity<RouteResponse> calculateRoute(@RequestBody RouteRequest request) {
        log.info("📍 Calculating route from: {} to: {}", request.getOrigin(), request.getDestination());
        
        try {
            String vehicleType = request.getVehicleType() != null ? request.getVehicleType() : "TRUCK";
            
            RoutingResult result = routingEngine.calculateRoute(
                request.getOrigin(), 
                request.getDestination(),
                vehicleType
            );
            
            RouteResponse response = new RouteResponse();
            response.setDistanceKm(result.getDistanceKm().doubleValue());
            response.setDurationMinutes(result.getDurationHours().doubleValue() * 60); // hours to minutes
            response.setOrigin(request.getOrigin());
            response.setDestination(request.getDestination());
            response.setProvider(result.getProvider());
            response.setSuccess(true);
            response.setSummary(String.format("Distance: %.1f km, Duration: %.0f min (via %s)", 
                result.getDistanceKm().doubleValue(), 
                result.getDurationHours().doubleValue() * 60,
                result.getProvider()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error calculating route: {}", e.getMessage(), e);
            
            RouteResponse errorResponse = new RouteResponse();
            errorResponse.setSuccess(false);
            errorResponse.setOrigin(request.getOrigin());
            errorResponse.setDestination(request.getDestination());
            errorResponse.setErrorMessage(e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Calculate distance only (simplified)
     */
    @PostMapping("/distance")
    public ResponseEntity<RouteResponse> calculateDistance(@RequestBody RouteRequest request) {
        log.info("📍 Calculating distance from: {} to: {}", request.getOrigin(), request.getDestination());
        
        try {
            String vehicleType = request.getVehicleType() != null ? request.getVehicleType() : "TRUCK";
            
            RoutingResult result = routingEngine.calculateRoute(
                request.getOrigin(), 
                request.getDestination(),
                vehicleType
            );
            
            RouteResponse response = new RouteResponse();
            response.setDistanceKm(result.getDistanceKm().doubleValue());
            response.setDurationMinutes(result.getDurationHours().doubleValue() * 60);
            response.setOrigin(request.getOrigin());
            response.setDestination(request.getDestination());
            response.setProvider(result.getProvider());
            response.setSuccess(true);
            response.setSummary(String.format("Distance: %.1f km (via %s)", 
                result.getDistanceKm().doubleValue(),
                result.getProvider()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error calculating distance: {}", e.getMessage(), e);
            
            RouteResponse errorResponse = new RouteResponse();
            errorResponse.setSuccess(false);
            errorResponse.setOrigin(request.getOrigin());
            errorResponse.setDestination(request.getDestination());
            errorResponse.setErrorMessage(e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Calculate route using direct coordinates
     */
    @PostMapping("/direct")
    public ResponseEntity<RouteResponse> calculateDirectRoute(@RequestBody DirectRouteRequest request) {
        log.info("📍 Calculating direct route from: {},{} to: {},{}", 
            request.getStartLat(), request.getStartLng(),
            request.getEndLat(), request.getEndLng());
        
        try {
            String vehicleType = request.getVehicleType() != null ? request.getVehicleType() : "TRUCK";
            
            RoutingResult result = routingEngine.calculateRouteDirect(
                request.getStartLat(),
                request.getStartLng(),
                request.getEndLat(),
                request.getEndLng(),
                vehicleType
            );
            
            RouteResponse response = new RouteResponse();
            response.setDistanceKm(result.getDistanceKm().doubleValue());
            response.setDurationMinutes(result.getDurationHours().doubleValue() * 60);
            response.setOrigin("Direct coordinates");
            response.setDestination("Direct coordinates");
            response.setProvider(result.getProvider());
            response.setSuccess(true);
            response.setSummary(String.format("Distance: %.1f km, Duration: %.0f min (via %s)", 
                result.getDistanceKm().doubleValue(), 
                result.getDurationHours().doubleValue() * 60,
                result.getProvider()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error calculating direct route: {}", e.getMessage(), e);
            
            RouteResponse errorResponse = new RouteResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage(e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get provider status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getProviderStatus() {
        // This would need to be implemented in RoutingEngine
        return ResponseEntity.ok(Map.of(
            "status", "available",
            "providers", Map.of(
                "ors", true,
                "google", false,
                "mapbox", false,
                "fallback", true
            )
        ));
    }
}
