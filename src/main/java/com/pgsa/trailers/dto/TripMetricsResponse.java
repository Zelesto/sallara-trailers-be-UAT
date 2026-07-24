package com.pgsa.trailers.dto;

import com.pgsa.trailers.entity.ops.TripMetrics;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripMetricsResponse {

    private Long id;
    private Long tripId;
    
    private BigDecimal totalDistanceKm;
    private BigDecimal totalDurationHours;
    private BigDecimal idleTimeHours;
    private BigDecimal averageSpeedKmh;

    private BigDecimal fuelUsedLiters;
    private BigDecimal fuelEfficiencyKmPerLiter;
    private BigDecimal fuelConsumptionLPer100km;

    private Integer incidentCount;
    private Integer tasksCompleted;

    private BigDecimal revenueAmount;
    private BigDecimal costAmount;
    private BigDecimal costPerKm;
    private BigDecimal revenuePerKm;
    private BigDecimal profitMargin;
    private Boolean isProfitable;

    // Variance metrics
    private BigDecimal plannedVsActualDistanceVarianceKm;
    private BigDecimal plannedVsActualDurationVarianceHours;
    private BigDecimal onTimePerformance;

    // Location-based metrics
    private BigDecimal originCityTravelTimeHours;
    private BigDecimal destinationCityTravelTimeHours;
    private BigDecimal geocodingConfidenceScore;

    private Boolean finalized;
    private String finalizedAt;

    /**
     * Convert TripMetrics entity to DTO
     */
    public static TripMetricsResponse fromEntity(TripMetrics metrics) {
        if (metrics == null) {
            return null;
        }

        return TripMetricsResponse.builder()
                .id(metrics.getId())
                .tripId(metrics.getTrip() != null ? metrics.getTrip().getId() : null)
                .totalDistanceKm(metrics.getTotalDistanceKm())
                .totalDurationHours(metrics.getTotalDurationHours())
                .idleTimeHours(metrics.getIdleTimeHours())
                .averageSpeedKmh(metrics.getAverageSpeedKmh())
                .fuelUsedLiters(metrics.getFuelUsedLiters())
                .fuelEfficiencyKmPerLiter(metrics.calculateFuelEfficiency())
                .fuelConsumptionLPer100km(metrics.calculateFuelConsumptionLPer100km())
                .incidentCount(metrics.getIncidentCount())
                .tasksCompleted(metrics.getTasksCompleted())
                .revenueAmount(metrics.getRevenueAmount())
                .costAmount(metrics.getCostAmount())
                .costPerKm(metrics.calculateCostPerKm())
                .revenuePerKm(metrics.calculateRevenuePerKm())
                .profitMargin(metrics.calculateProfitMargin())
                .isProfitable(metrics.isProfitable())
                .plannedVsActualDistanceVarianceKm(metrics.getPlannedVsActualDistanceVarianceKm())
                .plannedVsActualDurationVarianceHours(metrics.getPlannedVsActualDurationVarianceHours())
                .originCityTravelTimeHours(metrics.getOriginCityTravelTimeHours())
                .destinationCityTravelTimeHours(metrics.getDestinationCityTravelTimeHours())
                .geocodingConfidenceScore(metrics.getGeocodingConfidenceScore())
                .finalized(metrics.isFinalized())
                .finalizedAt(metrics.getFinalizedAt() != null ? metrics.getFinalizedAt().toString() : null)
                .build();
    }

    /**
     * Calculate on-time performance based on planned hours
     */
    public BigDecimal calculateOnTimePerformance(BigDecimal plannedHours) {
        if (plannedHours == null || plannedHours.compareTo(BigDecimal.ZERO) <= 0 ||
            totalDurationHours == null) {
            return BigDecimal.valueOf(100);
        }
        
        BigDecimal variance = totalDurationHours.subtract(plannedHours);
        if (variance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.valueOf(100);
        }
        
        BigDecimal performance = BigDecimal.valueOf(100).subtract(
            variance.divide(plannedHours, 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
        );
        
        return performance.max(BigDecimal.ZERO);
    }
}
