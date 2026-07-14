// src/main/java/com/pgsa/trailers/repository/DepotRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.ops.Depot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepotRepository extends JpaRepository<Depot, Long> {

    /**
     * Find depot by depot code
     */
    Optional<Depot> findByDepotCode(String depotCode);

    /**
     * Check if depot code exists
     */
    boolean existsByDepotCode(String depotCode);

    /**
     * Find all depots ordered by name
     */
    List<Depot> findAllByOrderByNameAsc();

    /**
     * Find active depots ordered by name
     */
    List<Depot> findByIsActiveTrueOrderByNameAsc();

    /**
     * Find depots by city
     */
    List<Depot> findByCityContainingIgnoreCase(String city);

    /**
     * Find depots by province
     */
    List<Depot> findByProvince(String province);

    /**
     * Find depots with coordinates (for distance calculation)
     */
    List<Depot> findByLatitudeIsNotNullAndLongitudeIsNotNull();
}
