// src/main/java/com/pgsa/trailers/dto/PunchRequestDTO.java
package com.pgsa.trailers.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PunchRequestDTO {
    private Long driverId;
    private String punchType; // CLOCK_IN, BREAK_START, BREAK_END, CLOCK_OUT
    private String location;
    private BigDecimal latitude;
    private BigDecimal longitude;
}

