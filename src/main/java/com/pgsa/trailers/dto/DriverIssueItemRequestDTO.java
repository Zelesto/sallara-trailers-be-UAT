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
public class DriverIssueItemRequestDTO {
    private Long itemId;
    private BigDecimal quantity;
    private String condition;
    private String notes;
}
