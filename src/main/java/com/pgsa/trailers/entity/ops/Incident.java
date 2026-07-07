package com.pgsa.trailers.entity.ops;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
@Data
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "incident_type", nullable = false)
    private String incidentType;

    @Column(nullable = false)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(columnDefinition = "TEXT")
    private String description;

    private String location;

    @Column(name = "requires_assistance")
    private Boolean requiresAssistance = false;

    @Column(name = "resolved")
    private Boolean resolved = false;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // ====== NEW PAYMENT FIELDS ======
    
    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "voucher_type", length = 50)
    private String voucherType;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(name = "direction", length = 10)
    private String direction; // IN or OUT

    @Column(name = "additional_notes", columnDefinition = "TEXT")
    private String additionalNotes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (reportedAt == null) {
            reportedAt = LocalDateTime.now();
        }
        if (requiresAssistance == null) {
            requiresAssistance = false;
        }
        if (resolved == null) {
            resolved = false;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ====== BUSINESS METHODS ======
    
    /**
     * Check if this incident has a payment associated
     */
    public boolean hasPayment() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Get payment direction label
     */
    public String getPaymentDirectionLabel() {
        if (direction == null) {
            // Default: OUT for vouchers/expenses, IN for refunds/claims
            if ("VOUCHER".equalsIgnoreCase(incidentType) || 
                "TOLL".equalsIgnoreCase(incidentType) ||
                "FUEL".equalsIgnoreCase(incidentType) ||
                "FOOD".equalsIgnoreCase(incidentType) ||
                "MAINTENANCE".equalsIgnoreCase(incidentType)) {
                return "OUT";
            }
            if ("DEMURRAGE".equalsIgnoreCase(incidentType) ||
                "DETENTION".equalsIgnoreCase(incidentType) ||
                "TOLL_REFUND".equalsIgnoreCase(incidentType)) {
                return "IN";
            }
            return null;
        }
        return direction;
    }

    /**
     * Get formatted amount with currency
     */
    public String getFormattedAmount() {
        if (amount == null) return null;
        return String.format("R %.2f", amount);
    }

    /**
     * Check if this is a voucher
     */
    public boolean isVoucher() {
        return "VOUCHER".equalsIgnoreCase(incidentType);
    }

    /**
     * Check if this is an adverse event
     */
    public boolean isAdverseEvent() {
        return "ADVERSE_EVENT".equalsIgnoreCase(incidentType);
    }

    /**
     * Check if this is an incident
     */
    public boolean isIncident() {
        return !isVoucher() && !isAdverseEvent();
    }

    /**
     * Check if urgent
     */
    public boolean isUrgent() {
        return "CRITICAL".equalsIgnoreCase(severity) || 
               (requiresAssistance != null && requiresAssistance);
    }

    /**
     * Resolve the incident
     */
    public void resolve(String notes) {
        this.resolved = true;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNotes = notes;
    }

    /**
     * Mark for assistance
     */
    public void markForAssistance() {
        this.requiresAssistance = true;
    }
}
