package com.pgsa.trailers.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
public class VehicleDTO {
    // Basic fields - using camelCase to match entity
    private Long id;
    private String registrationNumber;
    private String vin;
    private String make;
    private String model;
    private Integer year;
    private String fuelType;
    private BigDecimal currentMileage;
    private String status;
    private String createdBy;
    private String updatedBy;
    private BigDecimal avgConsumption;
    private BigDecimal currentOdometer;
    private LocalDate lastServiceDate;
    private BigDecimal lastServiceOdometer;
    private Integer serviceIntervalDays;
    private Integer serviceIntervalKm;
    private String insurancePolicyNumber;
    private LocalDate insuranceExpiry;
    private LocalDate roadworthyExpiry;
    private String fleetNumber;
    private Long assignedDriverId;
    private Long gpsTrackerId;
    private String maintenanceStatus;
    private LocalDate nextServiceDue;
    private BigDecimal nextServiceOdometer;
    private Integer incidentsLogged;
    private String notes;
    private Map<String, Object> auditTrail;
    private String category;
    private String vehicleType; // Will be converted to enum
    private Boolean isActive;
    private Integer version;
    private BigDecimal currentValue;
    private LocalDate purchaseDate;
    private BigDecimal purchasePrice;
    private BigDecimal maintenanceCost;
    private LocalDate lastMaintenanceDate;
    private LocalDate nextMaintenanceDue;
    private BigDecimal fuelEfficiency;
    private String insuranceProvider;
    private LocalDate insuranceExpiryDate;
}
