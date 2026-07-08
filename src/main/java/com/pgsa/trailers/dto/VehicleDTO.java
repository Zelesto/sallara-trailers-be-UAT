package com.pgsa.trailers.dto;

import com.fasterxml.jackson.annotation.JsonIgnore; 
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pgsa.trailers.entity.assets.Vehicle;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VehicleDTO {
    // Basic fields - using camelCase to match entity with snake_case JSON mapping
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("registration_number")
    private String registrationNumber;
    
    @JsonProperty("vin")
    private String vin;
    
    @JsonProperty("make")
    private String make;
    
    @JsonProperty("model")
    private String model;
    
    @JsonProperty("year")
    private Integer year;
    
    @JsonProperty("fuel_type")
    private String fuelType;
    
    @JsonProperty("current_mileage")
    private BigDecimal currentMileage;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("created_by")
    private String createdBy;
    
    @JsonProperty("updated_by")
    private String updatedBy;
    
    @JsonProperty("avg_consumption")
    private BigDecimal avgConsumption;
    
    @JsonProperty("current_odometer")
    private BigDecimal currentOdometer;
    
    @JsonProperty("last_service_date")
    private LocalDate lastServiceDate;
    
    @JsonProperty("last_service_odometer")
    private BigDecimal lastServiceOdometer;
    
    @JsonProperty("service_interval_days")
    private Integer serviceIntervalDays;
<<<<<<< HEAD
    
    @JsonProperty("service_interval_km")
    private Integer serviceIntervalKm;
    
    @JsonProperty("insurance_policy_number")
    private String insurancePolicyNumber;
    
    @JsonProperty("insurance_expiry")
    private LocalDate insuranceExpiry;
    
    @JsonProperty("roadworthy_expiry")
    private LocalDate roadworthyExpiry;
    
    @JsonProperty("fleet_number")
    private String fleetNumber;
    
    @JsonProperty("assigned_driver_id")
    private Long assignedDriverId;
    
    @JsonProperty("gps_tracker_id")
    private Long gpsTrackerId;
    
    @JsonProperty("maintenance_status")
    private String maintenanceStatus;
    
    @JsonProperty("next_service_due")
    private LocalDate nextServiceDue;
    
    @JsonProperty("next_service_odometer")
    private BigDecimal nextServiceOdometer;
    
    @JsonProperty("incidents_logged")
    private Integer incidentsLogged;
    
    @JsonProperty("notes")
    private String notes;

    @JsonIgnore
    private Map<String, Object> auditTrail;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("vehicle_type")  // ← CRITICAL: Maps snake_case to camelCase
    private String vehicleType;
    
    @JsonProperty("is_active")
    private Boolean isActive;
    
    @JsonProperty("version")
    private Integer version;
    
    @JsonProperty("current_value")
    private BigDecimal currentValue;
    
    @JsonProperty("purchase_date")
    private LocalDate purchaseDate;
    
    @JsonProperty("purchase_price")
    private BigDecimal purchasePrice;
    
    @JsonProperty("maintenance_cost")
    private BigDecimal maintenanceCost;
    
    @JsonProperty("last_maintenance_date")
    private LocalDate lastMaintenanceDate;
    
    @JsonProperty("next_maintenance_due")
    private LocalDate nextMaintenanceDue;
    
    @JsonProperty("fuel_efficiency")
    private BigDecimal fuelEfficiency;
    
    @JsonProperty("insurance_provider")
    private String insuranceProvider;
    
    @JsonProperty("insurance_expiry_date")
    private LocalDate insuranceExpiryDate;
    
    // Optional: Add a method to convert from Entity to DTO
    public static VehicleDTO fromEntity(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        
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
        //dto.setAuditTrail(vehicle.getAuditTrail());
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
=======
    private Integer serviceIntervalKm;
    private Long assignedDriverId;
>>>>>>> 22cb06bce9627db382efdb06a7f1a83560a01d5a
}
