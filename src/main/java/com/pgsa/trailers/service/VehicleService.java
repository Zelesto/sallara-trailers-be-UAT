package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.VehicleDTO;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.enums.VehicleStatus;
import com.pgsa.trailers.enums.VehicleType;
import com.pgsa.trailers.repository.DriverRepository;
import com.pgsa.trailers.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    // ====== Query Methods ======
    
    public List<Vehicle> getAllVehicles() {
        log.debug("Fetching all vehicles");
        return vehicleRepository.findAll();
    }

    public List<Vehicle> getAllActiveVehicles() {
        log.debug("Fetching all active vehicles");
        return vehicleRepository.findByIsActiveTrue();
    }

    public Vehicle getVehicleById(Long id) {
        log.debug("Fetching vehicle by ID: {}", id);
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with ID: " + id));
    }

    public Vehicle getActiveVehicleById(Long id) {
        log.debug("Fetching active vehicle by ID: {}", id);
        return vehicleRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Active vehicle not found with ID: " + id));
    }

    public Vehicle getVehicleByRegistration(String registrationNumber) {
        log.debug("Fetching vehicle by registration: {}", registrationNumber);
        return vehicleRepository.findByRegistrationNumber(registrationNumber)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with registration: " + registrationNumber));
    }

    public List<Vehicle> getVehiclesByStatus(VehicleStatus status) {
        log.debug("Fetching vehicles by status: {}", status);
        return vehicleRepository.findByStatus(status);
    }

    public List<Vehicle> getActiveVehicles() {
        log.debug("Fetching active vehicles");
        return vehicleRepository.findByStatusIn(List.of(VehicleStatus.ACTIVE, VehicleStatus.AVAILABLE));
    }

    public List<Vehicle> searchVehicles(String searchTerm) {
        log.debug("Searching vehicles by: {}", searchTerm);
        return vehicleRepository.searchVehicles(searchTerm);
    }

    public List<Vehicle> getVehiclesByDriver(Long driverId) {
        log.debug("Fetching vehicles for driver: {}", driverId);
        return vehicleRepository.findByAssignedDriverId(driverId);
    }

    public List<Vehicle> getAvailableVehicles() {
        log.debug("Fetching available vehicles");
        return vehicleRepository.findByAssignedDriverIsNullAndStatusIn(
            List.of(VehicleStatus.AVAILABLE, VehicleStatus.ACTIVE)
        );
    }

    public List<Vehicle> getVehiclesWithUpcomingService() {
        log.debug("Fetching vehicles with upcoming service");
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysFromNow = today.plusDays(30);
        return vehicleRepository.findByNextServiceDueBetween(today, thirtyDaysFromNow);
    }

    public List<Vehicle> getVehiclesOverdueForService() {
        log.debug("Fetching vehicles overdue for service");
        return vehicleRepository.findByNextServiceDueBefore(LocalDate.now());
    }

    public List<Vehicle> getVehiclesWithExpiredInsurance() {
        log.debug("Fetching vehicles with expired insurance");
        return vehicleRepository.findByInsuranceExpiryBefore(LocalDate.now());
    }

    public List<Vehicle> getVehiclesWithExpiredRoadworthy() {
        log.debug("Fetching vehicles with expired roadworthy");
        return vehicleRepository.findByRoadworthyExpiryBefore(LocalDate.now());
    }

    // ====== Create Methods ======
    
    @Transactional
    public Vehicle createVehicle(Vehicle vehicle) {
        log.info("Creating vehicle with registration: {}", vehicle.getRegistrationNumber());

        // Check for duplicates
        if (vehicleRepository.findByRegistrationNumber(vehicle.getRegistrationNumber()).isPresent()) {
            throw new RuntimeException("Vehicle with registration " + vehicle.getRegistrationNumber() + " already exists");
        }
        if (vehicle.getVin() != null && vehicleRepository.findByVin(vehicle.getVin()).isPresent()) {
            throw new RuntimeException("Vehicle with VIN " + vehicle.getVin() + " already exists");
        }
        if (vehicle.getFleetNumber() != null && vehicleRepository.findByFleetNumber(vehicle.getFleetNumber()).isPresent()) {
            throw new RuntimeException("Vehicle with fleet number " + vehicle.getFleetNumber() + " already exists");
        }

        // Ensure defaults are set
        if (vehicle.getStatus() == null) {
            vehicle.setStatus(VehicleStatus.ACTIVE);
        }
        if (vehicle.getIsActive() == null) {
            vehicle.setIsActive(true);
        }
        if (vehicle.getVersion() == null) {
            vehicle.setVersion(0);
        }
        if (vehicle.getIncidentsLogged() == null) {
            vehicle.setIncidentsLogged(0);
        }
        
        vehicle.calculateNextService();
        
        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("✅ Successfully created vehicle with ID: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Vehicle createVehicleFromDTO(VehicleDTO dto) {
        log.info("Creating vehicle from DTO with registration: {}", dto.getRegistrationNumber());
        
        Vehicle vehicle = new Vehicle();
        mapDtoToEntity(dto, vehicle);
        
        // Check for duplicates
        if (vehicleRepository.findByRegistrationNumber(vehicle.getRegistrationNumber()).isPresent()) {
            throw new RuntimeException("Vehicle with registration " + vehicle.getRegistrationNumber() + " already exists");
        }
        if (vehicle.getVin() != null && vehicleRepository.findByVin(vehicle.getVin()).isPresent()) {
            throw new RuntimeException("Vehicle with VIN " + vehicle.getVin() + " already exists");
        }
        if (vehicle.getFleetNumber() != null && vehicleRepository.findByFleetNumber(vehicle.getFleetNumber()).isPresent()) {
            throw new RuntimeException("Vehicle with fleet number " + vehicle.getFleetNumber() + " already exists");
        }
        
        vehicle.calculateNextService();
        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("✅ Successfully created vehicle from DTO with ID: {}", saved.getId());
        return saved;
    }

    // ====== Update Methods ======
    
    @Transactional
    public Vehicle updateVehicle(Long id, VehicleDTO dto) {
        log.info("Updating vehicle ID: {}", id);
        
        Vehicle vehicle = getVehicleById(id);
        
        // Map DTO to Entity
        mapDtoToEntity(dto, vehicle);
        
        // Recalculate service
        vehicle.calculateNextService();

        log.info("Saving vehicle - registration: {}, make: {}, model: {}, vehicleType: {}, status: {}", 
            vehicle.getRegistrationNumber(), 
            vehicle.getMake(), 
            vehicle.getModel(), 
            vehicle.getVehicleType(), 
            vehicle.getStatus());

        try {
            Vehicle saved = vehicleRepository.save(vehicle);
            log.info("✅ Successfully updated vehicle ID: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("❌ Failed to update vehicle: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update vehicle: " + e.getMessage(), e);
        }
    }

    // ====== Delete Methods ======
    
    @Transactional
    public void deleteVehicle(Long id) {
        log.info("Deleting vehicle ID: {}", id);
        Vehicle vehicle = getVehicleById(id);
        vehicleRepository.delete(vehicle);
        log.info("✅ Successfully deleted vehicle ID: {}", id);
    }

    @Transactional
    public void softDeleteVehicle(Long id) {
        log.info("Soft deleting vehicle ID: {}", id);
        Vehicle vehicle = getVehicleById(id);
        vehicle.softDelete();
        vehicle.setStatus(VehicleStatus.INACTIVE);
        vehicleRepository.save(vehicle);
        log.info("✅ Successfully soft deleted vehicle ID: {}", id);
    }

    @Transactional
    public void restoreVehicle(Long id) {
        log.info("Restoring vehicle ID: {}", id);
        Vehicle vehicle = getVehicleById(id);
        vehicle.restore();
        if (vehicle.getStatus() == VehicleStatus.INACTIVE) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
        }
        vehicleRepository.save(vehicle);
        log.info("✅ Successfully restored vehicle ID: {}", id);
    }

    // ====== Business Operations ======
    
    @Transactional
    public void assignDriverToVehicle(Long vehicleId, Long driverId) {
        log.info("Assigning driver {} to vehicle {}", driverId, vehicleId);
        Vehicle vehicle = getActiveVehicleById(vehicleId);
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + driverId));
        
        if (!driver.isActive()) {
            throw new RuntimeException("Driver is not active");
        }
        
        vehicle.assignDriver(driver);
        vehicleRepository.save(vehicle);
        log.info("✅ Driver {} assigned to vehicle {}", driverId, vehicleId);
    }

    @Transactional
    public void unassignDriverFromVehicle(Long vehicleId) {
        log.info("Unassigning driver from vehicle {}", vehicleId);
        Vehicle vehicle = getActiveVehicleById(vehicleId);
        vehicle.unassignDriver();
        vehicleRepository.save(vehicle);
        log.info("✅ Driver unassigned from vehicle {}", vehicleId);
    }

    @Transactional
    public void updateOdometer(Long vehicleId, BigDecimal newOdometer) {
        log.info("Updating odometer for vehicle {} to {}", vehicleId, newOdometer);
        Vehicle vehicle = getActiveVehicleById(vehicleId);
        vehicle.updateOdometer(newOdometer);
        vehicleRepository.save(vehicle);
        log.info("✅ Odometer updated for vehicle {}", vehicleId);
    }

    // ====== Helper Methods ======
    
    private void mapDtoToEntity(VehicleDTO dto, Vehicle vehicle) {
        // Basic fields
        if (dto.getRegistrationNumber() != null) {
            vehicle.setRegistrationNumber(dto.getRegistrationNumber().trim());
        }
        if (dto.getVin() != null) {
            vehicle.setVin(dto.getVin().trim());
        }
        if (dto.getMake() != null) {
            vehicle.setMake(dto.getMake().trim());
        }
        if (dto.getModel() != null) {
            vehicle.setModel(dto.getModel().trim());
        }
        if (dto.getYear() != null) {
            vehicle.setYear(dto.getYear());
        }
        if (dto.getFuelType() != null) {
            vehicle.setFuelType(dto.getFuelType().trim());
        }
        if (dto.getCurrentMileage() != null) {
            vehicle.setCurrentMileage(dto.getCurrentMileage());
        }
        
        // Handle status enum - always set if provided
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            try {
                vehicle.setStatus(VehicleStatus.valueOf(dto.getStatus().toUpperCase()));
                log.info("✅ Set status to: {}", vehicle.getStatus());
            } catch (IllegalArgumentException e) {
                log.warn("❌ Invalid status: {}, keeping current: {}", dto.getStatus(), vehicle.getStatus());
                if (vehicle.getStatus() == null) {
                    vehicle.setStatus(VehicleStatus.ACTIVE);
                }
            }
        } else if (vehicle.getStatus() == null) {
            // Set default if null
            vehicle.setStatus(VehicleStatus.ACTIVE);
        }
        
        // Handle vehicle type enum - always set if provided
        if (dto.getVehicleType() != null && !dto.getVehicleType().isEmpty()) {
            try {
                String vehicleTypeStr = dto.getVehicleType().trim().toUpperCase();
                vehicle.setVehicleType(VehicleType.valueOf(vehicleTypeStr));
                log.info("✅ Set vehicle type to: {}", vehicle.getVehicleType());
            } catch (IllegalArgumentException e) {
                log.error("❌ Invalid vehicle type: '{}', using default TRUCK", dto.getVehicleType());
                vehicle.setVehicleType(VehicleType.TRUCK);
            }
        } else if (vehicle.getVehicleType() == null) {
            // Set default if null
            vehicle.setVehicleType(VehicleType.TRUCK);
        }
        
        // Numeric and date fields
        if (dto.getAvgConsumption() != null) {
            vehicle.setAvgConsumption(dto.getAvgConsumption());
        }
        if (dto.getCurrentOdometer() != null) {
            vehicle.setCurrentOdometer(dto.getCurrentOdometer());
        }
        if (dto.getLastServiceDate() != null) {
            vehicle.setLastServiceDate(dto.getLastServiceDate());
        }
        if (dto.getLastServiceOdometer() != null) {
            vehicle.setLastServiceOdometer(dto.getLastServiceOdometer());
        }
        if (dto.getServiceIntervalDays() != null) {
            vehicle.setServiceIntervalDays(dto.getServiceIntervalDays());
        }
        if (dto.getServiceIntervalKm() != null) {
            vehicle.setServiceIntervalKm(dto.getServiceIntervalKm());
        }
        if (dto.getInsurancePolicyNumber() != null) {
            vehicle.setInsurancePolicyNumber(dto.getInsurancePolicyNumber().trim());
        }
        if (dto.getInsuranceExpiry() != null) {
            vehicle.setInsuranceExpiry(dto.getInsuranceExpiry());
        }
        if (dto.getRoadworthyExpiry() != null) {
            vehicle.setRoadworthyExpiry(dto.getRoadworthyExpiry());
        }
        if (dto.getFleetNumber() != null) {
            vehicle.setFleetNumber(dto.getFleetNumber().trim());
        }
        if (dto.getGpsTrackerId() != null) {
            vehicle.setGpsTrackerId(dto.getGpsTrackerId());
        }
        if (dto.getMaintenanceStatus() != null) {
            vehicle.setMaintenanceStatus(dto.getMaintenanceStatus().trim());
        }
        if (dto.getNextServiceDue() != null) {
            vehicle.setNextServiceDue(dto.getNextServiceDue());
        }
        if (dto.getNextServiceOdometer() != null) {
            vehicle.setNextServiceOdometer(dto.getNextServiceOdometer());
        }
        if (dto.getIncidentsLogged() != null) {
            vehicle.setIncidentsLogged(dto.getIncidentsLogged());
        }
        if (dto.getNotes() != null) {
            vehicle.setNotes(dto.getNotes().trim());
        }
        if (dto.getAuditTrail() != null) {
            vehicle.setAuditTrail(dto.getAuditTrail());
        }
        if (dto.getCategory() != null) {
            vehicle.setCategory(dto.getCategory().trim());
        }
        if (dto.getIsActive() != null) {
            vehicle.setIsActive(dto.getIsActive());
        }
        if (dto.getVersion() != null) {
            vehicle.setVersion(dto.getVersion());
        }
        if (dto.getCurrentValue() != null) {
            vehicle.setCurrentValue(dto.getCurrentValue());
        }
        if (dto.getPurchaseDate() != null) {
            vehicle.setPurchaseDate(dto.getPurchaseDate());
        }
        if (dto.getPurchasePrice() != null) {
            vehicle.setPurchasePrice(dto.getPurchasePrice());
        }
        if (dto.getMaintenanceCost() != null) {
            vehicle.setMaintenanceCost(dto.getMaintenanceCost());
        }
        if (dto.getLastMaintenanceDate() != null) {
            vehicle.setLastMaintenanceDate(dto.getLastMaintenanceDate());
        }
        if (dto.getNextMaintenanceDue() != null) {
            vehicle.setNextMaintenanceDue(dto.getNextMaintenanceDue());
        }
        if (dto.getFuelEfficiency() != null) {
            vehicle.setFuelEfficiency(dto.getFuelEfficiency());
        }
        if (dto.getInsuranceProvider() != null) {
            vehicle.setInsuranceProvider(dto.getInsuranceProvider().trim());
        }
        if (dto.getInsuranceExpiryDate() != null) {
            vehicle.setInsuranceExpiryDate(dto.getInsuranceExpiryDate());
        }

        // Handle driver assignment
        if (dto.getAssignedDriverId() != null) {
            if (dto.getAssignedDriverId() > 0) {
                Driver driver = driverRepository.findById(dto.getAssignedDriverId())
                    .orElseThrow(() -> new RuntimeException("Driver not found with id: " + dto.getAssignedDriverId()));
                vehicle.setAssignedDriver(driver);
                log.info("Assigned driver: {}", driver.getId());
            } else {
                vehicle.setAssignedDriver(null);
                log.info("Unassigned driver");
            }
        }
    }
}
