package com.pgsa.trailers.dto;

import lombok.Data;

@Data
public class DirectRouteRequest {
    private double startLat;
    private double startLng;
    private double endLat;
    private double endLng;
    private String vehicleType = "TRUCK";
}
