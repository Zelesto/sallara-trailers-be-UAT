// src/main/java/com/pgsa/trailers/repository/PodRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.ops.Pod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PodRepository extends JpaRepository<Pod, Long> {

    Optional<Pod> findByPodNumber(String podNumber);

    List<Pod> findByTripId(Long tripId);

    Page<Pod> findByTripId(Long tripId, Pageable pageable);

    Page<Pod> findByStatus(String status, Pageable pageable);

    Page<Pod> findByCustomerNameContainingIgnoreCase(String customerName, Pageable pageable);

    // Count methods
    long countByStatus(String status);
    
    long countBySource(String source);
    
    long countByTripId(Long tripId);
    
    @Query("SELECT COUNT(p) FROM Pod p WHERE p.status IN ('PENDING', 'SCANNED')")
    long countPendingDebrief();
    
    @Query("SELECT COUNT(p) FROM Pod p WHERE p.source = 'SCANNED' AND p.uploadedAt >= :since")
    long countScannedSince(@Param("since") LocalDateTime since);

    // Search methods
    @Query("SELECT p FROM Pod p WHERE " +
           "LOWER(p.podNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.driverName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.status) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Pod> searchPods(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Pod p WHERE p.deliveryDate BETWEEN :startDate AND :endDate")
    List<Pod> findPodsByDateRange(@Param("startDate") LocalDate startDate, 
                                   @Param("endDate") LocalDate endDate);

    // Find by status and source
    Page<Pod> findByStatusAndSource(String status, String source, Pageable pageable);
    
    List<Pod> findByStatusAndSource(String status, String source);
    
    // Find by source
    Page<Pod> findBySource(String source, Pageable pageable);
    
    List<Pod> findBySource(String source);

    // Existence checks
    boolean existsByTripId(Long tripId);
    
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Pod p WHERE p.tripId = :tripId AND p.status = :status")
    boolean existsByTripIdAndStatus(@Param("tripId") Long tripId, @Param("status") String status);

    // Delete methods
    void deleteByTripId(Long tripId);
    
    @Query("DELETE FROM Pod p WHERE p.tripId = :tripId")
    void deleteAllByTripId(@Param("tripId") Long tripId);

    // Statistics query - get POD counts by status
    @Query("SELECT p.status, COUNT(p) FROM Pod p GROUP BY p.status")
    List<Object[]> countPodsByStatus();

    // Get recent PODs
    @Query("SELECT p FROM Pod p ORDER BY p.createdAt DESC")
    Page<Pod> findRecentPods(Pageable pageable);
    
    // Get PODs with file
    @Query("SELECT p FROM Pod p WHERE p.fileUrl IS NOT NULL")
    Page<Pod> findPodsWithFile(Pageable pageable);
    
    // Get PODs without file
    @Query("SELECT p FROM Pod p WHERE p.fileUrl IS NULL")
    Page<Pod> findPodsWithoutFile(Pageable pageable);

    // Get PODs by trip number (if trip number is stored or via join)
    @Query("SELECT p FROM Pod p JOIN Trip t ON p.tripId = t.id WHERE t.tripNumber LIKE CONCAT('%', :tripNumber, '%')")
    Page<Pod> findByTripNumberContaining(@Param("tripNumber") String tripNumber, Pageable pageable);

    // Get PODs uploaded by a specific user
    Page<Pod> findByUploadedBy(String uploadedBy, Pageable pageable);

    // Get PODs with status in a list
    Page<Pod> findByStatusIn(List<String> statuses, Pageable pageable);
}
