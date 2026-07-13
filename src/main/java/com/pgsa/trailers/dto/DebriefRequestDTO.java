// src/main/java/com/pgsa/trailers/dto/DebriefRequestDTO.java
package com.pgsa.trailers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebriefRequestDTO {
    
    @NotBlank(message = "Status is required")
    private String status;
    
    private String notes;
    
    @NotBlank(message = "Received by is required")
    private String receivedBy;
    
    private String signature;
    
    @NotNull(message = "Quality rating is required")
    private Integer qualityRating;
    
    private String issuesFound; 
    
    private String additionalInfo;
    
    private String deliveryCondition;
    
    @NotBlank(message = "Debrief notes are required when rejecting")
    private String debriefNotes;
    
    private String debriefedBy;
}
