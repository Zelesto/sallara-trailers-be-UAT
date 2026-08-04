// src/main/java/com/pgsa/trailers/entity/assets/VehicleFuelStatus.java
package com.pgsa.trailers.entity.assets;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_fuel_status")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleFuelStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "tank_number")
    private Integer tankNumber;

    @Column(name = "current_level")
    private BigDecimal currentLevel;

    @Column(name = "capacity")
    private BigDecimal capacity;

    @Column(name = "last_refill_date")
    private LocalDate lastRefillDate;

    @Column(name = "last_refill_odometer")
    private BigDecimal lastRefillOdometer;

    @Column(name = "estimated_range")
    private BigDecimal estimatedRange;

    @Column(name = "percentage_full")
    private BigDecimal percentageFull;

    @Column(name = "status")
    private String status; // NORMAL, LOW, CRITICAL, EMPTY

    @Version
    @Column(name = "version")
    private Integer version;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
