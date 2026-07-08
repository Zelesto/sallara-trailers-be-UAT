// src/main/java/com/pgsa/trailers/service/EnumManagementService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.CustomEnumDto;
import com.pgsa.trailers.entity.enums.CustomEnum;
import com.pgsa.trailers.enums.EnumCategory;
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

    @Cacheable(value = "enums", key = "#enumType + '_' + #tenantId")
    public List<CustomEnumDto> getEnumsByType(String enumType, Long tenantId) {
        EnumCategory category = EnumCategory.fromCode(enumType);
        if (category == null) {
            throw new IllegalArgumentException("Invalid enum type: " + enumType);
        }

        List<CustomEnumDto> systemEnums = getSystemEnums(category);

        List<CustomEnum> customEnums = customEnumRepository
                .findActiveByEnumTypeAndTenantId(enumType, tenantId);

        List<CustomEnumDto> allEnums = new ArrayList<>();
        allEnums.addAll(systemEnums);
        allEnums.addAll(customEnums.stream()
                .map(this::toDto)
                .collect(Collectors.toList()));

        allEnums.sort(Comparator.comparing(CustomEnumDto::getSortOrder)
                .thenComparing(CustomEnumDto::getDisplayName));

        return allEnums;
    }

    private List<CustomEnumDto> getSystemEnums(EnumCategory category) {
        List<CustomEnumDto> systemEnums = new ArrayList<>();

        switch (category) {
            case VEHICLE_TYPE:
                String[][] vehicleTypes = {
                    {"TRUCK", "Truck", "🚛", "#4CAF50"},
                    {"TRAILER", "Trailer", "🚌", "#2196F3"},
                    {"CAR", "Car", "🚗", "#9C27B0"},
                    {"VAN", "Van", "🚐", "#FF9800"},
                    {"BUS", "Bus", "🚍", "#F44336"},
                    {"MOTORCYCLE", "Motorcycle", "🏍️", "#607D8B"},
                    {"HEAVY_EQUIPMENT", "Heavy Equipment", "🏗️", "#795548"},
                    {"OTHER", "Other", "📋", "#757575"}
                };
                for (String[] type : vehicleTypes) {
                    systemEnums.add(createSystemEnumDto(
                            category.getCode(), type[0], type[1], type[2], type[3], "System vehicle type"
                    ));
                }
                break;

            case VEHICLE_STATUS:
                String[][] vehicleStatuses = {
                    {"AVAILABLE", "Available", "✅", "#4CAF50"},
                    {"ASSIGNED", "Assigned", "🔄", "#FF9800"},
                    {"IN_USE", "In Use", "🚛", "#2196F3"},
                    {"MAINTENANCE", "Maintenance", "🔧", "#FF9800"},
                    {"REPAIR", "Repair", "🔴", "#F44336"},
                    {"OUT_OF_SERVICE", "Out of Service", "❌", "#9E9E9E"},
                    {"RETIRED", "Retired", "📋", "#757575"},
                    {"SOLD", "Sold", "💰", "#9C27B0"},
                    {"DECOMMISSIONED", "Decommissioned", "⚫", "#424242"}
                };
                for (String[] status : vehicleStatuses) {
                    systemEnums.add(createSystemEnumDto(
                            category.getCode(), status[0], status[1], status[2], status[3], "System vehicle status"
                    ));
                }
                break;

            case DRIVER_STATUS:
                String[][] driverStatuses = {
                    {"ACTIVE", "Active", "🟢", "#4CAF50"},
                    {"INACTIVE", "Inactive", "⚪", "#9E9E9E"},
                    {"SUSPENDED", "Suspended", "🟠", "#FF9800"},
                    {"ON_LEAVE", "On Leave", "🔵", "#2196F3"},
                    {"TERMINATED", "Terminated", "🔴", "#F44336"},
                    {"AVAILABLE", "Available", "✅", "#4CAF50"},
                    {"ASSIGNED", "Assigned", "🔄", "#FF9800"},
                    {"RESTING", "Resting", "😴", "#9C27B0"}
                };
                for (String[] status : driverStatuses) {
                    systemEnums.add(createSystemEnumDto(
                            category.getCode(), status[0], status[1], status[2], status[3], "System driver status"
                    ));
                }
                break;

            case LOAD_STATUS:
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
                            category.getCode(), status[0], status[1], status[2], status[3], "System load status"
                    ));
                }
                break;

            default:
                break;
        }

        return systemEnums;
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
                .metadata(new HashMap<>()) // Fix: Use empty Map instead of null
                .build();
    }

    @Transactional
    @CacheEvict(value = "enums", key = "#dto.enumType + '_' + #tenantId")
    public CustomEnumDto addCustomEnum(CustomEnumDto dto, Long tenantId, Long userId) {
        EnumCategory category = EnumCategory.fromCode(dto.getEnumType());
        if (category == null) {
            throw new IllegalArgumentException("Invalid enum type: " + dto.getEnumType());
        }

        if (!category.isUserConfigurable()) {
            throw new IllegalArgumentException("Enum type '" + dto.getEnumType() + "' does not allow custom values");
        }

        validateEnumValue(dto.getValue());

        Optional<CustomEnum> existing = customEnumRepository
                .findByEnumTypeAndTenantIdAndValueIgnoreCase(
                        dto.getEnumType(), tenantId, dto.getValue());

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
        customEnum.setTenantId(tenantId);
        customEnum.setCreatedBy(userId);
        customEnum.setUpdatedBy(userId);
        customEnum.setMetadata(new HashMap<>()); // Fix: Use empty Map instead of null

        CustomEnum saved = customEnumRepository.save(customEnum);
        log.info("Added custom enum: {} - {} for tenant: {}", 
                dto.getEnumType(), dto.getValue(), tenantId);

        return toDto(saved);
    }

    @Transactional
    @CacheEvict(value = "enums", key = "#dto.enumType + '_' + #tenantId")
    public CustomEnumDto updateCustomEnum(Long id, CustomEnumDto dto, Long tenantId, Long userId) {
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
                    .findByEnumTypeAndTenantIdAndValueIgnoreCase(
                            customEnum.getEnumType(), tenantId, dto.getValue());
            
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
        customEnum.setMetadata(new HashMap<>()); // Fix: Use empty Map instead of null
        customEnum.setUpdatedBy(userId);

        CustomEnum updated = customEnumRepository.save(customEnum);
        log.info("Updated custom enum: {} - {}", updated.getEnumType(), updated.getValue());

        return toDto(updated);
    }

    @Transactional
    @CacheEvict(value = "enums", key = "#enumType + '_' + #tenantId")
    public void deleteCustomEnum(Long id, String enumType, Long tenantId) {
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

    public Page<CustomEnumDto> getEnumsPaginated(String enumType, Long tenantId, Pageable pageable) {
        Page<CustomEnum> page;
        if (enumType != null && !enumType.isEmpty()) {
            page = customEnumRepository.findByEnumTypeAndTenantId(enumType, tenantId, pageable);
        } else {
            page = customEnumRepository.findAllByTenantId(tenantId, pageable);
        }
        return page.map(this::toDto);
    }

    public List<String> getEnumTypes(Long tenantId) {
        List<String> systemTypes = Arrays.stream(EnumCategory.values())
                .map(EnumCategory::getCode)
                .collect(Collectors.toList());
        
        List<String> customTypes = customEnumRepository.findDistinctEnumTypesByTenantId(tenantId);
        
        Set<String> allTypes = new HashSet<>(systemTypes);
        allTypes.addAll(customTypes);
        return new ArrayList<>(allTypes);
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
                .metadata(entity.getMetadata() != null ? entity.getMetadata() : new HashMap<>()) // Fix: Handle null metadata
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
