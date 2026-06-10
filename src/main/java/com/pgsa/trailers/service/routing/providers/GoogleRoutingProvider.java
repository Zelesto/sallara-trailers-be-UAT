package com.pgsa.trailers.service.routing.providers;

import com.pgsa.trailers.service.routing.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Order(10)
public class GoogleRoutingProvider implements RoutingProvider {

    @Value("${google.maps.api.key:}")
    private String apiKey;

    @Override
    public String name() {
        return "google";
    }

    @Override
    public boolean supports(String vehicleType) {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public RoutingResult calculate(Coordinates origin,
                                   Coordinates destination,
                                   String vehicleType,
                                   Map<String, Object> context) {

        throw new UnsupportedOperationException(
                "Google provider not implemented yet"
        );
    }
}
