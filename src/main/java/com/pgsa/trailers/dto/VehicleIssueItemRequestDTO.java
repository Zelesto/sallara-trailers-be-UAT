// src/main/java/com/pgsa/trailers/dto/VehicleIssueItemRequestDTO.java
package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleIssueItemRequestDTO {
    private Long itemId;
    private BigDecimal quantity;
    private String condition;
    private String notes;
}

