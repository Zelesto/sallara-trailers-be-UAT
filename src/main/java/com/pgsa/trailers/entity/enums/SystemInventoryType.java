// src/main/java/com/pgsa/trailers/enums/SystemInventoryType.java
package com.pgsa.trailers.enums;

import lombok.Getter;

@Getter
public enum SystemInventoryType {
    RAW_MATERIAL("Raw Material", "📦", "#9E9E9E"),
    FINISHED_GOOD("Finished Good", "✅", "#4CAF50"),
    PACKAGING("Packaging", "📦", "#2196F3"),
    SUPPLIES("Supplies", "🔧", "#FF9800"),
    EQUIPMENT("Equipment", "⚙️", "#607D8B"),
    OTHER("Other", "📋", "#757575");

    private final String displayName;
    private final String icon;
    private final String color;

    SystemInventoryType(String displayName, String icon, String color) {
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
    }

    public static SystemInventoryType fromValue(String value) {
        for (SystemInventoryType type : values()) {
            if (type.name().equalsIgnoreCase(value) || 
                type.displayName.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return OTHER;
    }
}
