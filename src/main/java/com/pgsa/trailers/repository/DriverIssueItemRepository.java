package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.inventory.DriverIssueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverIssueItemRepository extends JpaRepository<DriverIssueItem, Long> {
    List<DriverIssueItem> findByIssueId(Long issueId);
    Optional<DriverIssueItem> findByIssueIdAndItemId(Long issueId, Long itemId);
}
