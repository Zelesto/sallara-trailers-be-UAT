package com.pgsa.trailers.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    // ====== NEW FIELDS ======
    @JsonProperty("current_status")
    private String currentStatus;
    
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
}
