package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.VehicleDTO;
import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.enums.VehicleStatus;
import com.pgsa.trailers.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    // ====== GET Endpoints ======
    
    @GetMapping
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        log.info("GET /api/vehicles");
        try {
            List<Vehicle> vehicles = vehicleService.getAllVehicles();
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error fetching vehicles: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/active")
    public ResponseEntity<List<Vehicle>> getActiveVehicles() {
        log.info("GET /api/vehicles/active");
        try {
            List<Vehicle> vehicles = vehicleService.getAllActiveVehicles();
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error fetching active vehicles: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getVehicleById(@PathVariable Long id) {
        log.info("GET /api/vehicles/{}", id);
        try {
            Vehicle vehicle = vehicleService.getVehicleById(id);
            return ResponseEntity.ok(vehicle);
        } catch (RuntimeException e) {
            log.error("Vehicle not found with ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching vehicle by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/registration/{registrationNumber}")
    public ResponseEntity<Vehicle> getVehicleByRegistration(@PathVariable String registrationNumber) {
        log.info("GET /api/vehicles/registration/{}", registrationNumber);
        try {
            Vehicle vehicle = vehicleService.getVehicleByRegistration(registrationNumber);
            return ResponseEntity.ok(vehicle);
        } catch (RuntimeException e) {
            log.error("Vehicle not found with registration {}: {}", registrationNumber, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching vehicle by registration {}: {}", registrationNumber, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Vehicle>> getVehiclesByStatus(@PathVariable String status) {
        log.info("GET /api/vehicles/status/{}", status);
        try {
            VehicleStatus vehicleStatus = VehicleStatus.valueOf(status.toUpperCase());
            List<Vehicle> vehicles = vehicleService.getVehiclesByStatus(vehicleStatus);
            return ResponseEntity.ok(vehicles);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}", status);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching vehicles by status {}: {}", status, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/available")
    public ResponseEntity<List<Vehicle>> getAvailableVehicles() {
        log.info("GET /api/vehicles/available");
        try {
            List<Vehicle> vehicles = vehicleService.getAvailableVehicles();
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error fetching available vehicles: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Vehicle>> searchVehicles(@RequestParam String term) {
        log.info("GET /api/vehicles/search?term={}", term);
        try {
            List<Vehicle> vehicles = vehicleService.searchVehicles(term);
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error searching vehicles: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<Vehicle>> getVehiclesByDriver(@PathVariable Long driverId) {
        log.info("GET /api/vehicles/driver/{}", driverId);
        try {
            List<Vehicle> vehicles = vehicleService.getVehiclesByDriver(driverId);
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error fetching vehicles for driver {}: {}", driverId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/overdue-service")
    public ResponseEntity<List<Vehicle>> getVehiclesOverdueForService() {
        log.info("GET /api/vehicles/overdue-service");
        try {
            List<Vehicle> vehicles = vehicleService.getVehiclesOverdueForService();
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error fetching vehicles overdue for service: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/upcoming-service")
    public ResponseEntity<List<Vehicle>> getVehiclesWithUpcomingService() {
        log.info("GET /api/vehicles/upcoming-service");
        try {
            List<Vehicle> vehicles = vehicleService.getVehiclesWithUpcomingService();
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error fetching vehicles with upcoming service: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/expired-insurance")
    public ResponseEntity<List<Vehicle>> getVehiclesWithExpiredInsurance() {
        log.info("GET /api/vehicles/expired-insurance");
        try {
            List<Vehicle> vehicles = vehicleService.getVehiclesWithExpiredInsurance();
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error fetching vehicles with expired insurance: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/expired-roadworthy")
    public ResponseEntity<List<Vehicle>> getVehiclesWithExpiredRoadworthy() {
        log.info("GET /api/vehicles/expired-roadworthy");
        try {
            List<Vehicle> vehicles = vehicleService.getVehiclesWithExpiredRoadworthy();
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error fetching vehicles with expired roadworthy: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ====== POST Endpoints ======
    
    @PostMapping
    public ResponseEntity<?> createVehicle(@RequestBody VehicleDTO vehicleDTO) {
        log.info("POST /api/vehicles - Creating vehicle: {}", vehicleDTO.getRegistrationNumber());
        try {
            // Validate required fields
            if (vehicleDTO.getRegistrationNumber() == null || vehicleDTO.getRegistrationNumber().isEmpty()) {
                return ResponseEntity.badRequest().body("Registration number is required");
            }
            
            Vehicle createdVehicle = vehicleService.createVehicleFromDTO(vehicleDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdVehicle);
        } catch (RuntimeException e) {
            log.error("Error creating vehicle: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating vehicle: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to create vehicle: " + e.getMessage());
        }
    }

    // ====== PUT Endpoints ======
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateVehicle(@PathVariable Long id, @RequestBody VehicleDTO vehicleDTO) {
        log.info("PUT /api/vehicles/{} - Updating vehicle", id);
        log.info("Received DTO: registrationNumber={}, make={}, model={}, vehicleType={}, status={}", 
            vehicleDTO.getRegistrationNumber(),
            vehicleDTO.getMake(),
            vehicleDTO.getModel(),
            vehicleDTO.getVehicleType(),
            vehicleDTO.getStatus());
        
        try {
            Vehicle updatedVehicle = vehicleService.updateVehicle(id, vehicleDTO);
            log.info("✅ Successfully updated vehicle: {}", updatedVehicle);
            return ResponseEntity.ok(updatedVehicle);
        } catch (RuntimeException e) {
            log.error("Error updating vehicle {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("❌ Error updating vehicle {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to update vehicle: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/assign-driver/{driverId}")
    public ResponseEntity<?> assignDriver(@PathVariable Long id, @PathVariable Long driverId) {
        log.info("PUT /api/vehicles/{}/assign-driver/{}", id, driverId);
        try {
            vehicleService.assignDriverToVehicle(id, driverId);
            return ResponseEntity.ok().body("Driver assigned successfully");
        } catch (RuntimeException e) {
            log.error("Error assigning driver: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error assigning driver: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to assign driver: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/unassign-driver")
    public ResponseEntity<?> unassignDriver(@PathVariable Long id) {
        log.info("PUT /api/vehicles/{}/unassign-driver", id);
        try {
            vehicleService.unassignDriverFromVehicle(id);
            return ResponseEntity.ok().body("Driver unassigned successfully");
        } catch (RuntimeException e) {
            log.error("Error unassigning driver: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error unassigning driver: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to unassign driver: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/odometer")
    public ResponseEntity<?> updateOdometer(@PathVariable Long id, @RequestParam BigDecimal odometer) {
        log.info("PUT /api/vehicles/{}/odometer?odometer={}", id, odometer);
        try {
            vehicleService.updateOdometer(id, odometer);
            return ResponseEntity.ok().body("Odometer updated successfully");
        } catch (RuntimeException e) {
            log.error("Error updating odometer: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating odometer: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to update odometer: " + e.getMessage());
        }
    }

    // ====== DELETE Endpoints ======
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id) {
        log.info("DELETE /api/vehicles/{} - Deleting vehicle", id);
        try {
            vehicleService.deleteVehicle(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Vehicle not found with ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting vehicle {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to delete vehicle: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/soft")
    public ResponseEntity<?> softDeleteVehicle(@PathVariable Long id) {
        log.info("DELETE /api/vehicles/{}/soft - Soft deleting vehicle", id);
        try {
            vehicleService.softDeleteVehicle(id);
            return ResponseEntity.ok().body("Vehicle soft deleted successfully");
        } catch (RuntimeException e) {
            log.error("Vehicle not found with ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error soft deleting vehicle {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to soft delete vehicle: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<?> restoreVehicle(@PathVariable Long id) {
        log.info("PUT /api/vehicles/{}/restore - Restoring vehicle", id);
        try {
            vehicleService.restoreVehicle(id);
            return ResponseEntity.ok().body("Vehicle restored successfully");
        } catch (RuntimeException e) {
            log.error("Vehicle not found with ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error restoring vehicle {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to restore vehicle: " + e.getMessage());
        }
    }
}
