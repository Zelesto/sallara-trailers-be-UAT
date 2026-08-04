// src/main/java/com/pgsa/trailers/repository/LeaveBalanceRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.attendance.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    List<LeaveBalance> findByDriverId(Long driverId);

    Optional<LeaveBalance> findByDriverIdAndLeaveTypeIdAndYear(Long driverId, Long leaveTypeId, Integer year);

    @Query("SELECT lb FROM LeaveBalance lb WHERE lb.driver.id = :driverId AND lb.year = :year")
    List<LeaveBalance> findByDriverIdAndYear(@Param("driverId") Long driverId, @Param("year") Integer year);

    @Query("SELECT lb FROM LeaveBalance lb WHERE lb.driver.id = :driverId AND lb.remainingDays < 5")
    List<LeaveBalance> findLowBalanceDrivers(@Param("driverId") Long driverId);
}
