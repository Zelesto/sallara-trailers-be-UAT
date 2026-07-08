// src/main/java/com/pgsa/trailers/config/EnumDataInitializer.java
package com.pgsa.trailers.config;

import com.pgsa.trailers.entity.enums.CustomEnum;
import com.pgsa.trailers.enums.EnumCategory;
import com.pgsa.trailers.repository.CustomEnumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnumDataInitializer implements ApplicationRunner {

    private final CustomEnumRepository customEnumRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (customEnumRepository.count() == 0) {
            log.info("🌱 Seeding system enums...");
            seedAllEnums();
            log.info("✅ System enums seeded successfully");
        } else {
            log.info("📊 System enums already exist, skipping seed");
        }
    }

    private void seedAllEnums() {
        List<CustomEnum> enums = new ArrayList<>();
        
        // Vehicle Types
        enums.addAll(createVehicleTypes());
        
        // Vehicle Statuses
        enums.addAll(createVehicleStatuses());
        
        // Driver Statuses
        enums.addAll(createDriverStatuses());
        
        // Load Statuses
        enums.addAll(createLoadStatuses());

        // Trip Statuses (system only - not configurable)
        enums.addAll(createTripStatuses());

        customEnumRepository.saveAll(enums);
        log.info("Seeded {} system enums", enums.size());
    }

    private List<CustomEnum> createVehicleTypes() {
        List<CustomEnum> enums = new ArrayList<>();
        for (SystemVehicleType type : SystemVehicleType.values()) {
            enums.add(createSystemEnum(
                EnumCategory.VEHICLE_TYPE.getCode(),
                type.name(),
                type.getDisplayName(),
                type.getIcon(),
                type.getColor(),
                "Standard vehicle type"
            ));
        }
        return enums;
    }

    private List<CustomEnum> createVehicleStatuses() {
        List<CustomEnum> enums = new ArrayList<>();
        for (SystemVehicleStatus status : SystemVehicleStatus.values()) {
            enums.add(createSystemEnum(
                EnumCategory.VEHICLE_STATUS.getCode(),
                status.name(),
                status.getDisplayName(),
                status.getIcon(),
                status.getColor(),
                "Standard vehicle status"
            ));
        }
        return enums;
    }

    private List<CustomEnum> createDriverStatuses() {
        List<CustomEnum> enums = new ArrayList<>();
        for (SystemDriverStatus status : SystemDriverStatus.values()) {
            enums.add(createSystemEnum(
                EnumCategory.DRIVER_STATUS.getCode(),
                status.name(),
                status.getDisplayName(),
                status.getIcon(),
                status.getColor(),
                "Standard driver status"
            ));
        }
        return enums;
    }

    private List<CustomEnum> createLoadStatuses() {
        List<CustomEnum> enums = new ArrayList<>();
        
        // Map existing LoadStatus to system enums
        String[][] loadStatuses = {
            {"PENDING", "Pending", "⏳", "#FF9800"},
            {"LOADED", "Loaded", "📦", "#2196F3"},
            {"IN_TRANSIT", "In Transit", "🚚", "#4CAF50"},
            {"DELIVERED", "Delivered", "✅", "#13DEB9"},
            {"COMPLETED", "Completed", "🏁", "#0097A7"},
            {"CANCELLED", "Cancelled", "❌", "#F44336"}
        };
        
        for (String[] status : loadStatuses) {
            enums.add(createSystemEnum(
                EnumCategory.LOAD_STATUS.getCode(),
                status[0],
                status[1],
                status[2],
                status[3],
                "Standard load status"
            ));
        }
        return enums;
    }

    private List<CustomEnum> createTripStatuses() {
        List<CustomEnum> enums = new ArrayList<>();
        
        // Map existing TripStatus to system enums
        String[][] tripStatuses = {
            {"DRAFT", "Draft", "✏️", "#9E9E9E"},
            {"PLANNED", "Planned", "📅", "#0288D1"},
            {"ASSIGNED", "Assigned", "👤", "#7B1FA2"},
            {"IN_PROGRESS", "In Progress", "🚚", "#ED6C02"},
            {"ON_HOLD", "On Hold", "⏸️", "#FF9800"},
            {"ACTIVE", "Active", "✅", "#2E7D32"},
            {"PENDING", "Pending", "⏳", "#FF9800"},
            {"COMPLETED", "Completed", "🏁", "#0097A7"},
            {"FINALIZED", "Finalized", "📊", "#388E3C"},
            {"CANCELLED", "Cancelled", "❌", "#D32F2F"},
            {"CLOSED", "Closed", "🔒", "#5D4037"}
        };
        
        for (String[] status : tripStatuses) {
            enums.add(createSystemEnum(
                EnumCategory.TRIP_STATUS.getCode(),
                status[0],
                status[1],
                status[2],
                status[3],
                "Standard trip status"
            ));
        }
        return enums;
    }

    private CustomEnum createSystemEnum(String enumType, String value, String displayName, 
                                        String icon, String color, String description) {
        CustomEnum customEnum = new CustomEnum();
        customEnum.setEnumType(enumType);
        customEnum.setValue(value);
        customEnum.setDisplayName(displayName);
        customEnum.setDescription(description);
        customEnum.setIcon(icon);
        customEnum.setColor(color);
        customEnum.setIsSystem(true);
        customEnum.setIsActive(true);
        customEnum.setSortOrder(0);
        customEnum.setTenantId(1L); // Default tenant
        return customEnum;
    }
}
