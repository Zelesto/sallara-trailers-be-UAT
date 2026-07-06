package com.pgsa.trailers.entity.assets;

import com.pgsa.trailers.config.BaseEntity;
import com.pgsa.trailers.enums.VehicleStatus;
import com.pgsa.trailers.enums.VehicleType;
import com.pgsa.trailers.helpers.JsonConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(
        name = "vehicle",
        indexes = {
                @Index(name = "idx_vehicle_registration", columnList = "registration_number"),
                @Index(name = "idx_vehicle_vin", columnList = "vin"),
                @Index(name = "idx_vehicle_fleet_number", columnList = "fleet_number"),
                @Index(name = "idx_vehicle_status", columnList = "status"),
                @Index(name = "idx_vehicle_vehicle_type", columnList = "vehicle_type"),
                @Index(name = "idx_vehicle_assigned_driver", columnList = "assigned_driver_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_vehicle_registration", columnNames = {"registration_number"}),
                @UniqueConstraint(name = "uq_vehicle_vin", columnNames = {"vin"}),
                @UniqueConstraint(name = "uq_vehicle_fleet_number", columnNames = {"fleet_number"})
        }
)
public class Vehicle extends BaseEntity {

    @Column(name = "registration_number", unique = true, nullable = false, length = 20)
    private String registrationNumber;

    @Column(name = "vin", length = 50, unique = true)
    private String vin;

    @Column(name = "make", length = 100)
    private String make;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "year")
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", length = 20)
    private VehicleType vehicleType;

    @Column(name = "fuel_type", length = 20)
    private String fuelType;

    @Column(name = "current_mileage", precision = 12, scale = 2)
    private BigDecimal currentMileage;

    @Column(name = "avg_consumption", precision = 12, scale = 2)
    private BigDecimal avgConsumption;

    @Column(name = "current_odometer", precision = 12, scale = 2)
    private BigDecimal currentOdometer;

    @Column(name = "last_service_date")
    private LocalDate lastServiceDate;

    @Column(name = "last_service_odometer", precision = 12, scale = 2)
    private BigDecimal lastServiceOdometer;

    @Column(name = "service_interval_days")
    private Integer serviceIntervalDays;

    @Column(name = "service_interval_km")
    private Integer serviceIntervalKm;

    @Column(name = "insurance_policy_number", length = 100)
    private String insurancePolicyNumber;

    @Column(name = "insurance_expiry")
    private LocalDate insuranceExpiry;

    @Column(name = "roadworthy_expiry")
    private LocalDate roadworthyExpiry;

    @Column(name = "fleet_number", length = 50, unique = true)
    private String fleetNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_driver_id")
    private Driver assignedDriver;

    @Column(name = "gps_tracker_id")
    private Long gpsTrackerId;

    @Column(name = "maintenance_status", length = 50)
    private String maintenanceStatus;

    @Column(name = "next_service_due")
    private LocalDate nextServiceDue;

    @Column(name = "next_service_odometer", precision = 12, scale = 2)
    private BigDecimal nextServiceOdometer;

    @Column(name = "incidents_logged")
    private Integer incidentsLogged = 0;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Convert(converter = JsonConverter.class)
    @Column(name = "audit_trail", columnDefinition = "jsonb")
    private Map<String, Object> auditTrail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_price", precision = 15, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "current_value", precision = 15, scale = 2)
    private BigDecimal currentValue;

    // ====== NEW FIELDS - ADD THESE ======
    
    @Column(name = "maintenance_cost", precision = 15, scale = 2)
    private BigDecimal maintenanceCost;

    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    @Column(name = "next_maintenance_due")
    private LocalDate nextMaintenanceDue;

    @Column(name = "fuel_efficiency", precision = 12, scale = 2)
    private BigDecimal fuelEfficiency;

    @Column(name = "insurance_provider", length = 100)
    private String insuranceProvider;

    @Column(name = "insurance_expiry_date")
    private LocalDate insuranceExpiryDate;

    // ====== Constructors ======
    public Vehicle() {
        this.status = VehicleStatus.AVAILABLE;
        this.incidentsLogged = 0;
    }

    // ====== Business Methods ======
    
    public void calculateNextService() {
        if (lastServiceDate != null && serviceIntervalDays != null) {
            this.nextServiceDue = lastServiceDate.plusDays(serviceIntervalDays);
        }
        if (lastServiceOdometer != null && serviceIntervalKm != null) {
            this.nextServiceOdometer = lastServiceOdometer.add(BigDecimal.valueOf(serviceIntervalKm));
        }
    }

    public boolean isActive() {
        return status == VehicleStatus.ACTIVE || status == VehicleStatus.AVAILABLE;
    }

    public boolean isAvailable() {
        return (status == VehicleStatus.AVAILABLE || status == VehicleStatus.ACTIVE) &&
                assignedDriver == null &&
                isInsuranceValid() &&
                isRoadworthyValid() &&
                !isInMaintenance();
    }

    public boolean isInsuranceValid() {
        return insuranceExpiry == null || !insuranceExpiry.isBefore(LocalDate.now());
    }

    public boolean isRoadworthyValid() {
        return roadworthyExpiry == null || !roadworthyExpiry.isBefore(LocalDate.now());
    }

    public boolean isInMaintenance() {
        return "MAINTENANCE".equalsIgnoreCase(maintenanceStatus) || 
               status == VehicleStatus.MAINTENANCE;
    }

    public boolean isOverdueForService() {
        if (nextServiceDue != null && nextServiceDue.isBefore(LocalDate.now())) {
            return true;
        }
        if (nextServiceOdometer != null && currentOdometer != null && 
            currentOdometer.compareTo(nextServiceOdometer) >= 0) {
            return true;
        }
        return false;
    }

    public void incrementIncidents() {
        this.incidentsLogged = (incidentsLogged == null ? 0 : incidentsLogged) + 1;
    }

    public void assignDriver(Driver driver) {
        this.assignedDriver = driver;
        this.status = VehicleStatus.ASSIGNED;
    }

    public void unassignDriver() {
        this.assignedDriver = null;
        if (status == VehicleStatus.ASSIGNED) {
            this.status = VehicleStatus.AVAILABLE;
        }
    }

    public void updateOdometer(BigDecimal newOdometer) {
        if (newOdometer != null) {
            this.currentOdometer = newOdometer;
            this.currentMileage = newOdometer;
        }
    }

    public void markForMaintenance(String reason) {
        this.maintenanceStatus = reason;
        this.status = VehicleStatus.MAINTENANCE;
    }

    public void completeMaintenance() {
        this.maintenanceStatus = null;
        this.status = VehicleStatus.AVAILABLE;
    }

    public BigDecimal getDistanceSinceLastService() {
        if (currentOdometer != null && lastServiceOdometer != null) {
            return currentOdometer.subtract(lastServiceOdometer);
        }
        return BigDecimal.ZERO;
    }

    public String getDisplayName() {
        return String.format("%s %s (%s)", 
            make != null ? make : "", 
            model != null ? model : "", 
            registrationNumber);
    }

    // ====== Lifecycle Hooks ======
    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = VehicleStatus.AVAILABLE;
        }
        if (incidentsLogged == null) {
            incidentsLogged = 0;
        }
        calculateNextService();
    }

    @PreUpdate
    protected void onUpdate() {
        calculateNextService();
    }
}
