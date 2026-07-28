// src/main/java/com/pgsa/trailers/entity/inventory/VehicleIssueItem.java
package com.pgsa.trailers.entity.inventory;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_issue_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleIssueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private VehicleIssue issue;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "quantity_issued", nullable = false)
    private BigDecimal quantityIssued;

    @Column(name = "quantity_returned")
    @Builder.Default
    private BigDecimal quantityReturned = BigDecimal.ZERO;

    @Column(name = "condition_issued", length = 50)
    private String conditionIssued;

    @Column(name = "condition_returned", length = 50)
    private String conditionReturned;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
