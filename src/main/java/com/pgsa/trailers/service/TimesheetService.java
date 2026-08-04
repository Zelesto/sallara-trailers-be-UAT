package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.PunchRequestDTO;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.attendance.TimesheetEntry;
import com.pgsa.trailers.repository.DriverRepository;
import com.pgsa.trailers.repository.TimesheetEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimesheetService {

    private final TimesheetEntryRepository timesheetEntryRepository;
    private final DriverRepository driverRepository;

    @Transactional
    public TimesheetEntry punch(PunchRequestDTO request) {
        log.info("📌 Punch request - Driver: {}, Type: {}, Location: {}", 
            request.getDriverId(), request.getPunchType(), request.getLocation());

        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> {
                    log.error("❌ Driver not found with ID: {}", request.getDriverId());
                    return new RuntimeException("Driver not found");
                });

        log.info("👤 Driver: {}, Current Status: {}", driver.getFullName(), driver.getCurrentStatus());

        TimesheetEntry entry = new TimesheetEntry();
        entry.setDriver(driver);
        entry.setEntryDate(LocalDate.now());
        entry.setPunchLocation(request.getLocation());
        entry.setPunchLatitude(request.getLatitude());
        entry.setPunchLongitude(request.getLongitude());

        switch (request.getPunchType().toUpperCase()) {
            case "CLOCK_IN":
                log.info("⏰ Processing CLOCK_IN for driver: {}", driver.getFullName());
                
                // Check if already clocked in
                if (driver.isClockedIn()) {
                    log.warn("⚠️ Driver {} is already clocked in", driver.getFullName());
                    throw new RuntimeException("Driver is already clocked in");
                }

                entry.setStartTime(LocalTime.now());
                entry.setClockInTime(LocalDateTime.now());
                entry.setPunchStatus("CLOCKED_IN");
                entry.setActivityType("DRIVING");
                entry.setStatus("ACTIVE");
                entry.setIsActive(true);

                // Use the driver's clockIn method
                driver.clockIn();
                
                log.info("✅ Driver {} clocked in at {}", driver.getFullName(), driver.getLastClockIn());
                break;

            case "BREAK_START":
                log.info("☕ Processing BREAK_START for driver: {}", driver.getFullName());
                
                TimesheetEntry activeEntry = findActiveEntry(driver.getId());
                if (activeEntry == null) {
                    log.error("❌ No active clock-in found for driver: {}", driver.getFullName());
                    throw new RuntimeException("No active clock-in found");
                }

                // Check if already on break
                if (driver.isOnBreak()) {
                    log.warn("⚠️ Driver {} is already on break", driver.getFullName());
                    throw new RuntimeException("Driver is already on break");
                }

                activeEntry.setBreakStartTime(LocalDateTime.now());
                activeEntry.setPunchStatus("ON_BREAK");
                
                // Use the driver's startBreak method
                driver.startBreak();
                
                log.info("✅ Driver {} started break at {}", driver.getFullName(), activeEntry.getBreakStartTime());
                return timesheetEntryRepository.save(activeEntry);

            case "BREAK_END":
                log.info("🔄 Processing BREAK_END for driver: {}", driver.getFullName());
                
                TimesheetEntry breakEntry = findBreakEntry(driver.getId());
                if (breakEntry == null) {
                    log.error("❌ No active break found for driver: {}", driver.getFullName());
                    throw new RuntimeException("No active break found");
                }

                breakEntry.setBreakEndTime(LocalDateTime.now());
                breakEntry.setPunchStatus("CLOCKED_IN");
                
                long breakDuration = ChronoUnit.MINUTES.between(breakEntry.getBreakStartTime(), LocalDateTime.now());
                breakEntry.setBreakDuration(breakEntry.getBreakDuration() + (int) breakDuration);
                
                // Use the driver's endBreak method
                driver.endBreak();
                
                log.info("✅ Driver {} ended break after {} minutes", driver.getFullName(), breakDuration);
                return timesheetEntryRepository.save(breakEntry);

            case "CLOCK_OUT":
                log.info("🏁 Processing CLOCK_OUT for driver: {}", driver.getFullName());
                
                TimesheetEntry clockOutEntry = findActiveEntry(driver.getId());
                if (clockOutEntry == null) {
                    log.error("❌ No active clock-in found for driver: {}", driver.getFullName());
                    throw new RuntimeException("No active clock-in found");
                }

                clockOutEntry.setEndTime(LocalTime.now());
                clockOutEntry.setClockOutTime(LocalDateTime.now());
                clockOutEntry.setPunchStatus("CLOCKED_OUT");
                
                if (clockOutEntry.getClockInTime() != null) {
                    long hours = ChronoUnit.HOURS.between(clockOutEntry.getClockInTime(), LocalDateTime.now());
                    clockOutEntry.setTotalHours(BigDecimal.valueOf(hours));
                    log.info("📊 Total hours worked: {}", hours);
                }
                
                // Use the driver's clockOut method
                driver.clockOut();
                
                log.info("✅ Driver {} clocked out at {}", driver.getFullName(), driver.getLastClockOut());
                return timesheetEntryRepository.save(clockOutEntry);

            default:
                log.error("❌ Invalid punch type: {}", request.getPunchType());
                throw new RuntimeException("Invalid punch type: " + request.getPunchType());
        }

        log.info("💾 Saving driver state");
        driverRepository.save(driver);
        
        log.info("💾 Saving timesheet entry");
        return timesheetEntryRepository.save(entry);
    }

    public TimesheetEntry findActiveEntry(Long driverId) {
        log.debug("🔍 Finding active entry for driver: {}", driverId);
        List<TimesheetEntry> entries = timesheetEntryRepository.findActivePunchEntries(driverId);
        if (entries.isEmpty()) {
            log.debug("No active entries found");
            return null;
        }
        return entries.get(0);
    }

    public TimesheetEntry findBreakEntry(Long driverId) {
        log.debug("🔍 Finding break entry for driver: {}", driverId);
        List<TimesheetEntry> entries = timesheetEntryRepository.findByDriverIdAndPunchStatus(driverId, "ON_BREAK");
        if (entries.isEmpty()) {
            log.debug("No break entries found");
            return null;
        }
        return entries.get(0);
    }

    public List<TimesheetEntry> getEntriesByDriver(Long driverId, LocalDate startDate, LocalDate endDate) {
        log.info("📋 Fetching timesheet entries for driver: {} from {} to {}", driverId, startDate, endDate);
        return timesheetEntryRepository.findByDriverIdAndEntryDateBetween(driverId, startDate, endDate);
    }

    public List<TimesheetEntry> getEntriesByDateRange(Long driverId, LocalDateTime start, LocalDateTime end) {
        log.info("📋 Fetching timesheet entries for driver: {} between {} and {}", driverId, start, end);
        return timesheetEntryRepository.findByClockInTimeBetween(driverId, start, end);
    }

    public BigDecimal getTotalHours(Long driverId, LocalDate startDate, LocalDate endDate) {
        log.info("📊 Calculating total hours for driver: {} from {} to {}", driverId, startDate, endDate);
        BigDecimal hours = timesheetEntryRepository.sumTotalHoursByDriverAndDateRange(driverId, startDate, endDate);
        return hours != null ? hours : BigDecimal.ZERO;
    }

    /**
     * Get current status for a driver
     */
    public String getDriverStatus(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        return driver.getCurrentStatus();
    }

    /**
     * Check if driver is clocked in
     */
    public boolean isDriverClockedIn(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        return driver.isClockedIn();
    }
}
