// src/main/java/com/pgsa/trailers/enums/SystemVehicleStatus.java
package com.pgsa.trailers.enums;

import lombok.Getter;

@Getter
public enum SystemVehicleStatus {
    AVAILABLE("Available", "✅", "#4CAF50"),
    ASSIGNED("Assigned", "🔄", "#FF9800"),
    IN_USE("In Use", "🚛", "#2196F3"),
    MAINTENANCE("Maintenance", "🔧", "#FF9800"),
    REPAIR("Repair", "🔴", "#F44336"),
    OUT_OF_SERVICE("Out of Service", "❌", "#9E9E9E"),
    RETIRED("Retired", "📋", "#757575"),
    SOLD("Sold", "💰", "#9C27B0"),
    DECOMMISSIONED("Decommissioned", "⚫", "#424242");

    private final String displayName;
    private final String icon;
    private final String color;

    SystemVehicleStatus(String displayName, String icon, String color) {
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
    }
}
