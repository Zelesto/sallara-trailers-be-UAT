// src/main/java/com/pgsa/trailers/controller/TimesheetController.java
package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.PunchRequestDTO;
import com.pgsa.trailers.dto.TimesheetEntryDTO;
import com.pgsa.trailers.entity.attendance.TimesheetEntry;
import com.pgsa.trailers.service.TimesheetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/timesheet")
@RequiredArgsConstructor
public class TimesheetController {

    private final TimesheetService timesheetService;

    @PostMapping("/punch")
    public ResponseEntity<?> punch(@RequestBody PunchRequestDTO request) {
        log.info("POST /api/timesheet/punch - Driver: {}, Type: {}", request.getDriverId(), request.getPunchType());
        try {
            TimesheetEntry entry = timesheetService.punch(request);
            return ResponseEntity.ok(entry);
        } catch (RuntimeException e) {
            log.error("Error processing punch: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error processing punch: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to process punch");
        }
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<TimesheetEntry>> getEntriesByDriver(
            @PathVariable Long driverId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/timesheet/driver/{}", driverId);
        try {
            List<TimesheetEntry> entries = timesheetService.getEntriesByDriver(driverId, startDate, endDate);
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            log.error("Error fetching timesheet entries: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/driver/{driverId}/hours")
    public ResponseEntity<BigDecimal> getTotalHours(
            @PathVariable Long driverId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/timesheet/driver/{}/hours", driverId);
        try {
            BigDecimal hours = timesheetService.getTotalHours(driverId, startDate, endDate);
            return ResponseEntity.ok(hours);
        } catch (Exception e) {
            log.error("Error fetching total hours: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/driver/{driverId}/active")
    public ResponseEntity<TimesheetEntry> getActiveEntry(@PathVariable Long driverId) {
        log.info("GET /api/timesheet/driver/{}/active", driverId);
        try {
            TimesheetEntry entry = timesheetService.findActiveEntry(driverId);
            return ResponseEntity.ok(entry);
        } catch (Exception e) {
            log.error("Error fetching active entry: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
