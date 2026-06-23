// src/main/java/com/pgsa/trailers/entity/ops/Load.java
package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.config.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "load", indexes = {
        @Index(name = "idx_load_load_number", columnList = "load_number", unique = true),
        @Index(name = "idx_load_status", columnList = "status"),
        @Index(name = "idx_load_customer", columnList = "customer_id"),
        @Index(name = "idx_load_loading_date", columnList = "loading_date")
})
public class Load extends BaseEntity {

    @Column(name = "load_number", unique = true, nullable = false, length = 50)
    private String loadNumber;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /* ========================
       Customer Relationship
       ======================== */
    @Column(name = "customer_id")
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    @Column(name = "weight_kg", precision = 10, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "volume_cubic_m", precision = 10, scale = 2)
    private BigDecimal volumeCubicM;

    @Column(name = "loading_date")
    private LocalDateTime loadingDate;

    @Column(name = "unloading_date")
    private LocalDateTime unloadingDate;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "commodity_type", length = 100)
    private String commodityType;

    @Column(name = "pallet_count")
    private Integer palletCount;

    @Column(name = "container_number", length = 50)
    private String containerNumber;

    @Column(name = "hazardous_material")
    private Boolean hazardousMaterial = false;

    @Column(name = "special_handling", columnDefinition = "TEXT")
    private String specialHandling;

    @Column(name = "estimated_value", precision = 15, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "actual_value", precision = 15, scale = 2)
    private BigDecimal actualValue;

    @Column(name = "priority", length = 20)
    private String priority;

    @OneToMany(mappedBy = "load", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trip> trips = new ArrayList<>();

    // Helper methods
    public void addTrip(Trip trip) {
        trips.add(trip);
        trip.setLoad(this);
        // Also set the load denormalized fields on trip
        trip.setLoadNumber(this.loadNumber);
        trip.setLoadType(this.getType());
        trip.setLoadDescription(this.description);
        trip.setLoadStatus(this.status);
    }

    public void removeTrip(Trip trip) {
        trips.remove(trip);
        trip.setLoad(null);
        trip.setLoadNumber(null);
        trip.setLoadType(null);
        trip.setLoadDescription(null);
        trip.setLoadStatus(null);
    }

    public boolean isEmpty() {
        return trips == null || trips.isEmpty();
    }

    public int getTripCount() {
        return trips != null ? trips.size() : 0;
    }

    public BigDecimal getTotalWeight() {
        if (weightKg != null) {
            return weightKg;
        }
        // Calculate from trips if not set
        if (trips != null && !trips.isEmpty()) {
            return trips.stream()
                    .map(Trip::getCargoWeight)
                    .filter(w -> w != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getTotalValue() {
        if (actualValue != null) {
            return actualValue;
        }
        // Calculate from trips if not set
        if (trips != null && !trips.isEmpty()) {
            return trips.stream()
                    .map(Trip::getCargoValue)
                    .filter(v -> v != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return BigDecimal.ZERO;
    }

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = "PENDING";
        }
        if (priority == null) {
            priority = "NORMAL";
        }
        if (hazardousMaterial == null) {
            hazardousMaterial = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Update all trips with load details when load is updated
        if (trips != null && !trips.isEmpty()) {
            for (Trip trip : trips) {
                trip.setLoadNumber(this.loadNumber);
                trip.setLoadType(this.getType());
                trip.setLoadDescription(this.description);
                trip.setLoadStatus(this.status);
            }
        }
    }

    // Helper to get type from commodity or other source
    public String getType() {
        // Could be derived from commodity type or a separate field
        if (commodityType != null && !commodityType.isEmpty()) {
            return commodityType;
        }
        return "GENERAL";
    }
}
