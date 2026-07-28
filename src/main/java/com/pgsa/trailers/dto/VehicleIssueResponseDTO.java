// src/main/java/com/pgsa/trailers/dto/VehicleIssueResponseDTO.java
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
public class VehicleIssueResponseDTO {
    private Long id;
    private String issueNumber;
    private Long vehicleId;
    private String vehicleRegistration;
    private Long driverId;
    private String driverName;
    private Long tripId;
    private String tripNumber;
    private LocalDateTime issueDate;
    private String status;
    private String notes;
    private List<VehicleIssueItemResponseDTO> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

