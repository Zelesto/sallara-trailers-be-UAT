package com.pgsa.trailers.entity.assets;

import com.pgsa.trailers.config.BaseEntity;
import com.pgsa.trailers.enums.DriverStatus;
import com.pgsa.trailers.entity.security.AppUser;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(
        name = "driver",
        indexes = {
                @Index(name = "idx_driver_license", columnList = "license_number"),
                @Index(name = "idx_driver_status", columnList = "status"),
                @Index(name = "idx_driver_app_user", columnList = "app_user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_driver_license_number", columnNames = {"license_number"}),
                @UniqueConstraint(name = "uk_driver_app_user", columnNames = {"app_user_id"})
        }
)
public class Driver extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser appUser;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "license_number", unique = true, nullable = false, length = 50)
    private String licenseNumber;

    @Column(name = "license_type", length = 50)
    private String licenseType;

    @Column(name = "license_expiry")
    private LocalDate licenseExpiry;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "email", length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DriverStatus status;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "termination_reason", length = 255)
    private String terminationReason;

    // ====== ADDITIONAL FIELDS FROM DATABASE ======
    
    @Column(name = "employment_type", length = 50)
    private String employmentType;

    @Column(name = "shift_pattern", length = 50)
    private String shiftPattern;

    @Column(name = "assigned_vehicle_id")
    private Long assignedVehicleId;

    @Column(name = "training_completed")
    private Boolean trainingCompleted = false;

    // ⭐ FIX: Change this to use JsonType since the column is JSON type
    @Type(JsonType.class)
    @Column(name = "training_certificates", columnDefinition = "jsonb")
    private Map<String, Object> trainingCertificates = new HashMap<>();

    @Column(name = "medical_clearance_date")
    private LocalDate medicalClearanceDate;

    @Column(name = "next_medical_due")
    private LocalDate nextMedicalDue;

    @Column(name = "incidents_logged")
    private Integer incidentsLogged = 0;

    @Column(name = "total_trips")
    private Integer totalTrips = 0;

    @Column(name = "total_km_travelled", precision = 12, scale = 2)
    private BigDecimal totalKmTravelled;

    @Column(name = "total_hours_active", precision = 12, scale = 2)
    private BigDecimal totalHoursActive;

    @Column(name = "performance_score", precision = 5, scale = 2)
    private BigDecimal performanceScore;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ====== AUDIT TRAIL WITH HIBERNATE TYPES ======
    @Type(JsonType.class)
    @Column(name = "audit_trail", columnDefinition = "jsonb")
    private Map<String, Object> auditTrail = new HashMap<>();

    // ========== CONSTRUCTORS ==========

    public Driver() {
        this.status = DriverStatus.ACTIVE;
        this.incidentsLogged = 0;
        this.totalTrips = 0;
        this.trainingCompleted = false;
        this.trainingCertificates = new HashMap<>();
        this.auditTrail = new HashMap<>();
        this.setIsActive(true);
        this.setVersion(0);
    }

    // ========== HELPER METHODS ==========

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Check if driver's license is expired
     */
    public boolean isLicenseExpired() {
        if (licenseExpiry == null) {
            return false;
        }
        return licenseExpiry.isBefore(LocalDate.now());
    }

    /**
     * Check if license expires within given days
     */
    public boolean isLicenseExpiringWithinDays(int days) {
        if (licenseExpiry == null) {
            return false;
        }
        LocalDate warningDate = LocalDate.now().plusDays(days);
        return !licenseExpiry.isBefore(LocalDate.now()) &&
                !licenseExpiry.isAfter(warningDate);
    }

    /**
     * Check if driver is active
     */
    public boolean isActive() {
        return status == DriverStatus.ACTIVE && super.isActive();
    }

    /**
     * Check if driver is available for assignment
     */
    public boolean isAvailableForAssignment() {
        return isActive() && 
               !isLicenseExpired() && 
               status != DriverStatus.SUSPENDED &&
               status != DriverStatus.ON_LEAVE &&
               assignedVehicleId == null;
    }

    /**
     * Get years of service
     */
    public Integer getYearsOfService() {
        if (hireDate == null) {
            return null;
        }
        return Period.between(hireDate, LocalDate.now()).getYears();
    }

    /**
     * Get driver's age (if date of birth is available in AppUser)
     */
    public Integer getAge() {
        return null;
    }

    /**
     * Validate required fields
     */
    public boolean isValid() {
        return firstName != null && !firstName.trim().isEmpty() &&
                lastName != null && !lastName.trim().isEmpty() &&
                licenseNumber != null && !licenseNumber.trim().isEmpty() &&
                appUser != null;
    }

    /**
     * Get formatted contact information
     */
    public String getContactInfo() {
        StringBuilder sb = new StringBuilder();
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            sb.append("Phone: ").append(phoneNumber);
        }
        if (email != null && !email.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append("Email: ").append(email);
        }
        return sb.length() > 0 ? sb.toString() : "No contact info";
    }

    /**
     * Get driver summary
     */
    public String getSummary() {
        return String.format("%s (%s) - %s",
                getFullName(),
                licenseNumber,
                status != null ? status.name() : "Unknown status");
    }

    // ========== BUSINESS LOGIC METHODS ==========

    /**
     * Activate driver
     */
    public void activate() {
        this.status = DriverStatus.ACTIVE;
        this.terminationDate = null;
        this.terminationReason = null;
        this.setIsActive(true);
    }

    /**
     * Deactivate driver
     */
    public void deactivate(String reason) {
        this.status = DriverStatus.INACTIVE;
        this.terminationDate = LocalDate.now();
        this.terminationReason = reason;
        this.setIsActive(false);
    }

    /**
     * Suspend driver
     */
    public void suspend(String reason) {
        this.status = DriverStatus.SUSPENDED;
        this.terminationReason = reason;
    }

    /**
     * Reinstate suspended driver
     */
    public void reinstate() {
        this.status = DriverStatus.ACTIVE;
        this.terminationReason = null;
        this.setIsActive(true);
    }

    /**
     * Set driver on leave
     */
    public void setOnLeave() {
        this.status = DriverStatus.ON_LEAVE;
    }

    /**
     * Check if driver can be assigned to a vehicle
     */
    public boolean canBeAssigned() {
        return isActive() &&
                !isLicenseExpired() &&
                status != DriverStatus.SUSPENDED &&
                status != DriverStatus.ON_LEAVE &&
                assignedVehicleId == null;
    }

    /**
     * Assign vehicle to driver
     */
    public void assignVehicle(Long vehicleId) {
        this.assignedVehicleId = vehicleId;
    }

    /**
     * Unassign vehicle from driver
     */
    public void unassignVehicle() {
        this.assignedVehicleId = null;
    }

    /**
     * Increment trips count
     */
    public void incrementTrips() {
        this.totalTrips = (totalTrips == null ? 0 : totalTrips) + 1;
    }

    /**
     * Increment incidents count
     */
    public void incrementIncidents() {
        this.incidentsLogged = (incidentsLogged == null ? 0 : incidentsLogged) + 1;
    }

    /**
     * Update total kilometers travelled
     */
    public void addKilometers(BigDecimal km) {
        if (km != null) {
            if (this.totalKmTravelled == null) {
                this.totalKmTravelled = BigDecimal.ZERO;
            }
            this.totalKmTravelled = this.totalKmTravelled.add(km);
        }
    }

    /**
     * Update total hours active
     */
    public void addActiveHours(BigDecimal hours) {
        if (hours != null) {
            if (this.totalHoursActive == null) {
                this.totalHoursActive = BigDecimal.ZERO;
            }
            this.totalHoursActive = this.totalHoursActive.add(hours);
        }
    }

    /**
     * Check if medical clearance is valid
     */
    public boolean isMedicalClearanceValid() {
        return nextMedicalDue == null || !nextMedicalDue.isBefore(LocalDate.now());
    }

    /**
     * Check if medical clearance is expiring within given days
     */
    public boolean isMedicalClearanceExpiringWithinDays(int days) {
        if (nextMedicalDue == null) {
            return false;
        }
        LocalDate warningDate = LocalDate.now().plusDays(days);
        return !nextMedicalDue.isBefore(LocalDate.now()) &&
                !nextMedicalDue.isAfter(warningDate);
    }

    // ========== EQUALS & HASHCODE ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Driver driver = (Driver) o;

        if (getId() != null) {
            return getId().equals(driver.getId());
        }

        return licenseNumber != null && licenseNumber.equals(driver.getLicenseNumber());
    }

    @Override
    public int hashCode() {
        if (getId() != null) {
            return getId().hashCode();
        }
        return licenseNumber != null ? licenseNumber.hashCode() : 0;
    }

    // ========== TO STRING ==========

    @Override
    public String toString() {
        return "Driver{" +
                "id=" + getId() +
                ", fullName='" + getFullName() + '\'' +
                ", licenseNumber='" + licenseNumber + '\'' +
                ", status=" + status +
                ", appUserId=" + (appUser != null ? appUser.getId() : "null") +
                ", isActive=" + getIsActive() +
                '}';
    }

    // ========== LIFECYCLE HOOKS ==========

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = DriverStatus.ACTIVE;
        }
        if (incidentsLogged == null) {
            incidentsLogged = 0;
        }
        if (totalTrips == null) {
            totalTrips = 0;
        }
        if (trainingCompleted == null) {
            trainingCompleted = false;
        }
        if (trainingCertificates == null) {
            trainingCertificates = new HashMap<>();
        }
        if (auditTrail == null) {
            auditTrail = new HashMap<>();
        }
        if (getIsActive() == null) {
            setIsActive(true);
        }
        if (getVersion() == null) {
            setVersion(0);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Any pre-update logic
    }
}
