// src/main/java/com/pgsa/trailers/dto/CreateCustomEnumRequest.java
package com.pgsa.trailers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCustomEnumRequest {
    
    @NotBlank(message = "Enum type is required")
    @Size(max = 50, message = "Enum type must be less than 50 characters")
    private String enumType;

    @NotBlank(message = "Value is required")
    @Size(max = 100, message = "Value must be less than 100 characters")
    private String value;

    @NotBlank(message = "Display name is required")
    @Size(max = 100, message = "Display name must be less than 100 characters")
    private String displayName;

    @Size(max = 255, message = "Description must be less than 255 characters")
    private String description;

    @Size(max = 50, message = "Icon must be less than 50 characters")
    private String icon;

    @Size(max = 20, message = "Color must be less than 20 characters")
    private String color;

    private Integer sortOrder;

    private String metadata;
}
