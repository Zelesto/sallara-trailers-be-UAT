// src/main/java/com/pgsa/trailers/dto/PodResponseDTO.java
package com.pgsa.trailers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PodResponseDTO {
    
    // Basic Information
    private Long id;
    private String podNumber;
    private Long tripId;
    private String tripNumber;
    private String customerName;
    private String driverName;
    private LocalDate deliveryDate;
    private String status;
    private String source;
    
    // Document Information
    private String documentType;
    private String fileSize;
    private String fileUrl;
    private String fileName;
    private String documentReference;
    private String notes;
    
    // Upload Information
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    
    // Verification Information
    private String verifiedBy;
    private LocalDateTime verifiedAt;
    
    // Rejection Information
    private String rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    
    // Debrief Information
    private LocalDateTime debriefedAt;
    private String debriefedBy;
    private String receivedBy;
    private Integer qualityRating;
    private String issuesFound;
    private String deliveryCondition;
    private String debriefNotes;
    private String additionalInfo;
    
    // Audit Information
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
