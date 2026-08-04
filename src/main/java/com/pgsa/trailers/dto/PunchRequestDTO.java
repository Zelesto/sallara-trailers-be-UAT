// src/main/java/com/pgsa/trailers/dto/PunchRequestDTO.java
package com.pgsa.trailers.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PunchRequestDTO {
    
    @JsonProperty("driver_id")
    private Long driverId;
    
    @JsonProperty("punch_type")
    private String punchType;
    
    @JsonProperty("location")
    private String location;
    
    @JsonProperty("latitude")
    private BigDecimal latitude;
    
    @JsonProperty("longitude")
    private BigDecimal longitude;
}
