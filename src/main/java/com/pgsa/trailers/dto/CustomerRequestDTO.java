// src/main/java/com/pgsa/trailers/dto/CustomerRequestDTO.java
package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequestDTO {
    private String customerCode;
    private String name;
    private String registrationNumber;
    private String vatNumber;
    private String email;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String province;
    private String postalCode;
    private String country;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String paymentTerms;
    private BigDecimal creditLimit;
    private Boolean isActive;
    private String notes;
}
