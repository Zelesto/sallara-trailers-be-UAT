// src/main/java/com/pgsa/trailers/dto/LoadResponseDTO.java
package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadResponseDTO {
    private Long id;
    private String loadNumber;
    private String loadType;
    private String description;
    private Long customerId;
    private String customerName;
    private String status;
    private String priority;
    private BigDecimal estimatedValue;
    private BigDecimal actualValue;
    private String notes;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private Integer tripCount;
    private List<TripSummaryDTO> trips;
}
