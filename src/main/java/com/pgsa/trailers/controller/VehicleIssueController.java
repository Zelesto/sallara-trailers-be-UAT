package com.pgsa.trailers.controller.inventory;

import com.pgsa.trailers.dto.VehicleIssueRequestDTO;
import com.pgsa.trailers.dto.VehicleIssueResponseDTO;
import com.pgsa.trailers.dto.ReturnItemRequestDTO;
import com.pgsa.trailers.entity.security.AppUser;
import com.pgsa.trailers.repository.AppUserRepository;
import com.pgsa.trailers.service.inventory.VehicleIssueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/vehicle-issues")
@RequiredArgsConstructor
@Slf4j
public class VehicleIssueController {

    private final VehicleIssueService vehicleIssueService;
    private final AppUserRepository appUserRepository;

    // ✅ GET all vehicle issues
    @GetMapping
public ResponseEntity<List<VehicleIssueResponseDTO>> getAllVehicleIssues() {
    log.info("📋 Fetching all vehicle issues");
    try {
        List<VehicleIssueResponseDTO> issues = vehicleIssueService.getAllVehicleIssues();
        log.info("📋 Returning {} vehicle issues", issues.size());
        return ResponseEntity.ok(issues);
    } catch (Exception e) {
        log.error("❌ Error fetching vehicle issues: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}


    @GetMapping("/debug/all")
public ResponseEntity<List<VehicleIssue>> debugGetAllIssues() {
    log.info("🐛 Debug: Getting all vehicle issues from repository");
    try {
        List<VehicleIssue> issues = vehicleIssueRepository.findAll();
        log.info("🐛 Found {} issues", issues.size());
        return ResponseEntity.ok(issues);
    } catch (Exception e) {
        log.error("❌ Debug error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
    
    // ✅ POST - Issue items to vehicle
    @PostMapping
    public ResponseEntity<VehicleIssueResponseDTO> issueItemsToVehicle(
            @RequestBody @Valid VehicleIssueRequestDTO request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        log.info("🚗 Creating vehicle issue for vehicle: {}", request.getVehicleId());
        VehicleIssueResponseDTO response = vehicleIssueService.issueItemsToVehicle(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ✅ POST - Return items from vehicle
    @PostMapping("/{issueId}/return")
    public ResponseEntity<VehicleIssueResponseDTO> returnItemsFromVehicle(
            @PathVariable Long issueId,
            @RequestBody @Valid List<ReturnItemRequestDTO> returns,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        log.info("🔄 Returning items from issue: {}", issueId);
        VehicleIssueResponseDTO response = vehicleIssueService.returnItemsFromVehicle(issueId, returns, userId);
        return ResponseEntity.ok(response);
    }

    // ✅ GET - Get issues by vehicle ID
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<VehicleIssueResponseDTO>> getIssuesByVehicle(@PathVariable Long vehicleId) {
        log.info("🚗 Fetching issues for vehicle: {}", vehicleId);
        return ResponseEntity.ok(vehicleIssueService.getIssuesByVehicle(vehicleId));
    }

    // ✅ GET - Get issues by driver ID
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<VehicleIssueResponseDTO>> getIssuesByDriver(@PathVariable Long driverId) {
        log.info("👤 Fetching issues for driver: {}", driverId);
        return ResponseEntity.ok(vehicleIssueService.getIssuesByDriver(driverId));
    }

    // ✅ GET - Get issue by ID
    @GetMapping("/{issueId}")
    public ResponseEntity<VehicleIssueResponseDTO> getIssueById(@PathVariable Long issueId) {
        log.info("📋 Fetching vehicle issue: {}", issueId);
        return ResponseEntity.ok(vehicleIssueService.getIssueById(issueId));
    }

    private Long getUserId(Authentication authentication) {
        String email = authentication.getName();
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return user.getId();
    }
}
