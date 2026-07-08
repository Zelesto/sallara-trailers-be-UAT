// src/main/java/com/pgsa/trailers/service/EnumValidationService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.enums.EnumCategory;
import com.pgsa.trailers.repository.CustomEnumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnumValidationService {

    private final CustomEnumRepository customEnumRepository;

    @Cacheable(value = "validEnums", key = "#enumType + '_' + #tenantId")
    public Set<String> getValidEnumValues(String enumType, Long tenantId) {
        // Get system enums (from code)
        Set<String> systemValues = getSystemEnumValues(enumType);
        
        // Get custom enums (from database)
        List<CustomEnum> customEnums = customEnumRepository
                .findByEnumTypeAndTenantIdAndIsActiveTrue(enumType, tenantId);
        
        Set<String> customValues = customEnums.stream()
                .map(CustomEnum::getValue)
                .collect(Collectors.toSet());
        
        // Combine both sets
        systemValues.addAll(customValues);
        return systemValues;
    }

    public boolean isValidEnumValue(String enumType, String value, Long tenantId) {
        if (value == null) return true;
        Set<String> validValues = getValidEnumValues(enumType, tenantId);
        return validValues.contains(value.toUpperCase());
    }

    private Set<String> getSystemEnumValues(String enumType) {
        EnumCategory category = EnumCategory.fromCode(enumType);
        if (category == null) return Set.of();

        // Based on enum type, return system values
        switch (category) {
            case VEHICLE_TYPE:
                return Set.of(SystemVehicleType.values()).stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet());
            case VEHICLE_STATUS:
                return Set.of(SystemVehicleStatus.values()).stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet());
            case DRIVER_STATUS:
                return Set.of(SystemDriverStatus.values()).stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet());
            case LOAD_STATUS:
                return Set.of(SystemLoadStatus.values()).stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet());
            default:
                return Set.of();
        }
    }
}
