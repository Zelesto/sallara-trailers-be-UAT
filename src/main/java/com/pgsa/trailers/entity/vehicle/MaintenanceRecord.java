// src/main/java/com/pgsa/trailers/entity/vehicle/MaintenanceRecord.java
package com.pgsa.trailers.entity.vehicle;

import com.pgsa.trailers.entity.assets.Vehicle;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_maintenance_schedule")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "scheduled_date")
    private LocalDate date;

    @Column(name = "scheduled_odometer")
    private BigDecimal odometer;

    @Column(name = "cost")
    private BigDecimal cost;

    @Column(name = "status")
    private String status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "completed_odometer")
    private BigDecimal completedOdometer;

    @Column(name = "service_provider")
    private String serviceProvider;

    @Column(name = "priority")
    private String priority;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Add @PrePersist and @PreUpdate methods
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
}
}
