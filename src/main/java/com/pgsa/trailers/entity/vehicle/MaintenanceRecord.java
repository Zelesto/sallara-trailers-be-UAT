// src/main/java/com/pgsa/trailers/entity/vehicle/MaintenanceRecord.java

package com.pgsa.trailers.entity.vehicle;

import com.pgsa.trailers.entity.assets.Vehicle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "vehicle_maintenance_schedule")
public class MaintenanceRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;
    
    // ✅ FIX: Use maintenance_type (matches database column name)
    @Column(name = "maintenance_type", nullable = false, length = 100)
    private String type;
    
    @Column(name = "scheduled_date")
    private LocalDate date;
    
    @Column(name = "scheduled_odometer", precision = 12, scale = 2)
    private BigDecimal odometer;
    
    @Column(name = "cost", precision = 15, scale = 2)
    private BigDecimal cost;
    
    @Column(name = "status", length = 50)
    private String status = "SCHEDULED";
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "completed_date")
    private LocalDate completedDate;
    
    @Column(name = "completed_odometer", precision = 12, scale = 2)
    private BigDecimal completedOdometer;
    
    @Column(name = "service_provider", length = 255)
    private String serviceProvider;
    
    @Column(name = "priority", length = 50)
    private String priority = "MEDIUM";
    
    @Column(name = "reminder_days")
    private Integer reminderDays;
    
    @Column(name = "is_recurring")
    private Boolean isRecurring = false;
    
    @Column(name = "recurrence_interval_days")
    private Integer recurrenceIntervalDays;
    
    @Column(name = "recurrence_interval_km", precision = 12, scale = 2)
    private BigDecimal recurrenceIntervalKm;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Add @PrePersist and @PreUpdate methods
    @PrePersist
    protected void onCreate() {
        if (type == null) {
            type = "SERVICE";  // Default value
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
