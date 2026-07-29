// src/main/java/com/pgsa/trailers/dto/SwapItemRequestDTO.java
package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwapItemRequestDTO {
    
    // The item being returned (damaged/faulty)
    private Long oldItemId;
    
    // The new item being issued (replacement)
    private Long newItemId;
    
    // Quantity of the new item to issue
    private Integer newQuantity;
    
    // Quantity of the old item being returned (optional, defaults to all)
    private BigDecimal returnQuantity;
    
    // Condition of the damaged item: "DAMAGED", "FAULTY", "BROKEN", "WORN"
    private String damagedCondition;
    
    // Description of the damage or fault
    private String damageNotes;
    
    // The issue type: "vehicle" or "driver"
    private String issueType;
}
