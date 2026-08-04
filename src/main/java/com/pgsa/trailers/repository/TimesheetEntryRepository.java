// src/main/java/com/pgsa/trailers/repository/TimesheetEntryRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.attendance.TimesheetEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import java.math.BigDecimal;

@Repository
public interface TimesheetEntryRepository extends JpaRepository<TimesheetEntry, Long> {

    List<TimesheetEntry> findByDriverIdAndEntryDateBetween(Long driverId, LocalDate startDate, LocalDate endDate);

    List<TimesheetEntry> findByDriverIdAndPunchStatus(Long driverId, String punchStatus);

    List<TimesheetEntry> findByDriverIdAndEntryDate(Long driverId, LocalDate entryDate);

    @Query("SELECT t FROM TimesheetEntry t WHERE t.driver.id = :driverId AND t.punchStatus IN ('CLOCKED_IN', 'ON_BREAK') ORDER BY t.clockInTime DESC")
    List<TimesheetEntry> findActivePunchEntries(@Param("driverId") Long driverId);

    @Query("SELECT t FROM TimesheetEntry t WHERE t.driver.id = :driverId AND t.clockInTime BETWEEN :start AND :end")
    List<TimesheetEntry> findByClockInTimeBetween(@Param("driverId") Long driverId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT SUM(t.totalHours) FROM TimesheetEntry t WHERE t.driver.id = :driverId AND t.entryDate BETWEEN :startDate AND :endDate AND t.status = 'APPROVED'")
    BigDecimal sumTotalHoursByDriverAndDateRange(@Param("driverId") Long driverId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
