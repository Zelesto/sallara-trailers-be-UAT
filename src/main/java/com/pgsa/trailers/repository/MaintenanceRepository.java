// src/main/java/com/pgsa/trailers/repository/MaintenanceRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.vehicle.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceRepository extends JpaRepository<MaintenanceRecord, Long> {
    List<MaintenanceRecord> findByVehicleId(Long vehicleId);
    List<MaintenanceRecord> findByVehicleIdOrderByDateDesc(Long vehicleId);
}
