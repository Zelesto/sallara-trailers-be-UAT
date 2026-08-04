package com.pgsa.trailers.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private Map<String, Object> trainingCertificates;
    
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
    
    @JsonIgnore
    private Map<String, Object> auditTrail;
    
    @JsonProperty("is_active")
    private Boolean isActive;
    
    @JsonProperty("version")
    private Integer version;
    
    @JsonProperty("app_user_id")
    private Long appUserId;
    
    // ====== NEW FIELDS ======
    @JsonProperty("current_status")
    private String currentStatus;
    
    @JsonProperty("last_clock_in")
    private LocalDateTime lastClockIn;
    
    @JsonProperty("last_clock_out")
    private LocalDateTime lastClockOut;
    
    @JsonProperty("last_trip_date")
    private LocalDate lastTripDate;
    
    @JsonProperty("date_of_birth")
    private LocalDate dateOfBirth;
    
    @JsonProperty("gender")
    private String gender;
    
    @JsonProperty("country")
    private String country;
    
    @JsonProperty("address")
    private String address;
    
    @JsonProperty("emergency_contact_name")
    private String emergencyContactName;
    
    @JsonProperty("emergency_contact_phone")
    private String emergencyContactPhone;
    
    @JsonProperty("bank_name")
    private String bankName;
    
    @JsonProperty("bank_account_number")
    private String bankAccountNumber;
    
    @JsonProperty("bank_branch_code")
    private String bankBranchCode;
    
    @JsonProperty("tax_number")
    private String taxNumber;
    
    @JsonProperty("last_medical_exam_date")
    private LocalDate lastMedicalExamDate;
    
    @JsonProperty("next_medical_exam_date")
    private LocalDate nextMedicalExamDate;
    
    @JsonProperty("driver_license_class")
    private String driverLicenseClass;
    
    @JsonProperty("license_issue_date")
    private LocalDate licenseIssueDate;
    
    @JsonProperty("license_restrictions")
    private String licenseRestrictions;
    
    @JsonProperty("endorsements")
    private String endorsements;
    
    @JsonProperty("driver_photo_url")
    private String driverPhotoUrl;
    
    @JsonProperty("employee_id")
    private String employeeId;
    
    @JsonProperty("department")
    private String department;
    
    @JsonProperty("supervisor_id")
    private Long supervisorId;
    
    // For backward compatibility
    private AppUserDTO appUser;
    private String password;
}
