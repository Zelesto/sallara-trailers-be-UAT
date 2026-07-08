// src/main/java/com/pgsa/trailers/enums/EnumType.java
package com.pgsa.trailers.enums;

import lombok.Getter;

@Getter
public enum EnumType {
    INVENTORY_TYPE("Inventory Type", "inventory_type", true),
    FUEL_STATION("Fuel Station", "fuel_station", true),
    PAYMENT_TYPE("Payment Type", "payment_type", true),
    INCIDENT_TYPE("Incident Type", "incident_type", true),
    VEHICLE_TYPE("Vehicle Type", "vehicle_type", true),
    COMMODITY_TYPE("Commodity Type", "commodity_type", true),
    CUSTOMER_TYPE("Customer Type", "customer_type", true),
    SUPPLIER_TYPE("Supplier Type", "supplier_type", true),
    DOCUMENT_TYPE("Document Type", "document_type", true),
    MAINTENANCE_TYPE("Maintenance Type", "maintenance_type", true);

    private final String displayName;
    private final String code;
    private final boolean allowUserAdditions;

    EnumType(String displayName, String code, boolean allowUserAdditions) {
        this.displayName = displayName;
        this.code = code;
        this.allowUserAdditions = allowUserAdditions;
    }

    public static EnumType fromCode(String code) {
        for (EnumType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
