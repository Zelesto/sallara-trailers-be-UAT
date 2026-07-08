// src/main/java/com/pgsa/trailers/service/EnumManagementService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.CustomEnumDto;
import com.pgsa.trailers.dto.CreateCustomEnumRequest;
import com.pgsa.trailers.entity.enums.CustomEnum;
import com.pgsa.trailers.enums.EnumCategory;
import com.pgsa.trailers.enums.SystemDriverStatus;
import com.pgsa.trailers.enums.SystemVehicleStatus;
import com.pgsa.trailers.enums.SystemVehicleType;
import com.pgsa.trailers.repository.CustomEnumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    @Cacheable(value = "enums", key = "#enumType + '_' + #tenantId")
    public List<CustomEnumDto> getEnumsByType(String enumType, Long tenantId) {
        EnumCategory category = EnumCategory.fromCode(enumType);
        if (category == null) {
            throw new IllegalArgumentException("Invalid enum type: " + enumType);
        }

        // Get system enums
        List<CustomEnumDto> systemEnums = getSystemEnums(category);

        // Get custom enums from database
        List<CustomEnum> customEnums = customEnumRepository
                .findActiveByEnumTypeAndTenantId(enumType, tenantId);

        // Combine and convert to DTOs
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
     * Get system enums for a specific category
     */
    private List<CustomEnumDto> getSystemEnums(EnumCategory category) {
        List<CustomEnumDto> systemEnums = new ArrayList<>();

        switch (category) {
            case VEHICLE_TYPE:
                for (SystemVehicleType type : SystemVehicleType.values()) {
                    systemEnums.add(createSystemEnumDto(
                            category.getCode(),
                            type.name(),
                            type.getDisplayName(),
                            type.getIcon(),
                            type.getColor(),
                            "System vehicle type"
                    ));
                }
                break;

            case VEHICLE_STATUS:
                for (SystemVehicleStatus status : SystemVehicleStatus.values()) {
                    systemEnums.add(createSystemEnumDto(
                            category.getCode(),
                            status.name(),
                            status.getDisplayName(),
                            status.getIcon(),
                            status.getColor(),
                            "System vehicle status"
                    ));
                }
                break;

            case DRIVER_STATUS:
                for (SystemDriverStatus status : SystemDriverStatus.values()) {
                    systemEnums.add(createSystemEnumDto(
                            category.getCode(),
                            status.name(),
                            status.getDisplayName(),
                            status.getIcon(),
                            status.getColor(),
                            "System driver status"
                    ));
                }
                break;

            case LOAD_STATUS:
                // LoadStatus is a simple enum without icons/colors
                String[][] loadStatuses = {
                    {"PENDING", "Pending", "⏳", "#FF9800"},
                    {"LOADED", "Loaded", "📦", "#2196F3"},
                    {"IN_TRANSIT", "In Transit", "🚚", "#4CAF50"},
                    {"DELIVERED", "Delivered", "✅", "#13DEB9"},
                    {"COMPLETED", "Completed", "🏁", "#0097A7"},
                    {"CANCELLED", "Cancelled", "❌", "#F44336"}
                };
                for (String[] status : loadStatuses) {
                    systemEnums.add(createSystemEnumDto(
                            category.getCode(),
                            status[0],
                            status[1],
                            status[2],
                            status[3],
                            "System load status"
                    ));
                }
                break;

            default:
                // No system enums for other types
                break;
        }

        return systemEnums;
    }

    private CustomEnumDto createSystemEnumDto(String enumType, String value, String displayName,
                                               String icon, String color, String description) {
        return CustomEnumDto.builder()
                .id(null) // System enums don't have an ID
                .enumType(enumType)
                .value(value)
                .displayName(displayName)
                .description(description)
                .icon(icon)
                .color(color)
                .isSystem(true)
                .isActive(true)
                .sortOrder(0)
                .build();
    }

    /**
     * Add a custom enum
     */
    @Transactional
    @CacheEvict(value = "enums", key = "#dto.enumType + '_' + #tenantId")
    public CustomEnumDto addCustomEnum(CustomEnumDto dto, Long tenantId, Long userId) {
        // Validate enum type
        EnumCategory category = EnumCategory.fromCode(dto.getEnumType());
        if (category == null) {
            throw new IllegalArgumentException("Invalid enum type: " + dto.getEnumType());
        }

        // Check if this enum type allows user additions
        if (!category.isUserConfigurable()) {
            throw new IllegalArgumentException("Enum type '" + dto.getEnumType() + "' does not allow custom values");
        }

        // Validate the value
        validateEnumValue(dto.getValue());

        // Check if value already exists (case-insensitive)
        Optional<CustomEnum> existing = customEnumRepository
                .findByEnumTypeAndTenantIdAndValueIgnoreCase(
                        dto.getEnumType(), tenantId, dto.getValue());

        if (existing.isPresent()) {
            throw new IllegalArgumentException("Enum value already exists: " + dto.getValue());
        }

        // Check if conflicts with system enum
        if (isSystemEnumConflict(category, dto.getValue())) {
            throw new IllegalArgumentException("Value conflicts with system enum: " + dto.getValue());
        }

        // Check if display name already exists
        if (customEnumRepository.existsByEnumTypeAndTenantIdAndValueIgnoreCase(
                dto.getEnumType(), tenantId, dto.getDisplayName())) {
            throw new IllegalArgumentException("Display name already exists: " + dto.getDisplayName());
        }

        CustomEnum customEnum = new CustomEnum();
        customEnum.setEnumType(dto.getEnumType().toUpperCase());
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
    @CacheEvict(value = "enums", key = "#dto.enumType + '_' + #tenantId")
    public CustomEnumDto updateCustomEnum(Long id, CustomEnumDto dto, Long tenantId, Long userId) {
        CustomEnum customEnum = customEnumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Enum not found: " + id));

        // Prevent modifying system enums
        if (customEnum.getIsSystem()) {
            throw new IllegalArgumentException("Cannot modify system enum");
        }

        // Check if this enum type allows user additions
        EnumCategory category = EnumCategory.fromCode(customEnum.getEnumType());
        if (category == null || !category.isUserConfigurable()) {
            throw new IllegalArgumentException("Enum type '" + customEnum.getEnumType() + "' does not allow modifications");
        }

        // Check for duplicate values (excluding current)
        if (!customEnum.getValue().equalsIgnoreCase(dto.getValue())) {
            Optional<CustomEnum> existing = customEnumRepository
                    .findByEnumTypeAndTenantIdAndValueIgnoreCase(
                            customEnum.getEnumType(), tenantId, dto.getValue());
            
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new IllegalArgumentException("Enum value already exists: " + dto.getValue());
            }

            // Check conflict with system enum
            if (isSystemEnumConflict(category, dto.getValue())) {
                throw new IllegalArgumentException("Value conflicts with system enum: " + dto.getValue());
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
    @CacheEvict(value = "enums", key = "#enumType + '_' + #tenantId")
    public void deleteCustomEnum(Long id, String enumType, Long tenantId) {
        CustomEnum customEnum = customEnumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Enum not found: " + id));

        if (customEnum.getIsSystem()) {
            throw new IllegalArgumentException("Cannot delete system enum");
        }

        // Check if enum is in use
        if (isEnumInUse(customEnum)) {
            throw new IllegalArgumentException("Cannot delete enum that is currently in use");
        }

        customEnum.setIsActive(false);
        customEnumRepository.save(customEnum);
        log.info("Deleted custom enum: {} - {}", customEnum.getEnumType(), customEnum.getValue());
    }

    /**
     * Toggle enum active status
     */
    @Transactional
    @CacheEvict(value = "enums", key = "#enumType + '_' + #tenantId")
    public CustomEnumDto toggleEnumStatus(Long id, String enumType, Long tenantId, Long userId) {
        CustomEnum customEnum = customEnumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Enum not found: " + id));

        if (customEnum.getIsSystem()) {
            throw new IllegalArgumentException("Cannot modify system enum");
        }

        customEnum.setIsActive(!customEnum.getIsActive());
        customEnum.setUpdatedBy(userId);
        
        CustomEnum updated = customEnumRepository.save(customEnum);
        log.info("Toggled enum status: {} - {} -> active: {}", 
                customEnum.getEnumType(), customEnum.getValue(), customEnum.getIsActive());

        return toDto(updated);
    }

    /**
     * Get paginated enums for admin
     */
    public Page<CustomEnumDto> getEnumsPaginated(String enumType, Long tenantId, Pageable pageable) {
        Page<CustomEnum> page;
        if (enumType != null && !enumType.isEmpty()) {
            page = customEnumRepository.findByEnumTypeAndTenantId(enumType, tenantId, pageable);
        } else {
            page = customEnumRepository.findAllByTenantId(tenantId, pageable);
        }
        return page.map(this::toDto);
    }

    /**
     * Get all enum types available for a tenant
     */
    public List<String> getEnumTypes(Long tenantId) {
        List<String> systemTypes = Arrays.stream(EnumCategory.values())
                .map(EnumCategory::getCode)
                .collect(Collectors.toList());
        
        List<String> customTypes = customEnumRepository.findDistinctEnumTypesByTenantId(tenantId);
        
        // Combine and return unique types
        Set<String> allTypes = new HashSet<>(systemTypes);
        allTypes.addAll(customTypes);
        return new ArrayList<>(allTypes);
    }

    /**
     * Bulk create enums (for migration)
     */
    @Transactional
    public void bulkCreateEnums(List<CustomEnumDto> enums, Long tenantId, Long userId) {
        List<CustomEnum> entities = enums.stream()
                .map(dto -> {
                    CustomEnum customEnum = new CustomEnum();
                    customEnum.setEnumType(dto.getEnumType());
                    customEnum.setValue(dto.getValue().toUpperCase());
                    customEnum.setDisplayName(dto.getDisplayName());
                    customEnum.setDescription(dto.getDescription());
                    customEnum.setIcon(dto.getIcon());
                    customEnum.setColor(dto.getColor());
                    customEnum.setIsSystem(false);
                    customEnum.setIsActive(true);
                    customEnum.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 100);
                    customEnum.setTenantId(tenantId);
                    customEnum.setCreatedBy(userId);
                    customEnum.setUpdatedBy(userId);
                    return customEnum;
                })
                .collect(Collectors.toList());

        customEnumRepository.saveAll(entities);
        log.info("Bulk created {} enums for tenant: {}", entities.size(), tenantId);
    }

    /**
     * Check if enum is in use
     */
    private boolean isEnumInUse(CustomEnum customEnum) {
        // Implement based on your domain logic
        // Check if any entities reference this enum value
        switch (customEnum.getEnumType()) {
            case "VEHICLE_TYPE":
                return vehicleRepository.existsByVehicleType(customEnum.getValue());
            case "VEHICLE_STATUS":
                return vehicleRepository.existsByStatus(customEnum.getValue());
            case "DRIVER_STATUS":
                return driverRepository.existsByStatus(customEnum.getValue());
            case "LOAD_STATUS":
                return loadRepository.existsByStatus(customEnum.getValue());
            default:
                return false;
        }
    }

    /**
     * Validate enum value
     */
    private void validateEnumValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Enum value cannot be empty");
        }
        
        if (!value.matches("^[a-zA-Z0-9_ ]+$")) {
            throw new IllegalArgumentException("Enum value can only contain letters, numbers, underscores, and spaces");
        }
        
        if (value.length() > 100) {
            throw new IllegalArgumentException("Enum value must be less than 100 characters");
        }
    }

    /**
     * Check if value conflicts with system enum
     */
    private boolean isSystemEnumConflict(EnumCategory category, String value) {
        switch (category) {
            case VEHICLE_TYPE:
                return Arrays.stream(SystemVehicleType.values())
                        .anyMatch(e -> e.name().equalsIgnoreCase(value));
            case VEHICLE_STATUS:
                return Arrays.stream(SystemVehicleStatus.values())
                        .anyMatch(e -> e.name().equalsIgnoreCase(value));
            case DRIVER_STATUS:
                return Arrays.stream(SystemDriverStatus.values())
                        .anyMatch(e -> e.name().equalsIgnoreCase(value));
            case LOAD_STATUS:
                // LoadStatus is from the old code - we'll keep it as system
                return true; // Prevent adding duplicates of load status
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

    // Inject repositories for isEnumInUse check
    // You'll need to add these to your constructor and inject them
    // private final VehicleRepository vehicleRepository;
    // private final DriverRepository driverRepository;
    // private final LoadRepository loadRepository;
}
