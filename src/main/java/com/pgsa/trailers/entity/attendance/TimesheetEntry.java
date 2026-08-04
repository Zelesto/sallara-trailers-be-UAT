// src/main/java/com/pgsa/trailers/entity/attendance/TimesheetEntry.java
package com.pgsa.trailers.entity.attendance;

import com.pgsa.trailers.entity.assets.Driver;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "timesheet_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "break_duration")
    private Integer breakDuration; // in minutes

    @Column(name = "total_hours")
    private BigDecimal totalHours;

    @Column(name = "activity_type", nullable = false)
    private String activityType; // DRIVING, REST, LOADING, UNLOADING, MAINTENANCE, TRAINING, OTHER

    @Column(name = "status")
    private String status; // ACTIVE, SUBMITTED, APPROVED, REJECTED

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Punch clock fields
    @Column(name = "clock_in_time")
    private LocalDateTime clockInTime;

    @Column(name = "clock_out_time")
    private LocalDateTime clockOutTime;

    @Column(name = "break_start_time")
    private LocalDateTime breakStartTime;

    @Column(name = "break_end_time")
    private LocalDateTime breakEndTime;

    @Column(name = "punch_status")
    private String punchStatus; // CLOCKED_OUT, CLOCKED_IN, ON_BREAK

    @Column(name = "punch_location")
    private String punchLocation;

    @Column(name = "punch_latitude")
    private BigDecimal punchLatitude;

    @Column(name = "punch_longitude")
    private BigDecimal punchLongitude;

    @Column(name = "is_active")
    private Boolean isActive;

    @Version
    @Column(name = "version")
    private Integer version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
