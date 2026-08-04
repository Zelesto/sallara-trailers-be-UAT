// src/main/java/com/pgsa/trailers/dto/LeaveBalanceDTO.java
package com.pgsa.trailers.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LeaveBalanceDTO {
    private Long id;
    private Long driverId;
    private Long leaveTypeId;
    private String leaveTypeName;
    private String leaveTypeCode;
    private Integer year;
    private BigDecimal totalDays;
    private BigDecimal usedDays;
    private BigDecimal pendingDays;
    private BigDecimal remainingDays;
    private BigDecimal carriedOver;
}
