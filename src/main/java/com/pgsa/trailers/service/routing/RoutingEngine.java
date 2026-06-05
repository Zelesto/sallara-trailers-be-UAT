package com.pgsa.trailers.service.routing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingEngine {

    private final List<RoutingProvider> providers;
    private final GeocodingService geocodingService;

    /**
     * MAIN ROUTE CALCULATION (with geocoding + failover)
     */
    public RoutingResult calculateRoute(String origin,
                                        String destination,
                                        String vehicleType) {

        Coordinates originCoords;
        Coordinates destCoords;

        try {
            originCoords = geocodingService.geocode(origin);
            destCoords = geocodingService.geocode(destination);
        } catch (Exception e) {
            log.error("Geocoding failed for origin/destination", e);
            throw new RuntimeException("Failed to geocode locations", e);
        }

        return executeWithFailover(originCoords, destCoords, vehicleType, origin, destination);
    }

    /**
     * DIRECT COORDINATE ROUTING (no geocoding)
     */
    public RoutingResult calculateRouteDirect(double startLat,
                                              double startLng,
                                              double endLat,
                                              double endLng,
                                              String vehicleType) {

        Coordinates origin = new Coordinates(startLat, startLng);
        Coordinates dest = new Coordinates(endLat, endLng);

        return executeWithFailover(origin, dest, vehicleType, "direct", "direct");
    }

    /**
     * CORE FAILOVER ENGINE
     */
    private RoutingResult executeWithFailover(Coordinates origin,
                                              Coordinates destination,
                                              String vehicleType,
                                              String originLabel,
                                              String destLabel) {

        Exception lastError = null;

        // Sort providers for deterministic behavior
        List<RoutingProvider> sortedProviders = providers.stream()
                .sorted(Comparator.comparing(RoutingProvider::name))
                .toList();

        log.info("Routing request: {} -> {} | vehicle={}",
                originLabel, destLabel, vehicleType);

        for (RoutingProvider provider : sortedProviders) {

            try {
                if (!provider.supports(vehicleType)) {
                    log.debug("Provider {} does not support {}", provider.name(), vehicleType);
                    continue;
                }

                log.info("Trying routing provider: {}", provider.name());

                RoutingResult result =
                        provider.calculate(origin, destination, vehicleType);

                log.info("Routing success via {}: {} km",
                        provider.name(), result.getDistance());

                return result;

            } catch (Exception e) {
                lastError = e;

                log.warn("Provider {} failed: {}",
                        provider.name(), e.getMessage());
            }
        }

        log.error("All routing providers failed for {} -> {}",
                originLabel, destLabel);

        throw new RuntimeException("All routing providers failed", lastError);
    }
}
