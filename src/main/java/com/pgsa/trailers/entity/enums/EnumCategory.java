// src/main/java/com/pgsa/trailers/enums/EnumCategory.java
package com.pgsa.trailers.enums;

import lombok.Getter;

@Getter
public enum EnumCategory {
    
    // User-configurable enums
    VEHICLE_TYPE("Vehicle Type", "vehicle_type", true),
    VEHICLE_STATUS("Vehicle Status", "vehicle_status", true),
    DRIVER_STATUS("Driver Status", "driver_status", true),
    LOAD_STATUS("Load Status", "load_status", true),
    TRIP_STATUS("Trip Status", "trip_status", false), // Keep as system enum
    
    // System-only enums (not configurable)
    ACCOUNT_TYPE("Account Type", "account_type", false),
    ACCOUNT_STATEMENT_STATUS("Account Statement Status", "account_statement_status", false),
    PAYMENT_STATUS("Payment Status", "payment_status", false),
    RECONCILIATION_STATUS("Reconciliation Status", "reconciliation_status", false),
    STOCK_MOVEMENT_TYPE("Stock Movement Type", "stock_movement_type", false),
    STOCK_MOVEMENT_REFERENCE_TYPE("Stock Movement Reference Type", "stock_movement_reference_type", false),
    STOCK_COUNT_STATUS("Stock Count Status", "stock_count_status", false),
    INVENTORY_LOCATION_TYPE("Inventory Location Type", "inventory_location_type", false);

    private final String displayName;
    private final String code;
    private final boolean userConfigurable;

    EnumCategory(String displayName, String code, boolean userConfigurable) {
        this.displayName = displayName;
        this.code = code;
        this.userConfigurable = userConfigurable;
    }

    public static EnumCategory fromCode(String code) {
        for (EnumCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return null;
    }
}
