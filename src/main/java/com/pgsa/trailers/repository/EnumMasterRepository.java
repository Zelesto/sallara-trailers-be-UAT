// src/main/java/com/pgsa/trailers/repository/EnumMasterRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.system.EnumMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnumMasterRepository extends JpaRepository<EnumMaster, Long> {

    // ============================================================
    // STANDARD QUERIES
    // ============================================================
    
    List<EnumMaster> findByModuleNameAndCategoryAndIsActiveTrueOrderBySortOrder(
        String moduleName, String category
    );

    List<EnumMaster> findByModuleNameAndIsActiveTrue(String moduleName);

    Optional<EnumMaster> findByModuleNameAndCategoryAndCode(
        String moduleName, String category, String code
    );

    Optional<EnumMaster> findByModuleNameAndCategoryAndIsDefaultTrue(
        String moduleName, String category
    );

    // ============================================================
    // SYSTEM ENUM QUERIES
    // ============================================================
    
    List<EnumMaster> findByModuleNameAndCategoryAndIsSystemTrueAndIsActiveTrueOrderBySortOrder(
        String moduleName, String category
    );

    // ============================================================
    // CUSTOM ENUM QUERIES
    // ============================================================
    
    List<EnumMaster> findByModuleNameAndCategoryAndIsSystemFalseAndIsActiveTrueOrderBySortOrder(
        String moduleName, String category
    );

    // ============================================================
    // ADMIN QUERIES
    // ============================================================
    
    List<EnumMaster> findByModuleName(String moduleName);

    boolean existsByModuleNameAndCategoryAndCode(
        String moduleName, String category, String code
    );
}
