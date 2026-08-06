// src/main/java/com/pgsa/trailers/dto/CertificateRequest.java
package com.pgsa.trailers.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CertificateRequest {
    private String type;
    private String number;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String issuer;
    private String documentUrl;
    private String description;
}
