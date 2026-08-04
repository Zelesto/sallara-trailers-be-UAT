// src/main/java/com/pgsa/trailers/repository/LeaveRequestRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.attendance.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByDriverId(Long driverId);

    List<LeaveRequest> findByDriverIdAndStatus(Long driverId, String status);

    List<LeaveRequest> findByDriverIdAndStartDateBetween(Long driverId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT l FROM LeaveRequest l WHERE l.driver.id = :driverId AND l.status = 'PENDING'")
    List<LeaveRequest> findPendingByDriverId(@Param("driverId") Long driverId);

    @Query("SELECT l FROM LeaveRequest l WHERE l.startDate <= :endDate AND l.endDate >= :startDate AND l.driver.id = :driverId AND l.status = 'APPROVED'")
    List<LeaveRequest> findOverlappingApprovedLeave(@Param("driverId") Long driverId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
