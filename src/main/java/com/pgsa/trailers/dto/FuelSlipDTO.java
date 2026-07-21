package com.pgsa.trailers.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgsa.trailers.entity.ops.FuelSlip;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Data
public class FuelSlipDTO {
    private Long id;
    private String slipNumber;
    private Long vehicleId;
    private String vehicleRegNumber;
    private Long driverId;
    private String driverName;
    private Long fuelSourceId;
    private String fuelSourceName;
    private LocalDateTime transactionDate;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private BigDecimal odometerReading;
    private String location;
    private String stationName;
    private String pumpNumber;
    private String notes;
    private Boolean finalized;
    private Long loadId;
    private Long tripId;
    private String fuelType;
    private String paymentMethod;
    private String receiptNumber;
    private String verifiedBy;
    private LocalDateTime verificationDate;
    private Boolean incidentFlag;
    private LocalDateTime lastStatusUpdate;
    private Map<String, Object> auditTrail;
    private Long accountStatementId;

    // Static ObjectMapper for JSON operations
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convert FuelSlip entity to DTO with safe lazy loading
     */
    public static FuelSlipDTO fromEntity(FuelSlip slip) {
        if (slip == null) return null;

        FuelSlipDTO dto = new FuelSlipDTO();

        try {
            // Basic fields
            dto.setId(slip.getId());
            dto.setSlipNumber(slip.getSlipNumber());

            // Vehicle mapping - safely handle lazy loading
            if (slip.getVehicle() != null) {
                dto.setVehicleId(slip.getVehicle().getId());
                try {
                    dto.setVehicleRegNumber(slip.getVehicle().getRegistrationNumber());
                } catch (Exception e) {
                    log.debug("Could not load vehicle registration for fuel slip: {}", slip.getId());
                    dto.setVehicleRegNumber(null);
                }
            }

            // Driver mapping - safely handle lazy loading
            if (slip.getDriver() != null) {
                dto.setDriverId(slip.getDriver().getId());
                try {
                    // Try to get full name using getFullName() method
                    dto.setDriverName(slip.getDriver().getFullName());
                } catch (Exception e) {
                    // Fallback: build name manually
                    try {
                        String firstName = slip.getDriver().getFirstName() != null ? slip.getDriver().getFirstName() : "";
                        String lastName = slip.getDriver().getLastName() != null ? slip.getDriver().getLastName() : "";
                        String fullName = (firstName + " " + lastName).trim();
                        dto.setDriverName(fullName.isEmpty() ? null : fullName);
                    } catch (Exception ex) {
                        log.debug("Could not load driver name for fuel slip: {}", slip.getId());
                        dto.setDriverName(null);
                    }
                }
            }

            // Fuel source mapping - safely handle lazy loading
            if (slip.getFuelSource() != null) {
                dto.setFuelSourceId(slip.getFuelSource().getId());
                try {
                    dto.setFuelSourceName(slip.getFuelSource().getName());
                } catch (Exception e) {
                    log.debug("Could not load fuel source name for fuel slip: {}", slip.getId());
                    dto.setFuelSourceName(null);
                }
            }

            // Transaction details
            dto.setTransactionDate(slip.getTransactionDate());
            dto.setQuantity(slip.getQuantity());
            dto.setUnitPrice(slip.getUnitPrice());
            dto.setTotalAmount(slip.getTotalAmount());
            dto.setOdometerReading(slip.getOdometerReading());
            dto.setLocation(slip.getLocation());
            dto.setStationName(slip.getStationName());
            dto.setPumpNumber(slip.getPumpNumber());
            dto.setNotes(slip.getNotes());
            dto.setFinalized(slip.getFinalized());
            dto.setLoadId(slip.getLoadId());
            dto.setTripId(slip.getTripId());
            dto.setFuelType(slip.getFuelType());
            dto.setPaymentMethod(slip.getPaymentMethod());
            dto.setReceiptNumber(slip.getReceiptNumber());
            dto.setVerifiedBy(slip.getVerifiedBy());
            dto.setVerificationDate(slip.getVerificationDate());
            dto.setIncidentFlag(slip.getIncidentFlag());
            dto.setLastStatusUpdate(slip.getLastStatusUpdate());

            // Convert auditTrail to Map
            dto.setAuditTrail(convertAuditTrailToMap(slip.getAuditTrail()));

            // Account statement mapping
            if (slip.getAccountStatement() != null) {
                dto.setAccountStatementId(slip.getAccountStatement().getId());
            }

        } catch (Exception e) {
            log.error("Error converting FuelSlip to DTO for ID: {}", slip.getId(), e);
            // Return what we have so far
        }

        return dto;
    }

    /**
     * Convert audit trail object to Map
     */
    private static Map<String, Object> convertAuditTrailToMap(Object auditTrail) {
        Map<String, Object> result = new HashMap<>();

        if (auditTrail == null) {
            return result;
        }

        try {
            if (auditTrail instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> auditMap = (Map<String, Object>) auditTrail;
                return auditMap;
            } else if (auditTrail instanceof String) {
                String strAuditTrail = (String) auditTrail;
                if (!strAuditTrail.trim().isEmpty()) {
                    return objectMapper.readValue(strAuditTrail, new TypeReference<Map<String, Object>>() {});
                }
            } else {
                String json = objectMapper.writeValueAsString(auditTrail);
                return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            result.put("error", "Failed to parse audit trail");
            result.put("originalType", auditTrail.getClass().getSimpleName());
        }

        return result;
    }

    /**
     * Get audit trail as JSON string
     */
    public String getAuditTrailAsJson() {
        if (this.auditTrail == null || this.auditTrail.isEmpty()) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(this.auditTrail);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
