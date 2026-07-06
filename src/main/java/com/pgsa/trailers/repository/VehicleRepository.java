package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.enums.VehicleStatus;
import com.pgsa.trailers.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    // Find by registration number
    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);
    Optional<Vehicle> findByRegistrationNumberIgnoreCase(String registrationNumber);

    // Find by VIN
    Optional<Vehicle> findByVin(String vin);
    Optional<Vehicle> findByVinIgnoreCase(String vin);

    // Find by fleet number
    Optional<Vehicle> findByFleetNumber(String fleetNumber);

    // ✅ KEEP ONLY ONE version of findByStatus
    List<Vehicle> findByStatus(String status);

    // ✅ KEEP ONLY ONE version of findByStatusIn
    List<Vehicle> findByStatusIn(List<String> statuses);

    // Find by vehicle type
    List<Vehicle> findByVehicleType(VehicleType vehicleType);

    // Find by make/model
    List<Vehicle> findByMakeContainingIgnoreCase(String make);
    List<Vehicle> findByModelContainingIgnoreCase(String model);

    // Search
    @Query("SELECT v FROM Vehicle v WHERE " +
           "LOWER(v.registrationNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(v.make) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(v.model) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(v.vin) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Vehicle> searchVehicles(@Param("searchTerm") String searchTerm);

    // Driver related
    List<Vehicle> findByAssignedDriverIsNotNull();
    List<Vehicle> findByAssignedDriverIsNull();
    List<Vehicle> findByAssignedDriverId(Long driverId);

    // Maintenance
    List<Vehicle> findByMaintenanceStatus(String maintenanceStatus);

    // Active status
    List<Vehicle> findByIsActiveTrue();
    List<Vehicle> findByIsActiveTrueAndStatus(VehicleStatus status);

    // Count
    long countByStatus(VehicleStatus status);

    // Service related
    @Query("SELECT v FROM Vehicle v WHERE v.nextServiceDue BETWEEN CURRENT_DATE AND CURRENT_DATE + 30")
    List<Vehicle> findVehiclesWithUpcomingService();

    @Query("SELECT v FROM Vehicle v WHERE v.nextServiceDue < CURRENT_DATE")
    List<Vehicle> findVehiclesOverdueForService();

    // Insurance and roadworthy
    @Query("SELECT v FROM Vehicle v WHERE v.insuranceExpiry < CURRENT_DATE")
    List<Vehicle> findVehiclesWithExpiredInsurance();

    @Query("SELECT v FROM Vehicle v WHERE v.roadworthyExpiry < CURRENT_DATE")
    List<Vehicle> findVehiclesWithExpiredRoadworthy();

    // Other queries
    List<Vehicle> findByYearBetween(Integer startYear, Integer endYear);
    List<Vehicle> findByFuelType(String fuelType);
}
