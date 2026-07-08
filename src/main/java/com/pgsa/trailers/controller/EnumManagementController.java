// src/main/java/com/pgsa/trailers/controller/EnumManagementController.java
package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.CreateCustomEnumRequest;
import com.pgsa.trailers.dto.CustomEnumDto;
import com.pgsa.trailers.service.EnumManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    @GetMapping("/{enumType}")
    public ResponseEntity<List<CustomEnumDto>> getEnumsByType(
            @PathVariable String enumType,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        Long effectiveTenantId = tenantId != null ? tenantId : 1L;
        List<CustomEnumDto> enums = enumManagementService.getEnumsByType(enumType, effectiveTenantId);
        return ResponseEntity.ok(enums);
    }

    @GetMapping
    public ResponseEntity<Page<CustomEnumDto>> getEnums(
            @RequestParam(required = false) String enumType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        Long effectiveTenantId = tenantId != null ? tenantId : 1L;
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.fromString(sortDir), sortBy);
        Page<CustomEnumDto> enums = enumManagementService.getEnumsPaginated(enumType, effectiveTenantId, pageable);
        return ResponseEntity.ok(enums);
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> getEnumTypes(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        Long effectiveTenantId = tenantId != null ? tenantId : 1L;
        List<String> types = enumManagementService.getEnumTypes(effectiveTenantId);
        return ResponseEntity.ok(types);
    }

    @PostMapping("/custom")
    public ResponseEntity<CustomEnumDto> addCustomEnum(
            @Valid @RequestBody CreateCustomEnumRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        Long effectiveTenantId = tenantId != null ? tenantId : 1L;
        Long userId = getCurrentUserId();
        
        CustomEnumDto dto = CustomEnumDto.builder()
                .enumType(request.getEnumType().toUpperCase())
                .value(request.getValue().toUpperCase())
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .color(request.getColor())
                .sortOrder(request.getSortOrder())
                .metadata(null) // Fix: Set to null instead of request.getMetadata()
                .build();
        
        CustomEnumDto created = enumManagementService.addCustomEnum(dto, effectiveTenantId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/custom/{id}")
    public ResponseEntity<CustomEnumDto> updateCustomEnum(
            @PathVariable Long id,
            @Valid @RequestBody CreateCustomEnumRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        Long effectiveTenantId = tenantId != null ? tenantId : 1L;
        Long userId = getCurrentUserId();
        
        CustomEnumDto dto = CustomEnumDto.builder()
                .enumType(request.getEnumType().toUpperCase())
                .value(request.getValue().toUpperCase())
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .color(request.getColor())
                .sortOrder(request.getSortOrder())
                .metadata(null) // Fix: Set to null instead of request.getMetadata()
                .build();
        
        CustomEnumDto updated = enumManagementService.updateCustomEnum(id, dto, effectiveTenantId, userId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/custom/{id}")
    public ResponseEntity<Void> deleteCustomEnum(
            @PathVariable Long id,
            @RequestParam String enumType,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        Long effectiveTenantId = tenantId != null ? tenantId : 1L;
        enumManagementService.deleteCustomEnum(id, enumType, effectiveTenantId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/custom/{id}/toggle")
    public ResponseEntity<CustomEnumDto> toggleEnumStatus(
            @PathVariable Long id,
            @RequestParam String enumType,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        Long effectiveTenantId = tenantId != null ? tenantId : 1L;
        Long userId = getCurrentUserId();
        CustomEnumDto updated = enumManagementService.toggleEnumStatus(id, enumType, effectiveTenantId, userId);
        return ResponseEntity.ok(updated);
    }

    private Long getCurrentUserId() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            return 1L;
        } catch (Exception e) {
            return 1L;
        }
    }
}
