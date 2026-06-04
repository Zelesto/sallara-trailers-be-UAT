package com.pgsa.trailers.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TripMetricsUpdateRequest {

    // Trip metrics to update
    private BigDecimal totalDistanceKm;
    private BigDecimal totalDurationHours;   // matches entity
    private BigDecimal fuelUsedLiters;        // matches entity
    private BigDecimal costAmount;      // matches entity
    private BigDecimal averageSpeed;    // matches entity
    private Integer incidentCount;      // matches entity
    private BigDecimal idleTimeHours;   // matches entity
    private Integer tasksCompleted;     // matches entity
    private BigDecimal revenueAmount;   // matches entity

    // New location-based metrics
    private BigDecimal originCityTravelTimeHours;
    private BigDecimal destinationCityTravelTimeHours;
    private BigDecimal plannedVsActualDistanceVarianceKm;
    private BigDecimal plannedVsActualDurationVarianceHours;
    private BigDecimal geocodingConfidenceScore;

    // Optional: cargo weight or delays if needed
    private BigDecimal cargoWeight;     // if tracked elsewhere
    private BigDecimal delays;          // if tracked elsewhere
}
