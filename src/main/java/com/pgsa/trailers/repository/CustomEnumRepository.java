// src/main/java/com/pgsa/trailers/repository/CustomEnumRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.enums.CustomEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomEnumRepository extends JpaRepository<CustomEnum, Long> {

    List<CustomEnum> findByEnumTypeAndTenantIdAndIsActiveTrue(String enumType, Long tenantId);

    Optional<CustomEnum> findByEnumTypeAndTenantIdAndValueIgnoreCase(
            String enumType, Long tenantId, String value);

    @Query("SELECT c FROM CustomEnum c WHERE c.enumType = :enumType AND c.tenantId = :tenantId")
    Page<CustomEnum> findByEnumTypeAndTenantId(@Param("enumType") String enumType,
                                                 @Param("tenantId") Long tenantId,
                                                 Pageable pageable);

    List<CustomEnum> findByEnumTypeAndTenantIdOrderBySortOrderAsc(String enumType, Long tenantId);

    @Query("SELECT DISTINCT c.enumType FROM CustomEnum c WHERE c.tenantId = :tenantId")
    List<String> findDistinctEnumTypesByTenantId(@Param("tenantId") Long tenantId);

    long countByEnumTypeAndTenantId(String enumType, Long tenantId);

    boolean existsByEnumTypeAndTenantIdAndValueIgnoreCase(
            String enumType, Long tenantId, String value);
}
