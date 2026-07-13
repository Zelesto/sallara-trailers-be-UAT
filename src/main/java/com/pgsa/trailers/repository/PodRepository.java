// src/main/java/com/pgsa/trailers/repository/PodRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.ops.Pod;
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
public interface PodRepository extends JpaRepository<Pod, Long> {

    /**
     * Find POD by POD number
     */
    Optional<Pod> findByPodNumber(String podNumber);

    /**
     * Find PODs by trip ID
     */
    List<Pod> findByTripId(Long tripId);

    /**
     * Find PODs by trip ID with pagination
     */
    Page<Pod> findByTripId(Long tripId, Pageable pageable);

    /**
     * Find PODs by status
     */
    Page<Pod> findByStatus(String status, Pageable pageable);

    /**
     * Count PODs by status
     */
    long countByStatus(String status);

    /**
     * Count PODs by source
     */
    long countBySource(String source);

    /**
     * Search PODs by pod number, customer name, or trip number
     */
    @Query("SELECT p FROM Pod p WHERE " +
           "LOWER(p.podNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.customerName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.tripNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.driverName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Pod> searchPods(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Count PODs pending debrief
     */
    @Query("SELECT COUNT(p) FROM Pod p WHERE p.status IN ('PENDING', 'SCANNED')")
    long countPendingDebrief();

    /**
     * Count PODs scanned since a specific time
     */
    @Query("SELECT COUNT(p) FROM Pod p WHERE p.source = 'SCANNED' AND p.uploadedAt >= :since")
    long countScannedSince(@Param("since") LocalDateTime since);

    /**
     * Find PODs with null or empty file URL
     * This is the method that was missing
     */
    @Query("SELECT p FROM Pod p WHERE p.fileUrl IS NULL OR p.fileUrl = ''")
    List<Pod> findByFileUrlIsNullOrFileUrlIsEmpty();

    /**
     * Find PODs with null or empty file URL with pagination
     */
    @Query("SELECT p FROM Pod p WHERE p.fileUrl IS NULL OR p.fileUrl = ''")
    Page<Pod> findByFileUrlIsNullOrFileUrlIsEmpty(Pageable pageable);

    /**
     * Find PODs by status and file URL null or empty
     */
    @Query("SELECT p FROM Pod p WHERE p.status = :status AND (p.fileUrl IS NULL OR p.fileUrl = '')")
    List<Pod> findByStatusAndFileUrlIsNullOrEmpty(@Param("status") String status);

    /**
     * Find PODs by delivery date range
     */
    @Query("SELECT p FROM Pod p WHERE p.deliveryDate BETWEEN :startDate AND :endDate")
    List<Pod> findByDeliveryDateBetween(@Param("startDate") LocalDateTime startDate, 
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Find PODs by customer name
     */
    List<Pod> findByCustomerNameContainingIgnoreCase(String customerName);

    /**
     * Find PODs by status and trip ID
     */
    List<Pod> findByStatusAndTripId(String status, Long tripId);

    /**
     * Find PODs by source and status
     */
    List<Pod> findBySourceAndStatus(String source, String status);

    /**
     * Count PODs by status and source
     */
    long countByStatusAndSource(String status, String source);

    /**
     * Get all PODs with file URLs
     */
    @Query("SELECT p FROM Pod p WHERE p.fileUrl IS NOT NULL AND p.fileUrl != ''")
    List<Pod> findAllWithFileUrls();

    /**
     * Get all PODs without file URLs
     */
    @Query("SELECT p FROM Pod p WHERE p.fileUrl IS NULL OR p.fileUrl = ''")
    List<Pod> findAllWithoutFileUrls();

    /**
     * Find PODs by pod number containing
     */
    List<Pod> findByPodNumberContainingIgnoreCase(String podNumber);

    /**
     * Find PODs by driver name
     */
    List<Pod> findByDriverNameContainingIgnoreCase(String driverName);

    /**
     * Find PODs by notes containing
     */
    List<Pod> findByNotesContainingIgnoreCase(String notes);

    /**
     * Get POD statistics by status
     */
    @Query("SELECT p.status, COUNT(p) FROM Pod p GROUP BY p.status")
    List<Object[]> countByStatusGroup();

    /**
     * Get POD statistics by source
     */
    @Query("SELECT p.source, COUNT(p) FROM Pod p GROUP BY p.source")
    List<Object[]> countBySourceGroup();

    /**
     * Find PODs created between dates
     */
    @Query("SELECT p FROM Pod p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Pod> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Find PODs by trip number (join with trip entity)
     */
    @Query("SELECT p FROM Pod p WHERE p.tripNumber = :tripNumber")
    List<Pod> findByTripNumber(@Param("tripNumber") String tripNumber);

    /**
     * Find PODs that are not debriefed
     */
    @Query("SELECT p FROM Pod p WHERE p.debriefedAt IS NULL")
    List<Pod> findNotDebriefed();

    /**
     * Find PODs debriefed after a specific time
     */
    @Query("SELECT p FROM Pod p WHERE p.debriefedAt >= :since")
    List<Pod> findDebriefedSince(@Param("since") LocalDateTime since);

    /**
     * Count PODs by delivery date
     */
    @Query("SELECT p.deliveryDate, COUNT(p) FROM Pod p GROUP BY p.deliveryDate")
    List<Object[]> countByDeliveryDateGroup();

    /**
     * Find recent PODs (last 7 days)
     */
    @Query("SELECT p FROM Pod p WHERE p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<Pod> findRecentPods(@Param("since") LocalDateTime since, Pageable pageable);
}
