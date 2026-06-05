package com.pgsa.trailers.service.routing;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class RoutingResult {

    private Coordinates origin;
    private Coordinates destination;

    private BigDecimal distanceKm;
    private BigDecimal durationHours;

    private String provider;

    private String geometry; // encoded or geojson
}
