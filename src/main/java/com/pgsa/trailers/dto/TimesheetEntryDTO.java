// src/main/java/com/pgsa/trailers/dto/TimesheetEntryDTO.java
package com.pgsa.trailers.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class TimesheetEntryDTO {
    private Long id;
    private Long driverId;
    private LocalDate entryDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer breakDuration;
    private BigDecimal totalHours;
    private String activityType;
    private String status;
    private String notes;
    private String punchStatus;
    private String punchLocation;
    private BigDecimal punchLatitude;
    private BigDecimal punchLongitude;
    private LocalDateTime clockInTime;
    private LocalDateTime clockOutTime;
    private LocalDateTime breakStartTime;
    private LocalDateTime breakEndTime;
}
