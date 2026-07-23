// src/main/java/com/pgsa/trailers/service/EnumService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.system.EnumMaster;
import com.pgsa.trailers.repository.EnumMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnumService {

    private final EnumMasterRepository enumRepository;

    // ============================================================
    // READ OPERATIONS
    // ============================================================

    @Cacheable(value = "enums", key = "#moduleName + ':' + #category")
    @Transactional(readOnly = true)
    public List<EnumMaster> getEnums(String moduleName, String category) {
        log.info("Fetching enums for module: {}, category: {}", moduleName, category);
        return enumRepository.findByModuleNameAndCategoryAndIsActiveTrueOrderBySortOrder(
            moduleName, category
        );
    }

    @Cacheable(value = "enums", key = "#moduleName + ':' + #category + ':system'")
    @Transactional(readOnly = true)
    public List<EnumMaster> getSystemEnums(String moduleName, String category) {
        log.info("Fetching system enums for module: {}, category: {}", moduleName, category);
        return enumRepository.findByModuleNameAndCategoryAndIsSystemTrueAndIsActiveTrueOrderBySortOrder(
            moduleName, category
        );
    }

    @Cacheable(value = "enums", key = "#moduleName + ':' + #category + ':custom'")
    @Transactional(readOnly = true)
    public List<EnumMaster> getCustomEnums(String moduleName, String category) {
        log.info("Fetching custom enums for module: {}, category: {}", moduleName, category);
        return enumRepository.findByModuleNameAndCategoryAndIsSystemFalseAndIsActiveTrueOrderBySortOrder(
            moduleName, category
        );
    }

    @Cacheable(value = "enums", key = "#moduleName + ':ALL'")
    @Transactional(readOnly = true)
    public Map<String, List<EnumMaster>> getEnumsByModule(String moduleName) {
        return enumRepository.findByModuleNameAndIsActiveTrue(moduleName)
            .stream()
            .collect(Collectors.groupingBy(EnumMaster::getCategory));
    }

    @Cacheable(value = "enumCodes", key = "#moduleName + ':' + #category + ':' + #code")
    @Transactional(readOnly = true)
    public EnumMaster getEnum(String moduleName, String category, String code) {
        return enumRepository.findByModuleNameAndCategoryAndCode(moduleName, category, code)
            .orElse(null);
    }

    @Cacheable(value = "enumDefaults", key = "#moduleName + ':' + #category")
    @Transactional(readOnly = true)
    public EnumMaster getDefaultEnum(String moduleName, String category) {
        return enumRepository.findByModuleNameAndCategoryAndIsDefaultTrue(moduleName, category)
            .orElse(null);
    }

    // ============================================================
    // CREATE OPERATIONS
    // ============================================================

    @Transactional
    public EnumMaster createEnum(EnumMaster enumMaster) {
        // Prevent creation of system enums
        if (enumMaster.getIsSystem() != null && enumMaster.getIsSystem()) {
            throw new RuntimeException("Cannot create system enums manually");
        }
        
        log.info("Creating custom enum: {} - {}", enumMaster.getModuleName(), enumMaster.getCode());
        return enumRepository.save(enumMaster);
    }

    // ============================================================
    // UPDATE OPERATIONS
    // ============================================================

    @Transactional
    @CacheEvict(value = {"enums", "enumCodes", "enumDefaults"}, allEntries = true)
    public EnumMaster updateEnum(Long id, EnumMaster enumMaster) {
        EnumMaster existing = enumRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Enum not found with ID: " + id));
        
        // Check if system enum can be modified
        if (existing.getIsSystem() && !enumMaster.getIsEditable()) {
            // System enums can only update display name, description, color, icon
            existing.setDisplayName(enumMaster.getDisplayName());
            existing.setDescription(enumMaster.getDescription());
            existing.setColorCode(enumMaster.getColorCode());
            existing.setIconName(enumMaster.getIconName());
        } else if (existing.getIsSystem()) {
            // System enums cannot be made inactive, or have their code changed
            throw new RuntimeException("System enums cannot be modified beyond display properties");
        } else {
            // Custom enums can be fully updated
            existing.setDisplayName(enumMaster.getDisplayName());
            existing.setDescription(enumMaster.getDescription());
            existing.setSortOrder(enumMaster.getSortOrder());
            existing.setIsDefault(enumMaster.getIsDefault());
            existing.setIsActive(enumMaster.getIsActive());
            existing.setColorCode(enumMaster.getColorCode());
            existing.setIconName(enumMaster.getIconName());
            existing.setMetadata(enumMaster.getMetadata());
        }
        
        existing.setUpdatedBy(enumMaster.getUpdatedBy());
        return enumRepository.save(existing);
    }

    // ============================================================
    // DELETE OPERATIONS
    // ============================================================

    @Transactional
    @CacheEvict(value = {"enums", "enumCodes", "enumDefaults"}, allEntries = true)
    public void deleteEnum(Long id) {
        EnumMaster enumMaster = enumRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Enum not found with ID: " + id));
        
        // Check if system enum can be deleted
        if (enumMaster.getIsSystem() && !enumMaster.getIsDeletable()) {
            throw new RuntimeException("System enums cannot be deleted");
        }
        
        // Check if enum is in use
        if (isEnumInUse(enumMaster)) {
            throw new RuntimeException("Enum is currently in use and cannot be deleted");
        }
        
        enumRepository.deleteById(id);
        log.info("Deleted enum: {} - {}", enumMaster.getModuleName(), enumMaster.getCode());
    }

    // ============================================================
    // VALIDATION HELPERS
    // ============================================================

    @Transactional(readOnly = true)
    public boolean isEnumInUse(EnumMaster enumMaster) {
        // This would check if any records are using this enum value
        // Implementation depends on your data model
        return false;
    }

    @Transactional(readOnly = true)
    public boolean canModify(EnumMaster enumMaster) {
        if (enumMaster.getIsSystem()) {
            return enumMaster.getIsEditable() != null && enumMaster.getIsEditable();
        }
        return true;
    }

    @Transactional(readOnly = true)
    public boolean canDelete(EnumMaster enumMaster) {
        if (enumMaster.getIsSystem()) {
            return enumMaster.getIsDeletable() != null && enumMaster.getIsDeletable();
        }
        return true;
    }
}
