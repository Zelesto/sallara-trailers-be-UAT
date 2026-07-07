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
            List<Vehicle
