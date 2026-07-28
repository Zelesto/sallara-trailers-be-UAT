package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.dto.TripMetricsDTO;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class TripMetricsMapper {

    public TripMetricsDTO toDto(TripMetrics metrics) {
        if (metrics == null) return null;

        TripMetricsDTO dto = new TripMetricsDTO();
        dto.setTripId(metrics.getTrip().getId());
        dto.setTotalDistanceKm(metrics.getTotalDistanceKm());
        dto.setTotalDurationHours(metrics.getTotalDurationHours());
        dto.setFuelUsedLiters(metrics.getFuelUsedLiters());
        dto.setAverageSpeedKmh(metrics.getAverageSpeedKmh());
        dto.setIdleTimeHours(metrics.getIdleTimeHours());
        dto.setIncidentCount(metrics.getIncidentCount());
        dto.setTasksCompleted(metrics.getTasksCompleted());
        dto.setRevenueAmount(metrics.getRevenueAmount());
        dto.setCostAmount(metrics.getCostAmount());
        
        // New location-based metrics
        dto.setOriginCityTravelTimeHours(metrics.getOriginCityTravelTimeHours());
        dto.setDestinationCityTravelTimeHours(metrics.getDestinationCityTravelTimeHours());
        dto.setPlannedVsActualDistanceVarianceKm(metrics.getPlannedVsActualDistanceVarianceKm());
        dto.setPlannedVsActualDurationVarianceHours(metrics.getPlannedVsActualDurationVarianceHours());
        dto.setGeocodingConfidenceScore(metrics.getGeocodingConfidenceScore());

        return dto;
    }

    public TripMetrics toEntity(TripMetricsDTO dto, Trip trip) {
        if (dto == null) return null;

        TripMetrics metrics = new TripMetrics();
        metrics.setTrip(trip);
        metrics.setTotalDistanceKm(dto.getTotalDistanceKm());
        metrics.setTotalDurationHours(dto.getTotalDurationHours());
        metrics.setFuelUsedLiters(dto.getFuelUsedLiters());
        metrics.setAverageSpeedKmh(dto.getAverageSpeedKmh());
        metrics.setIdleTimeHours(dto.getIdleTimeHours());
        metrics.setIncidentCount(dto.getIncidentCount());
        metrics.setTasksCompleted(dto.getTasksCompleted());
        metrics.setRevenueAmount(dto.getRevenueAmount());
        metrics.setCostAmount(dto.getCostAmount());
        
        // New location-based metrics
        metrics.setOriginCityTravelTimeHours(dto.getOriginCityTravelTimeHours());
        metrics.setDestinationCityTravelTimeHours(dto.getDestinationCityTravelTimeHours());
        metrics.setPlannedVsActualDistanceVarianceKm(dto.getPlannedVsActualDistanceVarianceKm());
        metrics.setPlannedVsActualDurationVarianceHours(dto.getPlannedVsActualDurationVarianceHours());
        metrics.setGeocodingConfidenceScore(dto.getGeocodingConfidenceScore());

        return metrics;
    }

    public void updateEntity(TripMetrics metrics, TripMetricsDTO dto) {
        if (metrics == null || dto == null) return;

        metrics.setTotalDistanceKm(dto.getTotalDistanceKm());
        metrics.setTotalDurationHours(dto.getTotalDurationHours());
        metrics.setFuelUsedLiters(dto.getFuelUsedLiters());
        metrics.setAverageSpeedKmh(dto.getAverageSpeedKmh());
        metrics.setIdleTimeHours(dto.getIdleTimeHours());
        metrics.setIncidentCount(dto.getIncidentCount());
        metrics.setTasksCompleted(dto.getTasksCompleted());
        metrics.setRevenueAmount(dto.getRevenueAmount());
        metrics.setCostAmount(dto.getCostAmount());
        
        // New location-based metrics
        metrics.setOriginCityTravelTimeHours(dto.getOriginCityTravelTimeHours());
        metrics.setDestinationCityTravelTimeHours(dto.getDestinationCityTravelTimeHours());
        metrics.setPlannedVsActualDistanceVarianceKm(dto.getPlannedVsActualDistanceVarianceKm());
        metrics.setPlannedVsActualDurationVarianceHours(dto.getPlannedVsActualDurationVarianceHours());
        metrics.setGeocodingConfidenceScore(dto.getGeocodingConfidenceScore());
    }
}
