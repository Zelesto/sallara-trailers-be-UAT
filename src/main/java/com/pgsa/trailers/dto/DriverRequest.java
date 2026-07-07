package com.pgsa.trailers.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DriverRequest {
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
    
    @JsonProperty("employment_type")
    private String employmentType;
    
    @JsonProperty("shift_pattern")
    private String shiftPattern;
    
    @JsonProperty("training_completed")
    private Boolean trainingCompleted;
    
    @JsonProperty("medical_clearance_date")
    private LocalDate medicalClearanceDate;
    
    @JsonProperty("next_medical_due")
    private LocalDate nextMedicalDue;
    
    @JsonProperty("notes")
    private String notes;
    
    @JsonProperty("app_user_id")
    private Long appUserId;
    
    @JsonProperty("password")
    private String password;
}
