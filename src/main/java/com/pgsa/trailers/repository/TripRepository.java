// src/main/java/com/pgsa/trailers/repository/TripRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.enums.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    // ======================== FIND BY STATUS ========================
    
    List<Trip> findByStatus(TripStatus status);
    
    // ⭐ Sort by ID descending (latest first)
    List<Trip> findByStatusOrderByIdDesc(TripStatus status);
    
    Page<Trip> findByStatus(TripStatus status, Pageable pageable);
/**
     * Find trips with multiple statuses with pagination
     */
    @Query("SELECT t FROM Trip t WHERE t.status IN :statuses")
    Page<Trip> findByStatusIn(@Param("statuses") List<TripStatus> statuses, Pageable pageable);
    
    /**
     * Find trips with multiple statuses (no pagination)
     */
    @Query("SELECT t FROM Trip t WHERE t.status IN :statuses")
    List<Trip> findTripsByStatusIn(@Param("statuses") List<TripStatus> statuses);
    
    /**
     * Find trips with multiple statuses, sorted by ID descending
     */
    @Query("SELECT t FROM Trip t WHERE t.status IN :statuses ORDER BY t.id DESC")
    List<Trip> findTripsByStatusInOrderByIdDesc(@Param("statuses") List<TripStatus> statuses);
    
    /**
     * Find trips without load with multiple statuses and pagination
     */
    @Query("SELECT t FROM Trip t WHERE (t.loadId IS NULL OR t.loadId = '') AND t.status IN :statuses")
    Page<Trip> findByLoadIdIsNullAndStatusIn(@Param("statuses") List<TripStatus> statuses, Pageable pageable);
    
    // ======================== FIND ALL WITH SORTING ========================
    
    // ⭐ Get all trips sorted by ID descending (latest first)
    @Query("SELECT t FROM Trip t ORDER BY t.id DESC")
    List<Trip> findAllOrderByIdDesc();
    
    // ⭐ Get all trips with pagination sorted by ID descending
    @Query("SELECT t FROM Trip t ORDER BY t.id DESC")
    Page<Trip> findAllOrderByIdDesc(Pageable pageable);
    
    // ======================== FIND BY RELATIONSHIPS ========================
    
    List<Trip> findByDriverId(Long driverId);
    
    // ⭐ Sort by ID descending
    List<Trip> findByDriverIdOrderByIdDesc(Long driverId);
    
    List<Trip> findByVehicleId(Long vehicleId);
    
    // ⭐ Sort by ID descending
    List<Trip> findByVehicleIdOrderByIdDesc(Long vehicleId);
    
    // loadId is String (load number)
    List<Trip> findByLoadId(String loadId);
    
    List<Trip> findByDriverIdAndVehicleId(Long driverId, Long vehicleId);
    
    List<Trip> findByDriverIdAndStatus(Long driverId, TripStatus status);
    
    List<Trip> findByVehicleIdAndStatus(Long vehicleId, TripStatus status);
    
    List<Trip> findByDriverIdAndVehicleIdAndStatus(Long driverId, Long vehicleId, TripStatus status);
    
    // ======================== FIND BY TRIP NUMBER ========================
    
    Optional<Trip> findByTripNumber(String tripNumber);

    @Query("SELECT t.tripNumber FROM Trip t WHERE t.id = :tripId")
    Optional<String> findTripNumberById(@Param("tripId") Long tripId);
    
    boolean existsByTripNumber(String tripNumber);
    
    // ======================== LOAD QUERIES ========================
    
    Page<Trip> findByLoadIdIsNull(Pageable pageable);

    @Query("SELECT t FROM Trip t WHERE t.loadId IS NULL OR t.loadId = ''")
    Page<Trip> findByLoadIdIsNullOrEmpty(Pageable pageable);
    
    @Query("SELECT t FROM Trip t WHERE t.loadId IS NULL OR t.loadId = '' ORDER BY t.id DESC")
    List<Trip> findTripsWithoutLoadOrderByIdDesc();
    
    @Query("SELECT t FROM Trip t WHERE t.customerId = :customerId " +
           "AND t.plannedStartDate BETWEEN :startDate AND :endDate " +
           "AND (t.loadId IS NULL OR t.loadId = '')")
    List<Trip> findByCustomerIdAndPlannedStartDateBetweenAndLoadIsNull(
        @Param("customerId") Long customerId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    Page<Trip> findByCustomerId(Long customerId, Pageable pageable);

    List<Trip> findByLoadNumber(String loadNumber);

    Long countByCustomerId(Long customerId);

    @Query("SELECT t FROM Trip t WHERE t.loadId IS NULL OR t.loadId = ''")
    List<Trip> findTripsWithoutLoad();

    @Query("SELECT t FROM Trip t WHERE t.loadId IS NULL OR t.loadId = ''")
    Page<Trip> findTripsWithoutLoad(Pageable pageable);

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.loadId = :loadId")
    long countByLoadId(@Param("loadId") String loadId);
    
    // ======================== ADVANCED QUERIES ========================
    
    @Query("SELECT t FROM Trip t WHERE " +
            "(:driverId IS NULL OR t.driver.id = :driverId) AND " +
            "(:vehicleId IS NULL OR t.vehicle.id = :vehicleId) AND " +
            "(:status IS NULL OR t.status = :status)")
    Page<Trip> findByFilters(@Param("driverId") Long driverId,
                             @Param("vehicleId") Long vehicleId,
                             @Param("status") TripStatus status,
                             Pageable pageable);
    
    @Query("SELECT t FROM Trip t WHERE " +
            "(:driverId IS NULL OR t.driver.id = :driverId) AND " +
            "(:vehicleId IS NULL OR t.vehicle.id = :vehicleId) AND " +
            "(:status IS NULL OR t.status = :status) " +
            "ORDER BY t.id DESC")
    List<Trip> findByFiltersOrderByIdDesc(@Param("driverId") Long driverId,
                                          @Param("vehicleId") Long vehicleId,
                                          @Param("status") TripStatus status);
    
    @Query("SELECT t FROM Trip t WHERE t.plannedStartDate BETWEEN :startDate AND :endDate")
    List<Trip> findByPlannedStartDateBetween(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Trip t WHERE t.plannedStartDate BETWEEN :startDate AND :endDate ORDER BY t.id DESC")
    List<Trip> findByPlannedStartDateBetweenOrderByIdDesc(@Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Trip t WHERE t.actualStartDate BETWEEN :startDate AND :endDate")
    List<Trip> findByActualStartDateBetween(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Trip t WHERE t.actualStartDate BETWEEN :startDate AND :endDate ORDER BY t.id DESC")
    List<Trip> findByActualStartDateBetweenOrderByIdDesc(@Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Trip t WHERE t.driver.id = :driverId AND t.plannedStartDate BETWEEN :startDate AND :endDate")
    List<Trip> findDriverTripsBetweenDates(@Param("driverId") Long driverId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Trip t WHERE t.driver.id = :driverId AND t.plannedStartDate BETWEEN :startDate AND :endDate ORDER BY t.id DESC")
    List<Trip> findDriverTripsBetweenDatesOrderByIdDesc(@Param("driverId") Long driverId,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Trip t WHERE t.status IN :statuses")
    List<Trip> findByStatusIn(@Param("statuses") List<TripStatus> statuses);
    
    @Query("SELECT t FROM Trip t WHERE t.status IN :statuses ORDER BY t.id DESC")
    List<Trip> findByStatusInOrderByIdDesc(@Param("statuses") List<TripStatus> statuses);
    
    @Query("SELECT t FROM Trip t WHERE t.status = :status AND t.plannedStartDate <= :date")
    List<Trip> findPlannedTripsUpToDate(@Param("status") TripStatus status,
                                         @Param("date") LocalDateTime date);
    
    @Query("SELECT t FROM Trip t WHERE t.status = :status AND t.plannedStartDate <= :date ORDER BY t.id DESC")
    List<Trip> findPlannedTripsUpToDateOrderByIdDesc(@Param("status") TripStatus status,
                                                     @Param("date") LocalDateTime date);
    
    // ======================== ACTIVE TRIPS ========================
    
    @Query("SELECT t FROM Trip t WHERE t.status IN ('PLANNED', 'ASSIGNED', 'IN_PROGRESS', 'ACTIVE')")
    List<Trip> findActiveTrips();
    
    @Query("SELECT t FROM Trip t WHERE t.status IN ('PLANNED', 'ASSIGNED', 'IN_PROGRESS', 'ACTIVE') ORDER BY t.id DESC")
    List<Trip> findActiveTripsOrderByIdDesc();
    
    @Query("SELECT t FROM Trip t WHERE t.status = 'IN_PROGRESS' OR t.status = 'ACTIVE'")
    List<Trip> findCurrentlyRunningTrips();
    
    @Query("SELECT t FROM Trip t WHERE t.status = 'IN_PROGRESS' OR t.status = 'ACTIVE' ORDER BY t.id DESC")
    List<Trip> findCurrentlyRunningTripsOrderByIdDesc();
    
    // ======================== SEARCH QUERIES ========================


   // ⭐ Safe search that handles null/empty search terms - FIXED
@Query("SELECT t FROM Trip t WHERE " +
       "(:searchTerm IS NULL OR :searchTerm = '' OR " +
       "LOWER(t.tripNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
       "LOWER(t.originCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
       "LOWER(t.destinationCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
       "LOWER(t.customer.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
Page<Trip> searchTripsSafe(@Param("searchTerm") String searchTerm, Pageable pageable);

// ⭐ Also fix the main searchTrips method
@Query("SELECT t FROM Trip t WHERE " +
       "(:searchTerm IS NULL OR :searchTerm = '' OR " +
       "LOWER(t.tripNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
       "LOWER(t.originCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
       "LOWER(t.destinationCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
       "LOWER(t.customer.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
Page<Trip> searchTrips(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // ⭐ Search trips with default sorting by ID descending (no pagination)
    @Query("SELECT t FROM Trip t WHERE " +
           "LOWER(t.tripNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.originCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.destinationCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.customer.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY t.id DESC")
    List<Trip> searchTripsOrderByIdDesc(@Param("searchTerm") String searchTerm);
    
    // ⭐ Advanced search with filters
    @Query("SELECT t FROM Trip t WHERE " +
           "(:searchTerm IS NULL OR " +
           "LOWER(t.tripNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.originCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.destinationCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.customer.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:city IS NULL OR LOWER(t.originCity) = LOWER(:city) OR LOWER(t.destinationCity) = LOWER(:city)) " +
           "AND (:customer IS NULL OR LOWER(t.customer.name) = LOWER(:customer))")
    Page<Trip> findWithFilters(@Param("searchTerm") String searchTerm,
                               @Param("status") TripStatus status,
                               @Param("city") String city,
                               @Param("customer") String customer,
                               Pageable pageable);
    
    // ⭐ Advanced search with filters and default sorting
    @Query("SELECT t FROM Trip t WHERE " +
           "(:searchTerm IS NULL OR " +
           "LOWER(t.tripNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.originCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.destinationCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.customer.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:city IS NULL OR LOWER(t.originCity) = LOWER(:city) OR LOWER(t.destinationCity) = LOWER(:city)) " +
           "AND (:customer IS NULL OR LOWER(t.customer.name) = LOWER(:customer)) " +
           "ORDER BY t.id DESC")
    Page<Trip> findWithFiltersOrderByIdDesc(@Param("searchTerm") String searchTerm,
                                            @Param("status") TripStatus status,
                                            @Param("city") String city,
                                            @Param("customer") String customer,
                                            Pageable pageable);
    
    // ======================== UPDATE QUERIES ========================
    
    @Modifying
    @Query("UPDATE Trip t SET t.status = :newStatus, t.lastStatusUpdate = :now WHERE t.id = :tripId")
    int updateStatus(@Param("tripId") Long tripId,
                     @Param("newStatus") TripStatus newStatus,
                     @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE Trip t SET t.status = :newStatus, t.lastStatusUpdate = :now WHERE t.id = :tripId AND t.status = :currentStatus")
    int updateStatusIfCurrent(@Param("tripId") Long tripId,
                              @Param("newStatus") TripStatus newStatus,
                              @Param("currentStatus") TripStatus currentStatus,
                              @Param("now") LocalDateTime now);
    
    // ======================== COUNT QUERIES ========================
    
    long countByStatus(TripStatus status);
    
    long countByDriverIdAndStatus(Long driverId, TripStatus status);
    
    long countByVehicleIdAndStatus(Long vehicleId, TripStatus status);
    
    @Query("SELECT COUNT(t) FROM Trip t WHERE t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate")
    long countByStatusAndDateRange(@Param("status") TripStatus status,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
    
    // ======================== AGGREGATION QUERIES ========================

    @Query("SELECT MAX(t.tripNumber) FROM Trip t WHERE t.tripNumber LIKE CONCAT('TRP-', :year, '-%')")
    String findMaxTripNumberForYear(@Param("year") int year);
    
    @Query("SELECT AVG(t.actualDistanceKm) FROM Trip t WHERE t.status = 'COMPLETED' AND t.vehicle.id = :vehicleId")
    Optional<Double> getAverageDistanceForVehicle(@Param("vehicleId") Long vehicleId);
    
    @Query("SELECT SUM(t.actualDistanceKm) FROM Trip t WHERE t.status = 'COMPLETED' AND t.driver.id = :driverId")
    Optional<BigDecimal> getTotalDistanceForDriver(@Param("driverId") Long driverId);

    // ======================== EXISTS QUERIES ========================
    
    boolean existsByDriverIdAndStatus(Long driverId, TripStatus status);
    
    boolean existsByVehicleIdAndStatus(Long vehicleId, TripStatus status);
}
