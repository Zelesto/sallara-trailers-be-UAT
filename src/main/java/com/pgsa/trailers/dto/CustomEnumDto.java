// src/main/java/com/pgsa/trailers/dto/CustomEnumDto.java
package com.pgsa.trailers.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class CustomEnumDto {
    private Long id;
    private String enumType;
    private String value;
    private String displayName;
    private String description;
    private String icon;
    private String color;
    private Boolean isSystem;
    private Boolean isActive;
    private Integer sortOrder;
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>(); // Fix: Set default to empty Map
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
