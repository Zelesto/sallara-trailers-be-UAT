// Create VehicleCertificateDTO.java
package com.pgsa.trailers.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class VehicleCertificateDTO {
    private Long id;
    private Long vehicleId;
    private String type;
    private String number;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String issuer;
    private String documentUrl;
    private String status;
    private LocalDate createdAt;
}
