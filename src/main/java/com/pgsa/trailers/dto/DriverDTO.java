package com.pgsa.trailers.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DriverDTO {
    private Long id;
    
    @JsonProperty("first_name")
    private String firstName;
    
    @JsonProperty("last_name")
    private String lastName;
    
    @JsonProperty("license_number")
    private String licenseNumber;
    
    @JsonProperty("license_type")
    private String licenseType;
    
    @JsonProperty("license_expiry")
    private LocalDate licenseExpiry;
    
    @JsonProperty("hire_date")
    private LocalDate hireDate;
    
    @JsonProperty("phone_number")
    private String phoneNumber;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("termination_date")
    private LocalDate terminationDate;
    
    @JsonProperty("termination_reason")
    private String terminationReason;
    
    @JsonProperty("employment_type")
    private String employmentType;
    
    @JsonProperty("shift_pattern")
    private String shiftPattern;
    
    @JsonProperty("assigned_vehicle_id")
    private Long assignedVehicleId;
    
    @JsonProperty("training_completed")
    private Boolean trainingCompleted;
    
    @JsonProperty("training_certificates")
    private String trainingCertificates;
    
    @JsonProperty("medical_clearance_date")
    private LocalDate medicalClearanceDate;
    
    @JsonProperty("next_medical_due")
    private LocalDate nextMedicalDue;
    
    @JsonProperty("incidents_logged")
    private Integer incidentsLogged;
    
    @JsonProperty("total_trips")
    private Integer totalTrips;
    
    @JsonProperty("total_km_travelled")
    private BigDecimal totalKmTravelled;
    
    @JsonProperty("total_hours_active")
    private BigDecimal totalHoursActive;
    
    @JsonProperty("performance_score")
    private BigDecimal performanceScore;
    
    @JsonProperty("notes")
    private String notes;
    
    @JsonIgnore  // ⭐ CRITICAL: Ignore audit_trail from frontend
    private Map<String, Object> auditTrail;
    
    @JsonProperty("is_active")
    private Boolean isActive;
    
    @JsonProperty("version")
    private Integer version;
    
    @JsonProperty("app_user_id")
    private Long appUserId;
    
    // For backward compatibility with existing code
    private AppUserDTO appUser;
    private String password;
}
