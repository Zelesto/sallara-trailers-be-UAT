// src/main/java/com/pgsa/trailers/dto/CustomEnumDto.java
package com.pgsa.trailers.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

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
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

