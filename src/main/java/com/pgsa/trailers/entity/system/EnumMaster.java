// src/main/java/com/pgsa/trailers/entity/system/EnumMaster.java
package com.pgsa.trailers.entity.system;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "enum_master", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_enum_module_category_code", 
                            columnNames = {"module_name", "category", "code"})
       })
public class EnumMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module_name", nullable = false, length = 50)
    private String moduleName;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // ⭐ NEW: System vs Custom
    @Column(name = "is_system")
    private Boolean isSystem = false;

    // ⭐ NEW: Can users edit this enum?
    @Column(name = "is_editable")
    private Boolean isEditable = true;

    // ⭐ NEW: Can users delete this enum?
    @Column(name = "is_deletable")
    private Boolean isDeletable = true;

    @Column(name = "color_code", length = 7)
    private String colorCode;

    @Column(name = "icon_name", length = 50)
    private String iconName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (sortOrder == null) sortOrder = 0;
        if (isDefault == null) isDefault = false;
        if (isActive == null) isActive = true;
        if (isSystem == null) isSystem = false;
        if (isEditable == null) isEditable = true;
        if (isDeletable == null) isDeletable = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
