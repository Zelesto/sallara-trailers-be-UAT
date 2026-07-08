// src/main/java/com/pgsa/trailers/controller/EnumManagementController.java
package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.CreateCustomEnumRequest;
import com.pgsa.trailers.dto.CustomEnumDto;
import com.pgsa.trailers.service.EnumManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enums")
@RequiredArgsConstructor
public class EnumManagementController {

    private final EnumManagementService enumManagementService;

    /**
     * Get all enums for a specific type
     */
    @GetMapping("/{enumType}")
    public ResponseEntity<List<CustomEnumDto>> getEnumsByType(
            @PathVariable String enumType,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        Long effectiveTenantId = tenantId != null ? tenantId : 1L;
        List<CustomEnumDto> enums = enumManagementService.getEnumsByType(enumType, effectiveTenantId);
        return ResponseEntity.ok(enums);
    }

    /**
     * Add a custom enum
     */
    @PostMapping("/custom")
    public ResponseEntity<CustomEnumDto> addCustomEnum(
            @Valid @RequestBody CreateCustomEnumRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        Long effectiveTenantId = tenantId != null ? tenantId : 1L;
        Long userId = getCurrentUserId();
        
        CustomEnumDto dto = CustomEnumDto.builder()
                .enumType(request.getEnumType())
                .value(request.getValue())
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .color(request.getColor())
                .sortOrder(request.getSortOrder())
                .metadata(request.getMetadata())
                .build();
        
        CustomEnumDto created = enumManagementService.addCustomEnum(dto, effectiveTenantId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update a custom enum
     */
    @PutMapping("/custom/{id}")
    public ResponseEntity<CustomEnumDto> updateCustomEnum(
            @PathVariable Long id,
            @Valid @RequestBody CreateCustomEnumRequest request) {
        
        Long userId = getCurrentUserId();
        
        CustomEnumDto dto = CustomEnumDto.builder()
                .enumType(request.getEnumType())
                .value(request.getValue())
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .color(request.getColor())
                .sortOrder(request.getSortOrder())
                .metadata(request.getMetadata())
                .build();
        
        CustomEnumDto updated = enumManagementService.updateCustomEnum(id, dto, userId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a custom enum (soft delete)
     */
    @DeleteMapping("/custom/{id}")
    public ResponseEntity<Void> deleteCustomEnum(@PathVariable Long id) {
        enumManagementService.deleteCustomEnum(id);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            // In a real implementation, look up user by email
            return 1L;
        } catch (Exception e) {
            return 1L;
        }
    }
}
