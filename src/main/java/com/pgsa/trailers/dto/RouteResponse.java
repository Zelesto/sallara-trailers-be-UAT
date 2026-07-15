package com.pgsa.trailers.dto;

import lombok.Data;

@Data
public class RouteResponse {
    private double distanceKm;
    private double durationMinutes;
    private String origin;
    private String destination;
    private String summary;
    private String provider;
    private boolean success;
    private String errorMessage;
}
