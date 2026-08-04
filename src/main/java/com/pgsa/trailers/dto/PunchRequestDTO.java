// src/main/java/com/pgsa/trailers/dto/PunchRequestDTO.java
package com.pgsa.trailers.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PunchRequestDTO {
    @JsonProperty("driver_id")  // Map from snake_case
    private Long driverId;
    
    @JsonProperty("punch_type")
    private String punchType;
    
    private String location;
    private BigDecimal latitude;
    private BigDecimal longitude;
}

