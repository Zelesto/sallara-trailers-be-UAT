// src/main/java/com/pgsa/trailers/repository/VehicleFuelStatusRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.assets.VehicleFuelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleFuelStatusRepository extends JpaRepository<VehicleFuelStatus, Long> {

    Optional<VehicleFuelStatus> findByVehicleIdAndTankNumber(Long vehicleId, Integer tankNumber);

    List<VehicleFuelStatus> findByVehicleId(Long vehicleId);

    @Query("SELECT vfs FROM VehicleFuelStatus vfs WHERE vfs.percentageFull < :threshold AND vfs.status != 'CRITICAL'")
    List<VehicleFuelStatus> findVehiclesWithLowFuel(@Param("threshold") BigDecimal threshold);

    @Modifying
    @Query("UPDATE VehicleFuelStatus vfs SET vfs.currentLevel = :level, vfs.percentageFull = (:level / vfs.capacity) * 100, vfs.updatedAt = CURRENT_TIMESTAMP WHERE vfs.vehicle.id = :vehicleId AND vfs.tankNumber = :tankNumber")
    void updateFuelLevel(@Param("vehicleId") Long vehicleId, @Param("tankNumber") Integer tankNumber, @Param("level") BigDecimal level);
}
