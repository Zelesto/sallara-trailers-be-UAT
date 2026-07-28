// src/main/java/com/pgsa/trailers/controller/inventory/VehicleIssueController.java
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

    @PostMapping
    public ResponseEntity<VehicleIssueResponseDTO> issueItemsToVehicle(
            @RequestBody @Valid VehicleIssueRequestDTO request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        VehicleIssueResponseDTO response = vehicleIssueService.issueItemsToVehicle(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{issueId}/return")
    public ResponseEntity<VehicleIssueResponseDTO> returnItemsFromVehicle(
            @PathVariable Long issueId,
            @RequestBody @Valid List<ReturnItemRequestDTO> returns,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        VehicleIssueResponseDTO response = vehicleIssueService.returnItemsFromVehicle(issueId, returns, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<VehicleIssueResponseDTO>> getIssuesByVehicle(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(vehicleIssueService.getIssuesByVehicle(vehicleId));
    }

    @GetMapping("/{issueId}")
    public ResponseEntity<VehicleIssueResponseDTO> getIssueById(@PathVariable Long issueId) {
        return ResponseEntity.ok(vehicleIssueService.getIssueById(issueId));
    }

    private Long getUserId(Authentication authentication) {
        String email = authentication.getName();
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return user.getId();
    }
}
