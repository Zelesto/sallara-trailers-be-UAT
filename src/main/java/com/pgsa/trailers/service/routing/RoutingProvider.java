package com.pgsa.trailers.service.routing;

import java.util.Map;

public interface RoutingProvider {

    String name();

    boolean supports(String vehicleType);

    RoutingResult calculate(Coordinates origin,
                            Coordinates destination,
                            String vehicleType,
                            Map<String, Object> context);
}
