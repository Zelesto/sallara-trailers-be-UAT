package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.VehicleDTO;
import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

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

    @PostMapping
    public ResponseEntity<?> createVehicle(@RequestBody Vehicle vehicle) {
        log.info("POST /api/vehicles - Creating vehicle: {}", vehicle.getRegistrationNumber());
        try {
            // Validate required fields
            if (vehicle.getRegistrationNumber() == null || vehicle.getRegistrationNumber().isEmpty()) {
                return ResponseEntity.badRequest().body("Registration number is required");
            }
            
            Vehicle createdVehicle = vehicleService.createVehicle(vehicle);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdVehicle);
        } catch (RuntimeException e) {
            log.error("Error creating vehicle: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating vehicle: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to create vehicle: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVehicle(@PathVariable Long id, @RequestBody VehicleDTO vehicleDTO) {
        log.info("PUT /api/vehicles/{} - Updating vehicle", id);
        log.info("Received DTO: registrationNumber={}, make={}, model={}, vehicleType={}, fuelType={}", 
            vehicleDTO.getRegistrationNumber(),
            vehicleDTO.getMake(),
            vehicleDTO.getModel(),
            vehicleDTO.getVehicleType(),
            vehicleDTO.getFuelType());
        
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

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Vehicle>> getVehiclesByStatus(@PathVariable String status) {
        log.info("GET /api/vehicles/status/{}", status);
        try {
            List<Vehicle> vehicles = vehicleService.getVehiclesByStatus(status.toUpperCase());
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error fetching vehicles by status {}: {}", status, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/active")
    public ResponseEntity<List<Vehicle>> getActiveVehicles() {
        log.info("GET /api/vehicles/active");
        try {
            List<Vehicle> vehicles = vehicleService.getActiveVehicles();
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error fetching active vehicles: {}", e.getMessage(), e);
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
}
