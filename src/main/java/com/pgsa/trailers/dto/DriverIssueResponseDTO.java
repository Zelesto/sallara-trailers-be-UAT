package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverIssueResponseDTO {
    private Long id;
    private String issueNumber;
    private Long driverId;
    private String driverName;
    private LocalDateTime issueDate;
    private String status;
    private String notes;
    private List<DriverIssueItemResponseDTO> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
