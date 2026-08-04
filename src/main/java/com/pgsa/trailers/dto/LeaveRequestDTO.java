// src/main/java/com/pgsa/trailers/dto/
package com.pgsa.trailers.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LeaveRequestDTO {
    private Long driverId;
    private Long leaveTypeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String notes;
}
