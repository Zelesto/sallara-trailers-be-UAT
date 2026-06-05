package com.pgsa.trailers.service.routing.providers;

import com.pgsa.trailers.service.routing.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class MapboxRoutingProvider implements RoutingProvider {

    @Value("${mapbox.token:}")
    private String token;

    @Override
    public String name() {
        return "mapbox";
    }

    @Override
    public boolean supports(String vehicleType) {
        return token != null && !token.isBlank();
    }

    @Override
    public RoutingResult calculate(Coordinates origin,
                                   Coordinates destination,
                                   String vehicleType) {

        throw new UnsupportedOperationException(
                "Mapbox provider not implemented yet"
        );
    }
}
