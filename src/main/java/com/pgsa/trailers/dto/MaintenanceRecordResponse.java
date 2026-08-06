// src/main/java/com/pgsa/trailers/dto/MaintenanceRecordResponse.java
package com.pgsa.trailers.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MaintenanceRecordResponse {
    private Long id;
    private Long vehicleId;
    private String type;
    private LocalDate date;
    private BigDecimal odometer;
    private BigDecimal cost;
    private String description;
    private String serviceProvider;
    private String status;
    private LocalDateTime createdAt;
}
