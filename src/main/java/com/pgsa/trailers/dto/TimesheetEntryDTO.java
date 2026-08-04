// src/main/java/com/pgsa/trailers/dto/TimesheetEntryDTO.java
package com.pgsa.trailers.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimesheetEntryDTO {
    
    private Long id;
    
    @JsonProperty("driver_id")
    private Long driverId;
    
    @JsonProperty("entry_date")
    private LocalDate entryDate;
    
    @JsonProperty("start_time")
    private LocalTime startTime;
    
    @JsonProperty("end_time")
    private LocalTime endTime;
    
    @JsonProperty("break_duration")
    private Integer breakDuration;
    
    @JsonProperty("total_hours")
    private BigDecimal totalHours;
    
    @JsonProperty("activity_type")
    private String activityType;
    
    private String status;
    private String notes;
    
    @JsonProperty("punch_status")
    private String punchStatus;
    
    @JsonProperty("punch_location")
    private String punchLocation;
    
    @JsonProperty("punch_latitude")
    private BigDecimal punchLatitude;
    
    @JsonProperty("punch_longitude")
    private BigDecimal punchLongitude;
    
    @JsonProperty("clock_in_time")
    private LocalDateTime clockInTime;
    
    @JsonProperty("clock_out_time")
    private LocalDateTime clockOutTime;
    
    @JsonProperty("break_start_time")
    private LocalDateTime breakStartTime;
    
    @JsonProperty("break_end_time")
    private LocalDateTime breakEndTime;
}
