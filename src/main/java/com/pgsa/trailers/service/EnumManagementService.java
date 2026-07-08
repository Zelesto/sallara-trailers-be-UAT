// src/main/java/com/pgsa/trailers/service/EnumManagementService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.CustomEnumDto;
import com.pgsa.trailers.entity.enums.CustomEnum;
import com.pgsa.trailers.enums.*;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class EnumManagementService {

    private final CustomEnumRepository customEnumRepository;

    @Cacheable(value = "enums", key = "#enumType")
    public List<CustomEnumDto> getEnumsByType(String enumType) {
        try {
            EnumCategory category = EnumCategory.fromCode(enumType);
            if (category == null) {
                log.warn("Invalid enum type requested: {}", enumType);
                return Collections.emptyList();
            }

            List<CustomEnumDto> systemEnums = getSystemEnums(category);

            List<CustomEnum> customEnums = customEnumRepository
                    .findActiveByEnumTypeOrderBySortOrder(enumType);
            
            if (customEnums == null) {
                customEnums = Collections.emptyList();
            }

            List<CustomEnumDto> allEnums = new ArrayList<>();
            allEnums.addAll(systemEnums);
            allEnums.addAll(customEnums.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList()));

            allEnums.sort(Comparator.comparing(CustomEnumDto::getSortOrder)
                    .thenComparing(CustomEnumDto::getDisplayName));

            return allEnums;
        } catch (Exception e) {
            log.error("Error getting enums for type {}: {}", enumType, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<CustomEnumDto> getSystemEnums(EnumCategory category) {
        List<CustomEnumDto> systemEnums = new ArrayList<>();

        switch (category) {
            case VEHICLE_TYPE:
                for (VehicleType type : VehicleType.values()) {
                    systemEnums.add(createSystemEnumDto(
                            category.getCode(), 
                            type.name(), 
                            type.name(), // Use enum name as display name
                            getIconForVehicleType(type),
                            getColorForVehicleType(type),
                            "System vehicle type"
                    ));
                }
                break;

            case VEHICLE_STATUS:
                for (VehicleStatus status : VehicleStatus.values()) {
                    systemEnums.add(createSystemEnumDto(
                            category.getCode(),
                            status.name(),
                            status.name().replace("_", " "),
                            getIconForVehicleStatus(status),
                            getColorForVehicleStatus(status),
                            "System vehicle status"
                    ));
                }
                break;

            case DRIVER_STATUS:
                for (DriverStatus status : DriverStatus.values()) {
                    systemEnums.add(createSystemEnumDto(
                            category.getCode(),
                            status.name(),
                            status.name().replace("_", " "),
                            getIconForDriverStatus(status),
                            getColorForDriverStatus(status),
                            "System driver status"
                    ));
                }
                break;

            case LOAD_STATUS:
                for (LoadStatus status : LoadStatus.values()) {
                    systemEnums.add(createSystemEnumDto(
                            category.getCode(),
                            status.name(),
                            status.name().replace("_", " "),
                            getIconForLoadStatus(status),
                            getColorForLoadStatus(status),
                            "System load status"
                    ));
                }
                break;

            default:
                break;
        }

        return systemEnums;
    }

    // Helper methods for icons and colors
    private String getIconForVehicleType(VehicleType type) {
        switch (type) {
            case TRUCK: return "🚛";
            case TRAILER: return "🚌";
            case CAR: return "🚗";
            case VAN: return "🚐";
            case BUS: return "🚍";
            case MOTORCYCLE: return "🏍️";
            case HEAVY_EQUIPMENT: return "🏗️";
            default: return "📋";
        }
    }

    private String getColorForVehicleType(VehicleType type) {
        switch (type) {
            case TRUCK: return "#4CAF50";
            case TRAILER: return "#2196F3";
            case CAR: return "#9C27B0";
            case VAN: return "#FF9800";
            case BUS: return "#F44336";
            case MOTORCYCLE: return "#607D8B";
            case HEAVY_EQUIPMENT: return "#795548";
            default: return "#757575";
        }
    }

    private String getIconForVehicleStatus(VehicleStatus status) {
        switch (status) {
            case AVAILABLE: return "✅";
            case ASSIGNED: return "🔄";
            case IN_USE: return "🚛";
            case MAINTENANCE: return "🔧";
            case REPAIR: return "🔴";
            case OUT_OF_SERVICE: return "❌";
            case RETIRED: return "📋";
            case SOLD: return "💰";
            case DECOMMISSIONED: return "⚫";
            case ACTIVE: return "🟢";
            case INACTIVE: return "⚪";
            default: return "📋";
        }
    }

    private String getColorForVehicleStatus(VehicleStatus status) {
        switch (status) {
            case AVAILABLE: return "#4CAF50";
            case ASSIGNED: return "#FF9800";
            case IN_USE: return "#2196F3";
            case MAINTENANCE: return "#FF9800";
            case REPAIR: return "#F44336";
            case OUT_OF_SERVICE: return "#9E9E9E";
            case RETIRED: return "#757575";
            case SOLD: return "#9C27B0";
            case DECOMMISSIONED: return "#424242";
            case ACTIVE: return "#4CAF50";
            case INACTIVE: return "#9E9E9E";
            default: return "#757575";
        }
    }

    private String getIconForDriverStatus(DriverStatus status) {
        switch (status) {
            case ACTIVE: return "🟢";
            case INACTIVE: return "⚪";
            case SUSPENDED: return "🟠";
            case ON_LEAVE: return "🔵";
            case TERMINATED: return "🔴";
            default: return "📋";
        }
    }

    private String getColorForDriverStatus(DriverStatus status) {
        switch (status) {
            case ACTIVE: return "#4CAF50";
            case INACTIVE: return "#9E9E9E";
            case SUSPENDED: return "#FF9800";
            case ON_LEAVE: return "#2196F3";
            case TERMINATED: return "#F44336";
            default: return "#757575";
        }
    }

    private String getIconForLoadStatus(LoadStatus status) {
        switch (status) {
            case PENDING: return "⏳";
            case LOADED: return "📦";
            case IN_TRANSIT: return "🚚";
            case DELIVERED: return "✅";
            case COMPLETED: return "🏁";
            case CANCELLED: return "❌";
            default: return "📋";
        }
    }

    private String getColorForLoadStatus(LoadStatus status) {
        switch (status) {
            case PENDING: return "#FF9800";
            case LOADED: return "#2196F3";
            case IN_TRANSIT: return "#4CAF50";
            case DELIVERED: return "#13DEB9";
            case COMPLETED: return "#0097A7";
            case CANCELLED: return "#F44336";
            default: return "#757575";
        }
    }

    private CustomEnumDto createSystemEnumDto(String enumType, String value, String displayName,
                                               String icon, String color, String description) {
        return CustomEnumDto.builder()
                .id(null)
                .enumType(enumType)
                .value(value)
                .displayName(displayName)
                .description(description)
                .icon(icon)
                .color(color)
                .isSystem(true)
                .isActive(true)
                .sortOrder(0)
                .metadata(new HashMap<>())
                .build();
    }

    @Transactional
    @CacheEvict(value = "enums", key = "#dto.enumType")
    public CustomEnumDto addCustomEnum(CustomEnumDto dto, Long userId) {
        EnumCategory category = EnumCategory.fromCode(dto.getEnumType());
        if (category == null) {
            throw new IllegalArgumentException("Invalid enum type: " + dto.getEnumType());
        }

        if (!category.isUserConfigurable()) {
            throw new IllegalArgumentException("Enum type '" + dto.getEnumType() + "' does not allow custom values");
        }

        validateEnumValue(dto.getValue());

        Optional<CustomEnum> existing = customEnumRepository
                .findByEnumTypeAndValueIgnoreCase(dto.getEnumType(), dto.getValue());

        if (existing.isPresent()) {
            throw new IllegalArgumentException("Enum value already exists: " + dto.getValue());
        }

        if (isSystemEnumConflict(category, dto.getValue())) {
            throw new IllegalArgumentException("Value conflicts with system enum: " + dto.getValue());
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
        customEnum.setCreatedBy(userId);
        customEnum.setUpdatedBy(userId);
        customEnum.setMetadata(new HashMap<>());

        CustomEnum saved = customEnumRepository.save(customEnum);
        log.info("Added custom enum: {} - {}", dto.getEnumType(), dto.getValue());

        return toDto(saved);
    }

    @Transactional
    @CacheEvict(value = "enums", key = "#dto.enumType")
    public CustomEnumDto updateCustomEnum(Long id, CustomEnumDto dto, Long userId) {
        CustomEnum customEnum = customEnumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Enum not found: " + id));

        if (customEnum.getIsSystem()) {
            throw new IllegalArgumentException("Cannot modify system enum");
        }

        EnumCategory category = EnumCategory.fromCode(customEnum.getEnumType());
        if (category == null || !category.isUserConfigurable()) {
            throw new IllegalArgumentException("Enum type '" + customEnum.getEnumType() + "' does not allow modifications");
        }

        if (!customEnum.getValue().equalsIgnoreCase(dto.getValue())) {
            Optional<CustomEnum> existing = customEnumRepository
                    .findByEnumTypeAndValueIgnoreCase(customEnum.getEnumType(), dto.getValue());
            
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new IllegalArgumentException("Enum value already exists: " + dto.getValue());
            }

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
        customEnum.setUpdatedBy(userId);

        CustomEnum updated = customEnumRepository.save(customEnum);
        log.info("Updated custom enum: {} - {}", updated.getEnumType(), updated.getValue());

        return toDto(updated);
    }

    @Transactional
    @CacheEvict(value = "enums", key = "#enumType")
    public void deleteCustomEnum(Long id, String enumType) {
        CustomEnum customEnum = customEnumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Enum not found: " + id));

        if (customEnum.getIsSystem()) {
            throw new IllegalArgumentException("Cannot delete system enum");
        }

        customEnum.setIsActive(false);
        customEnumRepository.save(customEnum);
        log.info("Deleted custom enum: {} - {}", customEnum.getEnumType(), customEnum.getValue());
    }

    @Transactional
    @CacheEvict(value = "enums", key = "#enumType")
    public CustomEnumDto toggleEnumStatus(Long id, String enumType, Long userId) {
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

    public Page<CustomEnumDto> getEnumsPaginated(String enumType, Pageable pageable) {
        try {
            Page<CustomEnum> page;
            if (enumType != null && !enumType.isEmpty()) {
                page = customEnumRepository.findByEnumType(enumType, pageable);
            } else {
                page = customEnumRepository.findAll(pageable);
            }
            return page.map(this::toDto);
        } catch (Exception e) {
            log.error("Error getting paginated enums: {}", e.getMessage(), e);
            return Page.empty(pageable);
        }
    }

    public List<String> getEnumTypes() {
        try {
            List<String> systemTypes = Arrays.stream(EnumCategory.values())
                    .map(EnumCategory::getCode)
                    .collect(Collectors.toList());
            
            List<String> customTypes = customEnumRepository.findDistinctEnumTypes();
            if (customTypes == null) {
                customTypes = Collections.emptyList();
            }
            
            Set<String> allTypes = new HashSet<>(systemTypes);
            allTypes.addAll(customTypes);
            return new ArrayList<>(allTypes);
        } catch (Exception e) {
            log.error("Error getting enum types: {}", e.getMessage(), e);
            // Return system types as fallback
            return Arrays.stream(EnumCategory.values())
                    .map(EnumCategory::getCode)
                    .collect(Collectors.toList());
        }
    }

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

    private boolean isSystemEnumConflict(EnumCategory category, String value) {
        List<CustomEnumDto> systemEnums = getSystemEnums(category);
        return systemEnums.stream()
                .anyMatch(e -> e.getValue().equalsIgnoreCase(value));
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
                .metadata(entity.getMetadata() != null ? entity.getMetadata() : new HashMap<>())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
