package com.pgsa.trailers.dto;

import lombok.Data;

@Data
public class TripDetailsDTO {
    private Long id;
    private String tripNumber;
    private Long vehicleId;
    private String vehicleRegistration;
    private String vehicleMake;
    private String vehicleModel;
    private Long driverId;
    private String driverName;
    private String driverLicenseNumber;
    
    // Location details with city and zip code
    private String originCity;
    private String originZipCode;
    private String originProvince;
    private String originStreetAddress;
    
    private String destinationCity;
    private String destinationZipCode;
    private String destinationProvince;
    private String destinationStreetAddress;
    
    // Route metrics
    private Double plannedDistanceKm;
    private Double plannedDurationHours;
    private Double actualDistanceKm;
    private Double actualDurationHours;
}
