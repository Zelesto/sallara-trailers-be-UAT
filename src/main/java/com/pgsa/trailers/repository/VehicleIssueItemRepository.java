// src/main/java/com/pgsa/trailers/repository/inventory/VehicleIssueItemRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.inventory.VehicleIssueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleIssueItemRepository extends JpaRepository<VehicleIssueItem, Long> {
    List<VehicleIssueItem> findByIssueId(Long issueId);
    Optional<VehicleIssueItem> findByIssueIdAndItemId(Long issueId, Long itemId);
}
