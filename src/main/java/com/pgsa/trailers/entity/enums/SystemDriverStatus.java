// src/main/java/com/pgsa/trailers/enums/SystemDriverStatus.java
package com.pgsa.trailers.enums;

import lombok.Getter;

@Getter
public enum SystemDriverStatus {
    ACTIVE("Active", "🟢", "#4CAF50"),
    INACTIVE("Inactive", "⚪", "#9E9E9E"),
    SUSPENDED("Suspended", "🟠", "#FF9800"),
    ON_LEAVE("On Leave", "🔵", "#2196F3"),
    TERMINATED("Terminated", "🔴", "#F44336"),
    AVAILABLE("Available", "✅", "#4CAF50"),
    ASSIGNED("Assigned", "🔄", "#FF9800"),
    RESTING("Resting", "😴", "#9C27B0");

    private final String displayName;
    private final String icon;
    private final String color;

    SystemDriverStatus(String displayName, String icon, String color) {
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
    }
}

