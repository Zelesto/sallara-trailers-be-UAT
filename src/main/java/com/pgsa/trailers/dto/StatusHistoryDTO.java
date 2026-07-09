// src/main/java/com/pgsa/trailers/dto/StatusHistoryDTO.java
package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusHistoryDTO {
    private String status;
    private String notes;
    private String updatedBy;
    private LocalDateTime timestamp;
}
