// src/main/java/com/pgsa/trailers/dto/inventory/InventoryLocationResponseDTO.java
package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLocationResponseDTO {
    private Long id;
    private String name;
    private String type;
}
