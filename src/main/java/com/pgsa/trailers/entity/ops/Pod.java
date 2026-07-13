// src/main/java/com/pgsa/trailers/entity/ops/Pod.java
package com.pgsa.trailers.entity.ops;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pods")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pod_number", unique = true, nullable = false)
    private String podNumber;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "trip_number")
    private String tripNumber;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "driver_name")
    private String driverName;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "status")
    private String status;

    @Column(name = "source")
    private String source;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "file_size")
    private String fileSize;

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "document_reference")  // NEW FIELD
    private String documentReference;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "rejected_by")
    private String rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "debriefed_by")
    private String debriefedBy;

    @Column(name = "debriefed_at")
    private LocalDateTime debriefedAt;

    @Column(name = "received_by")
    private String receivedBy;

    @Column(name = "quality_rating")
    private Integer qualityRating;

    @Column(name = "issues_found")
    private String issuesFound;

    @Column(name = "delivery_condition")
    private String deliveryCondition;

    @Column(name = "debrief_notes", columnDefinition = "TEXT")
    private String debriefNotes;

    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        if (podNumber == null) {
            podNumber = "POD-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
        if (source == null) {
            source = "UPLOADED";
        }
        if (documentType == null) {
            documentType = "PDF";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
