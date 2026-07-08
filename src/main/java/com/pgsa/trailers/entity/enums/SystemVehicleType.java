// src/main/java/com/pgsa/trailers/enums/SystemVehicleType.java
package com.pgsa.trailers.enums;

import lombok.Getter;

@Getter
public enum SystemVehicleType {
    TRUCK("Truck", "🚛", "#4CAF50"),
    TRAILER("Trailer", "🚌", "#2196F3"),
    CAR("Car", "🚗", "#9C27B0"),
    VAN("Van", "🚐", "#FF9800"),
    BUS("Bus", "🚍", "#F44336"),
    MOTORCYCLE("Motorcycle", "🏍️", "#607D8B"),
    HEAVY_EQUIPMENT("Heavy Equipment", "🏗️", "#795548"),
    OTHER("Other", "📋", "#757575");

    private final String displayName;
    private final String icon;
    private final String color;

    SystemVehicleType(String displayName, String icon, String color) {
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
    }

    public static SystemVehicleType fromValue(String value) {
        for (SystemVehicleType type : values()) {
            if (type.name().equalsIgnoreCase(value) || 
                type.displayName.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return OTHER;
    }
}

