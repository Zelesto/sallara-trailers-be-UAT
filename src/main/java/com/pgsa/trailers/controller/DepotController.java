// src/main/java/com/pgsa/trailers/controller/DepotController.java
package com.pgsa.trailers.controller;

import com.pgsa.trailers.entity.ops.Depot;
import com.pgsa.trailers.repository.DepotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/depots")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class DepotController {

    private final DepotRepository depotRepository;

    /**
     * Get current logged in username
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.warn("Could not get current username: {}", e.getMessage());
        }
        return "System";
    }

    /**
     * Get all depots
     */
    @GetMapping
    public ResponseEntity<List<Depot>> getAllDepots() {
        log.info("📦 Getting all depots");
        try {
            List<Depot> depots = depotRepository.findAllByOrderByNameAsc();
            log.info("✅ Found {} depots", depots.size());
            return ResponseEntity.ok(depots);
        } catch (Exception e) {
            log.error("❌ Error fetching depots: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get active depots only
     */
    @GetMapping("/active")
    public ResponseEntity<List<Depot>> getActiveDepots() {
        log.info("📦 Getting active depots");
        try {
            List<Depot> depots = depotRepository.findByIsActiveTrueOrderByNameAsc();
            log.info("✅ Found {} active depots", depots.size());
            return ResponseEntity.ok(depots);
        } catch (Exception e) {
            log.error("❌ Error fetching active depots: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get depot by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Depot> getDepotById(@PathVariable Long id) {
        log.info("📦 Getting depot by ID: {}", id);
        try {
            Optional<Depot> depot = depotRepository.findById(id);
            if (depot.isPresent()) {
                return ResponseEntity.ok(depot.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("❌ Error fetching depot {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get depot by code
     */
    @GetMapping("/code/{depotCode}")
    public ResponseEntity<Depot> getDepotByCode(@PathVariable String depotCode) {
        log.info("📦 Getting depot by code: {}", depotCode);
        try {
            Optional<Depot> depot = depotRepository.findByDepotCode(depotCode);
            if (depot.isPresent()) {
                return ResponseEntity.ok(depot.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("❌ Error fetching depot by code {}: {}", depotCode, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new depot
     */
    @PostMapping
    public ResponseEntity<?> createDepot(@RequestBody Depot depot) {
        log.info("📦 Creating new depot: {}", depot.getName());
        try {
            String currentUser = getCurrentUsername();
            
            // Check if depot code already exists
            if (depotRepository.existsByDepotCode(depot.getDepotCode())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Depot code already exists: " + depot.getDepotCode()));
            }

            depot.setCreatedBy(currentUser);
            depot.setCreatedAt(LocalDateTime.now());
            depot.setIsActive(depot.getIsActive() != null ? depot.getIsActive() : true);
            
            Depot saved = depotRepository.save(depot);
            log.info("✅ Created depot: {} with ID: {}", saved.getName(), saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("❌ Error creating depot: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create depot: " + e.getMessage()));
        }
    }

    /**
     * Update an existing depot
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDepot(@PathVariable Long id, @RequestBody Depot depot) {
        log.info("📦 Updating depot ID: {}", id);
        try {
            String currentUser = getCurrentUsername();
            
            Optional<Depot> existingOpt = depotRepository.findById(id);
            if (existingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Depot existing = existingOpt.get();
            
            // Update fields
            existing.setDepotCode(depot.getDepotCode());
            existing.setName(depot.getName());
            existing.setStreetAddress(depot.getStreetAddress());
            existing.setCity(depot.getCity());
            existing.setZipCode(depot.getZipCode());
            existing.setProvince(depot.getProvince());
            existing.setLatitude(depot.getLatitude());
            existing.setLongitude(depot.getLongitude());
            existing.setIsActive(depot.getIsActive());
            existing.setUpdatedBy(currentUser);
            existing.setUpdatedAt(LocalDateTime.now());
            
            Depot updated = depotRepository.save(existing);
            log.info("✅ Updated depot: {} with ID: {}", updated.getName(), updated.getId());
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("❌ Error updating depot {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update depot: " + e.getMessage()));
        }
    }

    /**
     * Toggle depot active status
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleDepotStatus(@PathVariable Long id) {
        log.info("📦 Toggling depot status for ID: {}", id);
        try {
            String currentUser = getCurrentUsername();
            
            Optional<Depot> existingOpt = depotRepository.findById(id);
            if (existingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Depot existing = existingOpt.get();
            existing.setIsActive(!existing.getIsActive());
            existing.setUpdatedBy(currentUser);
            existing.setUpdatedAt(LocalDateTime.now());
            
            Depot updated = depotRepository.save(existing);
            log.info("✅ Depot {} status toggled to: {}", updated.getName(), updated.getIsActive());
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("❌ Error toggling depot status {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to toggle depot status: " + e.getMessage()));
        }
    }

    /**
     * Delete a depot
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDepot(@PathVariable Long id) {
        log.info("🗑️ Deleting depot ID: {}", id);
        try {
            if (!depotRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            
            depotRepository.deleteById(id);
            log.info("✅ Deleted depot ID: {}", id);
            return ResponseEntity.ok(Map.of("message", "Depot deleted successfully"));
        } catch (Exception e) {
            log.error("❌ Error deleting depot {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete depot: " + e.getMessage()));
        }
    }
}
