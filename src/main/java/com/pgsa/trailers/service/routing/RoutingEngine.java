package com.pgsa.trailers.service.routing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingEngine {

    private final List<RoutingProvider> providers;
    private final GeocodingService geocodingService;

    public RoutingResult calculateRoute(String origin,
                                        String destination,
                                        String vehicleType) {

        if (origin == null || origin.isBlank() ||
            destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("Origin and destination must not be empty");
        }

        Coordinates originCoords;
        Coordinates destCoords;

        try {
            originCoords = geocodingService.geocode(origin);
            destCoords = geocodingService.geocode(destination);

            validateCoordinates(originCoords, origin);
            validateCoordinates(destCoords, destination);

        } catch (Exception e) {
            log.error("Geocoding failed for {} -> {}", origin, destination, e);
            throw new RuntimeException(
                    "Failed to geocode locations: " + origin + " -> " + destination,
                    e
            );
        }

        return executeWithFailover(originCoords, destCoords, vehicleType, origin, destination);
    }

    public RoutingResult calculateRouteDirect(double startLat,
                                              double startLng,
                                              double endLat,
                                              double endLng,
                                              String vehicleType) {

        Coordinates origin = new Coordinates(startLat, startLng);
        Coordinates dest = new Coordinates(endLat, endLng);

        return executeWithFailover(origin, dest, vehicleType, "direct", "direct");
    }

    private RoutingResult executeWithFailover(Coordinates origin,
                                              Coordinates destination,
                                              String vehicleType,
                                              String originLabel,
                                              String destLabel) {

        Exception lastError = null;

        Map<String, Object> context = new HashMap<>();
        context.put("originLabel", originLabel);
        context.put("destLabel", destLabel);

        log.info("Routing request: {} -> {} | vehicle={}",
                originLabel, destLabel, vehicleType);

        List<RoutingProvider> sortedProviders =
                providers.stream()
                        .sorted(AnnotationAwareOrderComparator.INSTANCE)
                        .toList();

        for (RoutingProvider provider : sortedProviders) {

            try {
                if (!provider.supports(vehicleType)) {
                    log.debug("Provider {} does not support {}", provider.name(), vehicleType);
                    continue;
                }

                log.info("Trying provider: {}", provider.name());

                RoutingResult result =
                        provider.calculate(origin, destination, vehicleType, context);

                if (result == null || result.getDistanceKm() == null) {
                    throw new IllegalStateException("Invalid routing result from " + provider.name());
                }

                log.info("Success via {} | distance={} km",
                        provider.name(), result.getDistanceKm());

                return result;

            } catch (Exception e) {
                lastError = e;
                log.warn("Provider {} failed: {}", provider.name(), e.getMessage());
            }
        }

        log.error("All routing providers failed for {} -> {}", originLabel, destLabel);

        throw new RuntimeException(
                "All routing providers failed for route: " +
                originLabel + " -> " + destLabel,
                lastError
        );
    }

    private void validateCoordinates(Coordinates coords, String input) {
        if (coords == null) {
            throw new IllegalArgumentException("Geocoding returned null for: " + input);
        }

        if (coords.getLat() == 0.0 && coords.getLng() == 0.0) {
            throw new IllegalArgumentException("Invalid geocoding result for: " + input);
        }

        if (Double.isNaN(coords.getLat()) || Double.isNaN(coords.getLng())) {
            throw new IllegalArgumentException("NaN coordinates for: " + input);
        }
    }
}
