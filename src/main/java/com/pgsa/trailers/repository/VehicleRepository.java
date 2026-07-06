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
    
    // Find by registration number (case insensitive)
    Optional<Vehicle> findByRegistrationNumberIgnoreCase(String registrationNumber);

    // Find by VIN
    Optional<Vehicle> findByVin(String vin);
    
    // Find by VIN (case insensitive)
    Optional<Vehicle> findByVinIgnoreCase(String vin);

    // Find by fleet number
    Optional<Vehicle> findByFleetNumber(String fleetNumber);

    // Find vehicles by status (String)
    List<Vehicle> findByStatus(String status);

    // Find vehicles by status (Enum)
    List<Vehicle> findByVehicleStatus(VehicleStatus status);

    // Find active vehicles by status list
    List<Vehicle> findByStatusIn(List<String> statuses);

    // Find active vehicles by status enum list
    List<Vehicle> findByVehicleStatusIn(List<VehicleStatus> statuses);

    // Find vehicles by vehicle type
    List<Vehicle> findByVehicleType(VehicleType vehicleType);

    // Find vehicles by make
    List<Vehicle> findByMakeContainingIgnoreCase(String make);

    // Find vehicles by model
    List<Vehicle> findByModelContainingIgnoreCase(String model);

    // Search vehicles by registration, make, or model
    @Query("SELECT v FROM Vehicle v WHERE " +
           "LOWER(v.registrationNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(v.make) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(v.model) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(v.vin) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Vehicle> searchVehicles(@Param("searchTerm") String searchTerm);

    // Find vehicles with assigned driver
    List<Vehicle> findByAssignedDriverIsNotNull();

    // Find vehicles without assigned driver
    List<Vehicle> findByAssignedDriverIsNull();

    // Find vehicles by assigned driver ID
    List<Vehicle> findByAssignedDriverId(Long driverId);

    // Find vehicles by maintenance status
    List<Vehicle> findByMaintenanceStatus(String maintenanceStatus);

    // Find vehicles by isActive
    List<Vehicle> findByIsActiveTrue();

    // Find vehicles by isActive and status
    List<Vehicle> findByIsActiveTrueAndStatus(VehicleStatus status);

    // Count vehicles by status
    long countByStatus(VehicleStatus status);

    // Find vehicles with upcoming service (next 30 days)
    @Query("SELECT v FROM Vehicle v WHERE v.nextServiceDue BETWEEN CURRENT_DATE AND CURRENT_DATE + 30")
    List<Vehicle> findVehiclesWithUpcomingService();

    // Find vehicles that are overdue for service
    @Query("SELECT v FROM Vehicle v WHERE v.nextServiceDue < CURRENT_DATE")
    List<Vehicle> findVehiclesOverdueForService();

    // Find vehicles with expired insurance
    @Query("SELECT v FROM Vehicle v WHERE v.insuranceExpiry < CURRENT_DATE")
    List<Vehicle> findVehiclesWithExpiredInsurance();

    // Find vehicles with expired roadworthy
    @Query("SELECT v FROM Vehicle v WHERE v.roadworthyExpiry < CURRENT_DATE")
    List<Vehicle> findVehiclesWithExpiredRoadworthy();

    // Find vehicles by year range
    List<Vehicle> findByYearBetween(Integer startYear, Integer endYear);

    // Find vehicles by fuel type
    List<Vehicle> findByFuelType(String fuelType);

    // Find vehicles with low mileage (less than specified)
    List<Vehicle> findByCurrentMileageLessThan(Double mileage);

    // Find vehicles with high mileage (greater than specified)
    List<Vehicle> findByCurrentMileageGreaterThan(Double mileage);
}
