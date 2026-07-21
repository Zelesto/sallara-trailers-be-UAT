// src/main/java/com/pgsa/trailers/dto/LoadResponseDTO.java
package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadResponseDTO {
    private Long id;
    private String loadNumber;
    private String referenceNumber;  // ← ADDED
    private String description;
    private Long customerId;
    private String customerName;
    private BigDecimal weightKg;
    private BigDecimal volumeCubicM;
    private LocalDateTime loadingDate;
    private LocalDateTime unloadingDate;
    private String status;
    private String commodityType;
    private Integer palletCount;
    private String containerNumber;
    private Boolean hazardousMaterial;
    private String specialHandling;
    private BigDecimal estimatedValue;
    private BigDecimal actualValue;
    private String priority;
    private Integer tripCount;
    private List<TripSummaryDTO> trips;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // ======================== NEW FIELDS ========================
    
    // Origin & Destination
    private String originLocation;
    private String destinationLocation;
    
    // Handling & Packaging
    private String handlingInstructions;
    private String packagingType;
    private String hazardClass;
    private String temperatureRequirements;
    
    // Preferred Resources (for load grouping)
    private Long preferredVehicleId;
    private String preferredVehicleRegistration;
    private Long preferredDriverId;
    private String preferredDriverName;
    
    // Statistics
    private Integer tripsCount;
    private Integer totalDistanceKm;
    private Integer totalHoursActive;
    private Integer incidentsLogged;
    private Integer completedTrips;
    private Integer pendingTrips;
    private Integer inProgressTrips;
    
    // Insurance
    private String insurancePolicyNumber;
    private LocalDate insuranceExpiry;
    private String customsClearanceStatus;
    
    // Warehouse & Supervision
    private Long warehouseId;
    private String warehouseName;
    private Long supervisorId;
    private String supervisorName;
    
    // Audit
    private LocalDateTime lastStatusUpdate;
    private String auditTrail;
    
    // ======================== DEPOT TRACKING ========================
    private BigDecimal totalFromDepotKm;
    private BigDecimal totalToDepotKm;
    private BigDecimal totalDepotKm;
    
    // Merge suggestion fields
    private Boolean mergeSuggestion;
    private String mergeMessage;
    
    // Calculated fields
    private BigDecimal totalWeight;
    private BigDecimal totalValue;
    private String statusDisplay;
    private Boolean isActive;
    private Boolean canAcceptTrip;
}
