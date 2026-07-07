package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.DriverDTO;
import com.pgsa.trailers.dto.DriverRequest;
import com.pgsa.trailers.enums.DriverStatus;
import com.pgsa.trailers.service.DriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    // ====== GET Endpoints ======
    
    @GetMapping
    public ResponseEntity<List<DriverDTO>> getAllDrivers() {
        log.info("GET /api/drivers");
        try {
            List<DriverDTO> drivers = driverService.getAllDrivers();
            return ResponseEntity.ok(drivers);
        } catch (Exception e) {
            log.error("Error fetching drivers: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/active")
    public ResponseEntity<List<DriverDTO>> getActiveDrivers() {
        log.info("GET /api/drivers/active");
        try {
            List<DriverDTO> drivers = driverService.getDriversByStatus(DriverStatus.ACTIVE);
            return ResponseEntity.ok(drivers);
        } catch (Exception e) {
            log.error("Error fetching active drivers: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverDTO> getDriverById(@PathVariable Long id) {
        log.info("GET /api/drivers/{}", id);
        try {
            DriverDTO driver = driverService.getDriverById(id);
            return ResponseEntity.ok(driver);
        } catch (RuntimeException e) {
            log.error("Driver not found with ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching driver by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<DriverDTO>> getDriversByStatus(@PathVariable String status) {
        log.info("GET /api/drivers/status/{}", status);
        try {
            DriverStatus driverStatus = DriverStatus.valueOf(status.toUpperCase());
            List<DriverDTO> drivers = driverService.getDriversByStatus(driverStatus);
            return ResponseEntity.ok(drivers);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}", status);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching drivers by status {}: {}", status, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ====== POST Endpoints ======
    
    @PostMapping
    public ResponseEntity<?> createDriver(@RequestBody DriverRequest request) {
        log.info("POST /api/drivers - Creating driver: {} {}", 
            request.getFirstName(), request.getLastName());
        log.info("📥 Received request - firstName: {}, lastName: {}, licenseNumber: {}, status: {}", 
            request.getFirstName(), request.getLastName(), 
            request.getLicenseNumber(), request.getStatus());
        
        try {
            DriverDTO created = driverService.createDriver(request);
            log.info("✅ Driver created successfully - ID: {}, name: {}", 
                created.getId(), created.getFirstName() + " " + created.getLastName());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            log.error("Error creating driver: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating driver: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to create driver: " + e.getMessage());
        }
    }

    // ====== PUT Endpoints ======
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDriver(@PathVariable Long id, @RequestBody DriverRequest request) {
        log.info("PUT /api/drivers/{} - Updating driver", id);
        log.info("📥 Received request - firstName: {}, lastName: {}, licenseNumber: {}, status: {}", 
            request.getFirstName(), request.getLastName(), 
            request.getLicenseNumber(), request.getStatus());
        
        try {
            DriverDTO updated = driverService.updateDriver(id, request);
            log.info("✅ Driver updated successfully - ID: {}, name: {}", 
                updated.getId(), updated.getFirstName() + " " + updated.getLastName());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("Error updating driver {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating driver {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to update driver: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/assign-vehicle/{vehicleId}")
    public ResponseEntity<?> assignVehicle(@PathVariable Long id, @PathVariable Long vehicleId) {
        log.info("PUT /api/drivers/{}/assign-vehicle/{}", id, vehicleId);
