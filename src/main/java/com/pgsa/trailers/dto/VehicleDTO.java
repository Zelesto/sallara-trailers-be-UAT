package com.pgsa.trailers.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    
    // Optional: Add a method to convert from Entity to DTO
    public static VehicleDTO fromEntity(Vehicle vehicle) {
        VehicleDTO dto = new VehicleDTO();
        dto.setId(vehicle.getId());
        dto.setRegistrationNumber(vehicle.getRegistrationNumber());
        dto.setVin(vehicle.getVin());
        dto.setMake(vehicle.getMake());
        dto.setModel(vehicle.getModel());
        dto.setYear(vehicle.getYear());
        dto.setFuelType(vehicle.getFuelType());
        dto.setCurrentMileage(vehicle.getCurrentMileage());
        dto.setStatus(vehicle.getStatus() != null ? vehicle.getStatus().name() : null);
        dto.setCreatedBy(vehicle.getCreatedBy());
        dto.setUpdatedBy(vehicle.getUpdatedBy());
        dto.setAvgConsumption(vehicle.getAvgConsumption());
        dto.setCurrentOdometer(vehicle.getCurrentOdometer());
        dto.setLastServiceDate(vehicle.getLastServiceDate());
        dto.setLastServiceOdometer(vehicle.getLastServiceOdometer());
        dto.setServiceIntervalDays(vehicle.getServiceIntervalDays());
        dto.setServiceIntervalKm(vehicle.getServiceIntervalKm());
        dto.setInsurancePolicyNumber(vehicle.getInsurancePolicyNumber());
        dto.setInsuranceExpiry(vehicle.getInsuranceExpiry());
        dto.setRoadworthyExpiry(vehicle.getRoadworthyExpiry());
        dto.setFleetNumber(vehicle.getFleetNumber());
        dto.setAssignedDriverId(vehicle.getAssignedDriver() != null ? vehicle.getAssignedDriver().getId() : null);
        dto.setGpsTrackerId(vehicle.getGpsTrackerId());
        dto.setMaintenanceStatus(vehicle.getMaintenanceStatus());
        dto.setNextServiceDue(vehicle.getNextServiceDue());
        dto.setNextServiceOdometer(vehicle.getNextServiceOdometer());
        dto.setIncidentsLogged(vehicle.getIncidentsLogged());
        dto.setNotes(vehicle.getNotes());
        dto.setAuditTrail(vehicle.getAuditTrail());
        dto.setCategory(vehicle.getCategory());
        dto.setVehicleType(vehicle.getVehicleType() != null ? vehicle.getVehicleType().name() : null);
        dto.setIsActive(vehicle.getIsActive());
        dto.setVersion(vehicle.getVersion());
        dto.setCurrentValue(vehicle.getCurrentValue());
        dto.setPurchaseDate(vehicle.getPurchaseDate());
        dto.setPurchasePrice(vehicle.getPurchasePrice());
        dto.setMaintenanceCost(vehicle.getMaintenanceCost());
        dto.setLastMaintenanceDate(vehicle.getLastMaintenanceDate());
        dto.setNextMaintenanceDue(vehicle.getNextMaintenanceDue());
        dto.setFuelEfficiency(vehicle.getFuelEfficiency());
        dto.setInsuranceProvider(vehicle.getInsuranceProvider());
        dto.setInsuranceExpiryDate(vehicle.getInsuranceExpiryDate());
        return dto;
    }
}
