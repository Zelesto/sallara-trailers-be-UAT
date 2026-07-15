package com.pgsa.trailers.dto;

import lombok.Data;

@Data
public class RouteRequest {
    private String origin;
    private String destination;
    private String vehicleType = "TRUCK";
}
