// src/main/java/com/pgsa/trailers/dto/CreateCustomEnumRequest.java
package com.pgsa.trailers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class CreateCustomEnumRequest {
    
    @NotBlank(message = "Enum type is required")
    @Size(max = 50, message = "Enum type must be less than 50 characters")
    @Pattern(regexp = "^[a-zA-Z_]+$", message = "Enum type can only contain letters and underscores")
    private String enumType;

    @NotBlank(message = "Value is required")
    @Size(max = 100, message = "Value must be less than 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_ ]+$", message = "Value can only contain letters, numbers, underscores, and spaces")
    private String value;

    @NotBlank(message = "Display name is required")
    @Size(max = 100, message = "Display name must be less than 100 characters")
    private String displayName;

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;

    @Size(max = 50, message = "Icon must be less than 50 characters")
    private String icon;

    @Size(max = 20, message = "Color must be less than 20 characters")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$|^[a-zA-Z]+$", message = "Color must be a hex code or valid color name")
    private String color;

    private Integer sortOrder;

    private Map<String, Object> metadata;
}
