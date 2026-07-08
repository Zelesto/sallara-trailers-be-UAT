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

    List<CustomEnum> findByEnumTypeAndIsActiveTrue(String enumType);

    Optional<CustomEnum> findByEnumTypeAndValueIgnoreCase(String enumType, String value);

    @Query("SELECT c FROM CustomEnum c WHERE c.enumType = :enumType AND c.isActive = true ORDER BY c.sortOrder ASC, c.displayName ASC")
    List<CustomEnum> findActiveByEnumTypeOrderBySortOrder(@Param("enumType") String enumType);

    @Query("SELECT c FROM CustomEnum c WHERE c.enumType = :enumType")
    Page<CustomEnum> findByEnumType(@Param("enumType") String enumType, Pageable pageable);

    @Query("SELECT DISTINCT c.enumType FROM CustomEnum c")
    List<String> findDistinctEnumTypes();

    long countByEnumType(String enumType);

    boolean existsByEnumTypeAndValueIgnoreCase(String enumType, String value);

    @Query("SELECT c FROM CustomEnum c ORDER BY c.sortOrder ASC, c.displayName ASC")
    List<CustomEnum> findAllOrderBySortOrder();

    @Query("SELECT COUNT(c) FROM CustomEnum c WHERE c.isActive = true")
    long countByIsActiveTrue();

    @Query("SELECT COUNT(c) FROM CustomEnum c WHERE c.isSystem = true")
    long countByIsSystemTrue();
}
