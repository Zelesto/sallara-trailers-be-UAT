// src/main/java/com/pgsa/trailers/service/EnumManagementService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.CustomEnumDto;
import com.pgsa.trailers.entity.enums.CustomEnum;
import com.pgsa.trailers.enums.EnumType;
import com.pgsa.trailers.repository.CustomEnumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnumManagementService {

    private final CustomEnumRepository customEnumRepository;

    /**
     * Get all enums for a specific type (system + custom)
     */
    public List<CustomEnumDto> getEnumsByType(String enumTypeCode, Long tenantId) {
        EnumType enumType = EnumType.fromCode(enumTypeCode);
        if (enumType == null) {
            throw new IllegalArgumentException("Invalid enum type: " + enumTypeCode);
        }

        // Get system enums based on type
        List<CustomEnumDto> systemEnums = getSystemEnums(enumType);

        // Get custom enums from database
        List<CustomEnum> customEnums = customEnumRepository
                .findByEnumTypeAndTenantIdAndIsActiveTrue(enumTypeCode, tenantId);

        // Combine and sort
        List<CustomEnumDto> allEnums = new ArrayList<>();
        allEnums.addAll(systemEnums);
        allEnums.addAll(customEnums.stream()
                .map(this::toDto)
                .collect(Collectors.toList()));

        // Sort by sortOrder then displayName
        allEnums.sort(Comparator.comparing(CustomEnumDto::getSortOrder)
                .thenComparing(CustomEnumDto::getDisplayName));

        return allEnums;
    }

    /**
     * Get system enums for a specific enum type
     */
    private List<CustomEnumDto> getSystemEnums(EnumType enumType) {
        List<CustomEnumDto> systemEnums = new ArrayList<>();

        switch (enumType) {
            case INVENTORY_TYPE:
                for (SystemInventoryType type : SystemInventoryType.values()) {
                    systemEnums.add(CustomEnumDto.builder()
                            .id(-System.identityHashCode(type)) // Negative ID to identify system enums
                            .enumType(enumType.getCode())
                            .value(type.name())
                            .displayName(type.getDisplayName())
                            .icon(type.getIcon())
                            .color(type.getColor())
                            .isSystem(true)
                            .isActive(true)
                            .sortOrder(0)
                            .build());
                }
                break;
            case FUEL_STATION:
                for (SystemFuelStation station : SystemFuelStation.values()) {
                    systemEnums.add(CustomEnumDto.builder()
                            .id(-System.identityHashCode(station))
                            .enumType(enumType.getCode())
                            .value(station.name())
                            .displayName(station.getDisplayName())
                            .icon(station.getIcon())
                            .color(station.getColor())
                            .isSystem(true)
                            .isActive(true)
                            .sortOrder(0)
                            .build());
                }
                break;
            case PAYMENT_TYPE:
                for (SystemPaymentType type : SystemPaymentType.values()) {
                    systemEnums.add(CustomEnumDto.builder()
                            .id(-System.identityHashCode(type))
                            .enumType(enumType.getCode())
                            .value(type.name())
                            .displayName(type.getDisplayName())
                            .icon(type.getIcon())
                            .color(type.getColor())
                            .isSystem(true)
                            .isActive(true)
                            .sortOrder(0)
                            .build());
                }
                break;
            // Add more enum types as needed
            default:
                // No system enums for this type
                break;
        }

        return systemEnums;
    }

    /**
     * Add a custom enum
     */
    @Transactional
    public CustomEnumDto addCustomEnum(CustomEnumDto dto, Long tenantId, Long userId) {
        // Validate
        validateEnumValue(dto.getEnumType(), dto.getValue());

        // Check if already exists
        Optional<CustomEnum> existing = customEnumRepository
                .findByEnumTypeAndTenantIdAndValueIgnoreCase(
                        dto.getEnumType(), tenantId, dto.getValue());

        if (existing.isPresent()) {
            throw new IllegalArgumentException("Enum value already exists: " + dto.getValue());
        }

        // Check if conflicts with system enum
        EnumType enumType = EnumType.fromCode(dto.getEnumType());
        if (enumType != null && isSystemEnumConflict(enumType, dto.getValue())) {
            throw new IllegalArgumentException("Value conflicts with system enum: " + dto.getValue());
        }

        CustomEnum customEnum = new CustomEnum();
        customEnum.setEnumType(dto.getEnumType());
        customEnum.setValue(dto.getValue().toUpperCase());
        customEnum.setDisplayName(dto.getDisplayName());
        customEnum.setDescription(dto.getDescription());
        customEnum.setIcon(dto.getIcon());
        customEnum.setColor(dto.getColor());
        customEnum.setIsActive(true);
        customEnum.setIsSystem(false);
        customEnum.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 100);
        customEnum.setTenantId(tenantId);
        customEnum.setCreatedBy(userId);
        customEnum.setUpdatedBy(userId);
        customEnum.setMetadata(dto.getMetadata());

        CustomEnum saved = customEnumRepository.save(customEnum);
        log.info("Added custom enum: {} - {} for tenant: {}", 
                dto.getEnumType(), dto.getValue(), tenantId);

        return toDto(saved);
    }

    /**
     * Update a custom enum
     */
    @Transactional
    public CustomEnumDto updateCustomEnum(Long id, CustomEnumDto dto, Long userId) {
        CustomEnum customEnum = customEnumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Enum not found: " + id));

        // Prevent modifying system enums
        if (customEnum.getIsSystem()) {
            throw new IllegalArgumentException("Cannot modify system enum");
        }

        // Check for duplicates
        if (!customEnum.getValue().equalsIgnoreCase(dto.getValue())) {
            Optional<CustomEnum> existing = customEnumRepository
                    .findByEnumTypeAndTenantIdAndValueIgnoreCase(
                            customEnum.getEnumType(), customEnum.getTenantId(), dto.getValue());
            
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new IllegalArgumentException("Enum value already exists: " + dto.getValue());
            }
        }

        customEnum.setValue(dto.getValue().toUpperCase());
        customEnum.setDisplayName(dto.getDisplayName());
        customEnum.setDescription(dto.getDescription());
        customEnum.setIcon(dto.getIcon());
        customEnum.setColor(dto.getColor());
        customEnum.setSortOrder(dto.getSortOrder());
        customEnum.setMetadata(dto.getMetadata());
        customEnum.setUpdatedBy(userId);

        CustomEnum updated = customEnumRepository.save(customEnum);
        log.info("Updated custom enum: {} - {}", updated.getEnumType(), updated.getValue());

        return toDto(updated);
    }

    /**
     * Delete a custom enum (soft delete)
     */
    @Transactional
    public void deleteCustomEnum(Long id) {
        CustomEnum customEnum = customEnumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Enum not found: " + id));

        if (customEnum.getIsSystem()) {
            throw new IllegalArgumentException("Cannot delete system enum");
        }

        // Check if enum is in use
        if (isEnumInUse(customEnum)) {
            throw new IllegalArgumentException("Cannot delete enum that is in use");
        }

        customEnum.setIsActive(false);
        customEnumRepository.save(customEnum);
        log.info("Deleted custom enum: {} - {}", customEnum.getEnumType(), customEnum.getValue());
    }

    /**
     * Check if enum value is in use
     */
    private boolean isEnumInUse(CustomEnum customEnum) {
        // Implement logic to check if enum is used in any entities
        // For example, check if any inventory items use this type
        return false; // Placeholder - implement based on your domain
    }

    /**
     * Validate enum value
     */
    private void validateEnumValue(String enumType, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Enum value cannot be empty");
        }
        
        if (!value.matches("^[a-zA-Z0-9_ ]+$")) {
            throw new IllegalArgumentException("Enum value can only contain letters, numbers, underscores, and spaces");
        }
    }

    /**
     * Check if value conflicts with system enum
     */
    private boolean isSystemEnumConflict(EnumType enumType, String value) {
        switch (enumType) {
            case INVENTORY_TYPE:
                return Arrays.stream(SystemInventoryType.values())
                        .anyMatch(e -> e.name().equalsIgnoreCase(value));
            case FUEL_STATION:
                return Arrays.stream(SystemFuelStation.values())
                        .anyMatch(e -> e.name().equalsIgnoreCase(value));
            case PAYMENT_TYPE:
                return Arrays.stream(SystemPaymentType.values())
                        .anyMatch(e -> e.name().equalsIgnoreCase(value));
            default:
                return false;
        }
    }

    private CustomEnumDto toDto(CustomEnum entity) {
        return CustomEnumDto.builder()
                .id(entity.getId())
                .enumType(entity.getEnumType())
                .value(entity.getValue())
                .displayName(entity.getDisplayName())
                .description(entity.getDescription())
                .icon(entity.getIcon())
                .color(entity.getColor())
                .isSystem(entity.getIsSystem())
                .isActive(entity.getIsActive())
                .sortOrder(entity.getSortOrder())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
