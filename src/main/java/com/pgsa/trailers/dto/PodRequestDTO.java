// src/main/java/com/pgsa/trailers/dto/PodRequestDTO.java
package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodRequestDTO {
    private Long tripId;
    private String customerName;
    private LocalDate deliveryDate;
    private String status;
    private String documentType;
    private String fileSize;
    private String fileUrl;
    private String fileName;
    private String notes;
    private String uploadedBy;
}
