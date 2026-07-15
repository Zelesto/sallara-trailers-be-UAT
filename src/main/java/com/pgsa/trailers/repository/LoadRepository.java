// src/main/java/com/pgsa/trailers/repository/LoadRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.enums.LoadStatus;
import com.pgsa.trailers.enums.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoadRepository extends JpaRepository<Load, Long> {

    // ======================== FIND BY LOAD NUMBER ========================
    
    Optional<Load> findByLoadNumber(String loadNumber);
    
    boolean existsByLoadNumber(String loadNumber);
    
    // ======================== FIND BY STATUS ========================
    
    // FIXED: Use @Query with LoadStatus enum
    @Query("SELECT l FROM Load l WHERE l.status = :status")
    List<Load> findByStatus(@Param("status") LoadStatus status);
    
    // FIXED: For String status, use a different method name or @Query
    @Query("SELECT l FROM Load l WHERE l.status = :status")
    Page<Load> findLoadsByStatus(@Param("status") String status, Pageable pageable);
    
    // Or use derived query with correct naming
    Page<Load> findByStatus(LoadStatus status, Pageable pageable);
    
    // ======================== FIND BY DATE RANGE ========================
    
    List<Load> findByLoadingDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    List<Load> findByUnloadingDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // ======================== FIND BY TRIP ========================
    
    @Query("SELECT DISTINCT l FROM Load l JOIN l.trips t WHERE t.id = :tripId")
    Optional<Load> findByTripId(@Param("tripId") Long tripId);
    
    @Query("SELECT l FROM Load l JOIN l.trips t WHERE t.status = :status")
    List<Load> findByTripStatus(@Param("status") TripStatus status);
    
    // ======================== ACTIVE LOADS ========================
    
    @Query("SELECT l FROM Load l WHERE l.status IN ('PENDING', 'IN_TRANSIT', 'LOADING')")
    List<Load> findActiveLoads();
    
    @Query("SELECT l FROM Load l WHERE l.loadingDate <= :now AND (l.unloadingDate IS NULL OR l.unloadingDate > :now)")
    List<Load> findCurrentLoads(@Param("now") LocalDateTime now);

    Optional<Load> findByReferenceNumber(String referenceNumber);
    
    // ======================== CUSTOMER ========================
       
    List<Load> findByCustomerId(Long customerId);
    
    @Query("SELECT l FROM Load l WHERE l.customerId = :customerId AND l.loadingDate BETWEEN :startDate AND :endDate")
    List<Load> findByCustomerIdAndLoadingDateBetween(
            @Param("customerId") Long customerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ======================== COUNT QUERIES ========================
    
    // FIXED: Use @Query with LoadStatus enum
    @Query("SELECT COUNT(l) FROM Load l WHERE l.status = :status")
    long countByStatus(@Param("status") LoadStatus status);
    
    // FIXED: For String status
    @Query("SELECT COUNT(l) FROM Load l WHERE l.status = :status")
    long countByStatusString(@Param("status") String status);
    
    @Query("SELECT COUNT(l) FROM Load l WHERE l.status = :status AND l.createdAt BETWEEN :startDate AND :endDate")
    long countByStatusAndDateRange(@Param("status") LoadStatus status,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
    
    // ======================== AGGREGATION QUERIES ========================
    
    @Query("SELECT SUM(l.weightKg) FROM Load l WHERE l.status = 'COMPLETED'")
    Optional<Double> getTotalWeightOfCompletedLoads();
    
    @Query("SELECT AVG(l.weightKg) FROM Load l WHERE l.status = 'COMPLETED'")
    Optional<Double> getAverageWeightOfCompletedLoads();
    
    // ======================== EXISTENCE CHECKS ========================
    
    // FIXED: Use @Query with LoadStatus enum
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Load l WHERE l.loadNumber = :loadNumber AND l.status = :status")
    boolean existsByLoadNumberAndStatus(@Param("loadNumber") String loadNumber, @Param("status") LoadStatus status);
    
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Load l JOIN l.trips t WHERE t.id = :tripId")
    boolean hasLoadAssociatedWithTrip(@Param("tripId") Long tripId);
    
    // ======================== BULK OPERATIONS ========================
    
    // FIXED: Use @Query with LoadStatus enum
    @Query("UPDATE Load l SET l.status = :newStatus WHERE l.id IN :loadIds AND l.status = :currentStatus")
    int updateStatusBulk(@Param("loadIds") List<Long> loadIds,
                         @Param("newStatus") LoadStatus newStatus,
                         @Param("currentStatus") LoadStatus currentStatus);
    
    // ======================== SEARCH ========================
    
    // Simple search with one parameter
    @Query("SELECT l FROM Load l WHERE " +
           "LOWER(l.loadNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.commodityType) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Load> searchLoads(@Param("search") String search, Pageable pageable);
    
    // Advanced search with multiple parameters
    @Query("SELECT l FROM Load l WHERE " +
           "(:loadNumber IS NULL OR l.loadNumber LIKE CONCAT('%', :loadNumber, '%')) AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:commodityType IS NULL OR l.commodityType = :commodityType)")
    Page<Load> searchLoadsAdvanced(@Param("loadNumber") String loadNumber,
                                   @Param("status") LoadStatus status,
                                   @Param("commodityType") String commodityType,
                                   Pageable pageable);
}
