// src/main/java/com/pgsa/trailers/dto/LoadRequestDTO.java
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
public class LoadRequestDTO {
    private String loadNumber;
    private String description;
    private Long customerId;
    private BigDecimal weightKg;
    private BigDecimal volumeCubicM;
    private LocalDateTime loadingDate;
    private LocalDateTime unloadingDate;
    private String status;
    private String commodityType;
    private Integer palletCount;
    private String containerNumber;
    private Boolean hazardousMaterial;
    private String specialHandling;
    private BigDecimal estimatedValue;
    private BigDecimal actualValue;
    private String priority;
    private List<Long> tripIds;  // Trips to add to this load
}
