package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.inventory.DriverIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverIssueRepository extends JpaRepository<DriverIssue, Long> {
    List<DriverIssue> findByDriverIdOrderByIssueDateDesc(Long driverId);
    List<DriverIssue> findAllByOrderByIssueDateDesc();
}
