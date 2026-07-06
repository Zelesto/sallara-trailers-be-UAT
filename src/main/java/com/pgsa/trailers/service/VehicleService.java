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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public List<Vehicle> getAllVehicles() {
        log.debug("Fetching all vehicles");
        return vehicleRepository.findAll();
    }

    public Vehicle getVehicleById(Long id) {
        log.debug("Fetching vehicle by ID: {}", id);
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with ID: " + id));
    }

    public Vehicle getVehicleByRegistration(String registrationNumber) {
        log.debug("Fetching vehicle by registration: {}", registrationNumber);
        return vehicleRepository.findByRegistrationNumber(registrationNumber)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with registration: " + registrationNumber));
    }

    public List<Vehicle> getVehiclesByStatus(String status) {
        log.debug("Fetching vehicles by status: {}", status);
        return vehicleRepository.findByStatus(status);
    }

    public List<Vehicle> getActiveVehicles() {
        log.debug("Fetching active vehicles");
        return vehicleRepository.findByStatusIn(List.of("ACTIVE", "AVAILABLE"));
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
        return vehicleRepository.findByAssignedDriverIsNull();
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

    @Transactional
    public Vehicle createVehicle(Vehicle vehicle) {
        log.info("Creating vehicle with registration: {}", vehicle.getRegistrationNumber());

        if (vehicleRepository.findByRegistrationNumber(vehicle.getRegistrationNumber()).isPresent()) {
            throw new RuntimeException("Vehicle with registration " + vehicle.getRegistrationNumber() + " already exists");
        }

        vehicle.setCreatedAt(LocalDateTime.now());
        vehicle.setUpdatedAt(LocalDateTime.now());
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle updateVehicle(Long id, VehicleDTO dto) {
        log.info("Updating vehicle ID: {} with DTO: {}", id, dto);
        
        Vehicle vehicle = getVehicleById(id);
        
        log.info("DTO fields - registrationNumber: {}, make: {}, model: {}, vehicleType: {}, fuelType: {}, status: {}, year: {}, vin: {}", 
            dto.getRegistrationNumber(), dto.getMake(), dto.getModel(), 
            dto.getVehicleType(), dto.getFuelType(), dto.getStatus(), 
            dto.getYear(), dto.getVin());

        // Update basic fields
        if (dto.getRegistrationNumber() != null) {
            vehicle.setRegistrationNumber(dto.getRegistrationNumber());
        }
        if (dto.getVin() != null) {
            vehicle.setVin(dto.getVin());
        }
        if (dto.getMake() != null) {
            vehicle.setMake(dto.getMake());
        }
        if (dto.getModel() != null) {
            vehicle.setModel(dto.getModel());
        }
        if (dto.getYear() != null) {
            vehicle.setYear(dto.getYear());
        }
        if (dto.getFuelType() != null) {
            vehicle.setFuelType(dto.getFuelType());
        }
        if (dto.getCurrentMileage() != null) {
            vehicle.setCurrentMileage(dto.getCurrentMileage());
        }
        
        // Handle status enum
        if (dto.getStatus() != null) {
            try {
                vehicle.setStatus(VehicleStatus.valueOf(dto.getStatus().toUpperCase()));
                log.info("✅ Set status to: {}", vehicle.getStatus());
            } catch (IllegalArgumentException e) {
                log.warn("❌ Invalid status: {}, keeping current: {}", dto.getStatus(), vehicle.getStatus());
            }
        }
        
        // Handle vehicle type enum
        if (dto.getVehicleType() != null && !dto.getVehicleType().isEmpty()) {
            try {
                String vehicleTypeStr = dto.getVehicleType().trim().toUpperCase();
                VehicleType vehicleType = VehicleType.valueOf(vehicleTypeStr);
                vehicle.setVehicleType(vehicleType);
                log.info("✅ Set vehicle type to: {}", vehicleType);
            } catch (IllegalArgumentException e) {
                log.error("❌ Invalid vehicle type: '{}'", dto.getVehicleType());
            }
        }
        
        // Update other fields
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
            vehicle.setInsurancePolicyNumber(dto.getInsurancePolicyNumber());
        }
        if (dto.getInsuranceExpiry() != null) {
            vehicle.setInsuranceExpiry(dto.getInsuranceExpiry());
        }
        if (dto.getRoadworthyExpiry() != null) {
            vehicle.setRoadworthyExpiry(dto.getRoadworthyExpiry());
        }
        if (dto.getFleetNumber() != null) {
            vehicle.setFleetNumber(dto.getFleetNumber());
        }
        if (dto.getGpsTrackerId() != null) {
            vehicle.setGpsTrackerId(dto.getGpsTrackerId());
        }
        if (dto.getMaintenanceStatus() != null) {
            vehicle.setMaintenanceStatus(dto.getMaintenanceStatus());
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
            vehicle.setNotes(dto.getNotes());
        }
        if (dto.getAuditTrail() != null) {
            vehicle.setAuditTrail(dto.getAuditTrail());
        }
        if (dto.getCategory() != null) {
            vehicle.setCategory(dto.getCategory());
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
            vehicle.setInsuranceProvider(dto.getInsuranceProvider());
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

        // Update timestamp and recalculate
        vehicle.setUpdatedAt(LocalDateTime.now());
        vehicle.calculateNextService();

        log.info("Saving vehicle with values - registration: {}, make: {}, model: {}, vehicleType: {}, status: {}", 
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

    @Transactional
    public void deleteVehicle(Long id) {
        log.info("Deleting vehicle ID: {}", id);
        Vehicle vehicle = getVehicleById(id);
        vehicleRepository.delete(vehicle);
    }
}
