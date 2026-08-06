// src/main/java/com/pgsa/trailers/dto/MaintenanceRecordRequest.java
package com.pgsa.trailers.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MaintenanceRecordRequest {
    private Long vehicleId;
    private String type;
    private LocalDate date;
    private BigDecimal odometer;
    private BigDecimal cost;
    private String description;
    private String serviceProvider;
    private String status;
}
