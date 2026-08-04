// src/main/java/com/pgsa/trailers/dto/LeaveRequestDTO.java
package com.pgsa.trailers.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeaveRequestDTO {
    
    @JsonProperty("driver_id")
    private Long driverId;
    
    @JsonProperty("leave_type_id")
    private Long leaveTypeId;
    
    @JsonProperty("start_date")
    private LocalDate startDate;
    
    @JsonProperty("end_date")
    private LocalDate endDate;
    
    private String reason;
    private String notes;
}
