// src/main/java/com/pgsa/trailers/entity/enums/CustomEnum.java
package com.pgsa.trailers.entity.enums;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "custom_enums", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "enum_type", "value"}))
@EntityListeners(AuditingEntityListener.class)
public class CustomEnum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enum_type", nullable = false, length = 50)
    private String enumType; // e.g., "INVENTORY_TYPE", "FUEL_STATION", "PAYMENT_TYPE"

    @Column(name = "value", nullable = false, length = 100)
    private String value;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false; // True for built-in enums

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "tenant_id")
    private Long tenantId; // For multi-tenant support

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata; // Additional JSON data

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
