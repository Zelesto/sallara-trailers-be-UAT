// src/main/java/com/pgsa/trailers/dto/PodRequestDTO.java
package com.pgsa.trailers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodRequestDTO {
    
    @NotNull(message = "Trip ID is required")
    private Long tripId;
    
    @NotBlank(message = "Customer name is required")
    private String customerName;
    
    private String driverName;
    
    @NotNull(message = "Delivery date is required")
    private LocalDate deliveryDate;
    
    private String status;
    private String documentType;
    private String fileSize;
    private String fileUrl;
    private String fileName;
    private String notes;
    private String uploadedBy;
}
