package com.pgsa.trailers.service.routing.providers;

import com.pgsa.trailers.service.routing.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(5)
public class MapboxRoutingProvider implements RoutingProvider {

    @Value("${mapbox.api.key:}")
    private String apiKey;

    @Override
    public String name() {
        return "mapbox";
    }

    @Override
    public boolean supports(String vehicleType) {
        // Only support if API key is configured AND we've implemented it
        boolean hasKey = apiKey != null && !apiKey.isBlank();
        if (hasKey) {
            log.warn("Mapbox provider not implemented yet, but API key is present");
        }
        return false; // Not implemented yet - prevent it from being used
    }

    @Override
    public RoutingResult calculate(Coordinates origin,
                                   Coordinates destination,
                                   String vehicleType,
                                   Map<String, Object> context) {
        // This should never be called since supports() returns false
        throw new UnsupportedOperationException("Mapbox routing not implemented yet");
    }
}
