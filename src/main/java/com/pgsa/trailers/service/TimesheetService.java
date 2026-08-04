// src/main/java/com/pgsa/trailers/service/TimesheetService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.TimesheetEntryDTO;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimesheetService {

    private final TimesheetEntryRepository timesheetEntryRepository;
    private final DriverRepository driverRepository;

    @Transactional
    public TimesheetEntry punch(PunchRequestDTO request) {
        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        TimesheetEntry entry = new TimesheetEntry();
        entry.setDriver(driver);
        entry.setEntryDate(LocalDate.now());
        entry.setPunchLocation(request.getLocation());
        entry.setPunchLatitude(request.getLatitude());
        entry.setPunchLongitude(request.getLongitude());

        switch (request.getPunchType().toUpperCase()) {
            case "CLOCK_IN":
                entry.setStartTime(LocalTime.now());
                entry.setClockInTime(LocalDateTime.now());
                entry.setPunchStatus("CLOCKED_IN");
                entry.setActivityType("DRIVING");
                entry.setStatus("ACTIVE");
                driver.setCurrentStatus("CLOCKED_IN");
                driver.setLastClockIn(LocalDateTime.now());
                break;

            case "BREAK_START":
                TimesheetEntry activeEntry = findActiveEntry(driver.getId());
                if (activeEntry == null) {
                    throw new RuntimeException("No active clock-in found");
                }
                activeEntry.setBreakStartTime(LocalDateTime.now());
                activeEntry.setPunchStatus("ON_BREAK");
                driver.setCurrentStatus("ON_BREAK");
                return timesheetEntryRepository.save(activeEntry);

            case "BREAK_END":
                TimesheetEntry breakEntry = findBreakEntry(driver.getId());
                if (breakEntry == null) {
                    throw new RuntimeException("No active break found");
                }
                breakEntry.setBreakEndTime(LocalDateTime.now());
                breakEntry.setPunchStatus("CLOCKED_IN");
                long breakDuration = ChronoUnit.MINUTES.between(breakEntry.getBreakStartTime(), LocalDateTime.now());
                breakEntry.setBreakDuration(breakEntry.getBreakDuration() + (int) breakDuration);
                driver.setCurrentStatus("CLOCKED_IN");
                return timesheetEntryRepository.save(breakEntry);

            case "CLOCK_OUT":
                TimesheetEntry clockOutEntry = findActiveEntry(driver.getId());
                if (clockOutEntry == null) {
                    throw new RuntimeException("No active clock-in found");
                }
                clockOutEntry.setEndTime(LocalTime.now());
                clockOutEntry.setClockOutTime(LocalDateTime.now());
                clockOutEntry.setPunchStatus("CLOCKED_OUT");
                // Calculate total hours
                if (clockOutEntry.getClockInTime() != null) {
                    long hours = ChronoUnit.HOURS.between(clockOutEntry.getClockInTime(), LocalDateTime.now());
                    clockOutEntry.setTotalHours(BigDecimal.valueOf(hours));
                }
                driver.setCurrentStatus("OFF_DUTY");
                driver.setLastClockOut(LocalDateTime.now());
                return timesheetEntryRepository.save(clockOutEntry);

            default:
                throw new RuntimeException("Invalid punch type: " + request.getPunchType());
        }

        driverRepository.save(driver);
        return timesheetEntryRepository.save(entry);
    }

    public TimesheetEntry findActiveEntry(Long driverId) {
        List<TimesheetEntry> entries = timesheetEntryRepository.findActivePunchEntries(driverId);
        return entries.isEmpty() ? null : entries.get(0);
    }

    public TimesheetEntry findBreakEntry(Long driverId) {
        List<TimesheetEntry> entries = timesheetEntryRepository.findByDriverIdAndPunchStatus(driverId, "ON_BREAK");
        return entries.isEmpty() ? null : entries.get(0);
    }

    public List<TimesheetEntry> getEntriesByDriver(Long driverId, LocalDate startDate, LocalDate endDate) {
        return timesheetEntryRepository.findByDriverIdAndEntryDateBetween(driverId, startDate, endDate);
    }

    public List<TimesheetEntry> getEntriesByDateRange(Long driverId, LocalDateTime start, LocalDateTime end) {
        return timesheetEntryRepository.findByClockInTimeBetween(driverId, start, end);
    }

    public BigDecimal getTotalHours(Long driverId, LocalDate startDate, LocalDate endDate) {
        return timesheetEntryRepository.sumTotalHoursByDriverAndDateRange(driverId, startDate, endDate);
    }
}
