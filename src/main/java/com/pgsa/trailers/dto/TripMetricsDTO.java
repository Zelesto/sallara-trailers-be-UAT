package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripMetricsDTO {
    private Long id;
    private Long tripId;
    private String tripNumber;
    private String vehicleType;
    
    // Distance & time
    private BigDecimal totalDistanceKm;
    private BigDecimal totalDurationHours;
    private BigDecimal idleTimeHours;
    private BigDecimal averageSpeedKmh;
    
    // Fuel
    private BigDecimal fuelUsedLiters;
    
    // Activity
    private Integer incidentCount;
    private Integer tasksCompleted;
    
    // Financial
    private BigDecimal revenueAmount;
    private BigDecimal costAmount;
    
    // Location-based
    private BigDecimal originCityTravelTimeHours;
    private BigDecimal destinationCityTravelTimeHours;
    private BigDecimal plannedVsActualDistanceVarianceKm;
    private BigDecimal plannedVsActualDurationVarianceHours;
    private BigDecimal geocodingConfidenceScore;
    
    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean finalized;
    private LocalDateTime finalizedAt;

    // ====== EXPLICIT GETTERS AND SETTERS (since Lombok may not work) ======

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public String getTripNumber() {
        return tripNumber;
    }

    public void setTripNumber(String tripNumber) {
        this.tripNumber = tripNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public BigDecimal getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(BigDecimal totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public BigDecimal getTotalDurationHours() {
        return totalDurationHours;
    }

    public void setTotalDurationHours(BigDecimal totalDurationHours) {
        this.totalDurationHours = totalDurationHours;
    }

    public BigDecimal getIdleTimeHours() {
        return idleTimeHours;
    }

    public void setIdleTimeHours(BigDecimal idleTimeHours) {
        this.idleTimeHours = idleTimeHours;
    }

    public BigDecimal getAverageSpeedKmh() {
        return averageSpeedKmh;
    }

    public void setAverageSpeedKmh(BigDecimal averageSpeedKmh) {
        this.averageSpeedKmh = averageSpeedKmh;
    }

    public BigDecimal getFuelUsedLiters() {
        return fuelUsedLiters;
    }

    public void setFuelUsedLiters(BigDecimal fuelUsedLiters) {
        this.fuelUsedLiters = fuelUsedLiters;
    }

    public Integer getIncidentCount() {
        return incidentCount;
    }

    public void setIncidentCount(Integer incidentCount) {
        this.incidentCount = incidentCount;
    }

    public Integer getTasksCompleted() {
        return tasksCompleted;
    }

    public void setTasksCompleted(Integer tasksCompleted) {
        this.tasksCompleted = tasksCompleted;
    }

    public BigDecimal getRevenueAmount() {
        return revenueAmount;
    }

    public void setRevenueAmount(BigDecimal revenueAmount) {
        this.revenueAmount = revenueAmount;
    }

    public BigDecimal getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(BigDecimal costAmount) {
        this.costAmount = costAmount;
    }

    public BigDecimal getOriginCityTravelTimeHours() {
        return originCityTravelTimeHours;
    }

    public void setOriginCityTravelTimeHours(BigDecimal originCityTravelTimeHours) {
        this.originCityTravelTimeHours = originCityTravelTimeHours;
    }

    public BigDecimal getDestinationCityTravelTimeHours() {
        return destinationCityTravelTimeHours;
    }

    public void setDestinationCityTravelTimeHours(BigDecimal destinationCityTravelTimeHours) {
        this.destinationCityTravelTimeHours = destinationCityTravelTimeHours;
    }

    public BigDecimal getPlannedVsActualDistanceVarianceKm() {
        return plannedVsActualDistanceVarianceKm;
    }

    public void setPlannedVsActualDistanceVarianceKm(BigDecimal plannedVsActualDistanceVarianceKm) {
        this.plannedVsActualDistanceVarianceKm = plannedVsActualDistanceVarianceKm;
    }

    public BigDecimal getPlannedVsActualDurationVarianceHours() {
        return plannedVsActualDurationVarianceHours;
    }

    public void setPlannedVsActualDurationVarianceHours(BigDecimal plannedVsActualDurationVarianceHours) {
        this.plannedVsActualDurationVarianceHours = plannedVsActualDurationVarianceHours;
    }

    public BigDecimal getGeocodingConfidenceScore() {
        return geocodingConfidenceScore;
    }

    public void setGeocodingConfidenceScore(BigDecimal geocodingConfidenceScore) {
        this.geocodingConfidenceScore = geocodingConfidenceScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean isFinalized() {
        return finalized;
    }

    public void setFinalized(Boolean finalized) {
        this.finalized = finalized;
    }

    public LocalDateTime getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(LocalDateTime finalizedAt) {
        this.finalizedAt = finalizedAt;
    }
}
