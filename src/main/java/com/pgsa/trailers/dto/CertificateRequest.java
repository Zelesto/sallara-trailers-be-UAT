// Create CertificateRequest.java
package com.pgsa.trailers.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@Data
public class CertificateRequest {
    @NotBlank(message = "Certificate type is required")
    private String type;
    
    private String number;
    
    private LocalDate issueDate;
    
    private LocalDate expiryDate;
    
    private String issuer;
    
    private String documentUrl;
}

