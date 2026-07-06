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

        // Update all fields - only if not null
        updateField(dto.getRegistrationNumber(), vehicle::setRegistrationNumber);
        updateField(dto.getVin(), vehicle::setVin);
        updateField(dto.getMake(), vehicle::setMake);
        updateField(dto.getModel(), vehicle::setModel);
        updateField(dto.getYear(), vehicle::setYear);
        updateField(dto.getFuelType(), vehicle::setFuelType);
        updateField(dto.getCurrentMileage(), vehicle::setCurrentMileage);
        updateField(dto.getStatus(), val -> vehicle.setStatus(VehicleStatus.valueOf(val)));
        updateField(dto.getCreatedBy(), vehicle::setCreatedBy);
        updateField(dto.getUpdatedBy(), vehicle::setUpdatedBy);
        updateField(dto.getAvgConsumption(), vehicle::setAvgConsumption);
        updateField(dto.getCurrentOdometer(), vehicle::setCurrentOdometer);
        updateField(dto.getLastServiceDate(), vehicle::setLastServiceDate);
        updateField(dto.getLastServiceOdometer(), vehicle::setLastServiceOdometer);
        updateField(dto.getServiceIntervalDays(), vehicle::setServiceIntervalDays);
        updateField(dto.getServiceIntervalKm(), vehicle::setServiceIntervalKm);
        updateField(dto.getInsurancePolicyNumber(), vehicle::setInsurancePolicyNumber);
        updateField(dto.getInsuranceExpiry(), vehicle::setInsuranceExpiry);
        updateField(dto.getRoadworthyExpiry(), vehicle::setRoadworthyExpiry);
        updateField(dto.getFleetNumber(), vehicle::setFleetNumber);
        updateField(dto.getGpsTrackerId(), vehicle::setGpsTrackerId);
        updateField(dto.getMaintenanceStatus(), vehicle::setMaintenanceStatus);
        updateField(dto.getNextServiceDue(), vehicle::setNextServiceDue);
        updateField(dto.getNextServiceOdometer(), vehicle::setNextServiceOdometer);
        updateField(dto.getIncidentsLogged(), vehicle::setIncidentsLogged);
        updateField(dto.getNotes(), vehicle::setNotes);
        updateField(dto.getAuditTrail(), vehicle::setAuditTrail);
        updateField(dto.getCategory(), vehicle::setCategory);
        
        if (dto.getVehicleType() != null) {
            vehicle.setVehicleType(VehicleType.valueOf(dto.getVehicleType()));
        }
        
        updateField(dto.getIsActive(), vehicle::setIsActive);
        updateField(dto.getVersion(), vehicle::setVersion);
        updateField(dto.getCurrentValue(), vehicle::setCurrentValue);
        updateField(dto.getPurchaseDate(), vehicle::setPurchaseDate);
        updateField(dto.getPurchasePrice(), vehicle::setPurchasePrice);
        updateField(dto.getMaintenanceCost(), vehicle::setMaintenanceCost);
        updateField(dto.getLastMaintenanceDate(), vehicle::setLastMaintenanceDate);
        updateField(dto.getNextMaintenanceDue(), vehicle::setNextMaintenanceDue);
        updateField(dto.getFuelEfficiency(), vehicle::setFuelEfficiency);
        updateField(dto.getInsuranceProvider(), vehicle::setInsuranceProvider);
        updateField(dto.getInsuranceExpiryDate(), vehicle::setInsuranceExpiryDate);

        // Handle driver assignment
        if (dto.getAssignedDriverId() != null) {
            if (dto.getAssignedDriverId() > 0) {
                Driver driver = driverRepository.findById(dto.getAssignedDriverId())
                    .orElseThrow(() -> new RuntimeException("Driver not found with id: " + dto.getAssignedDriverId()));
                vehicle.setAssignedDriver(driver);
            } else {
                vehicle.setAssignedDriver(null);
            }
        }

        // Update timestamp
        vehicle.setUpdatedAt(LocalDateTime.now());
        
        // Recalculate next service
        vehicle.calculateNextService();

        return vehicleRepository.save(vehicle);
    }

    // Helper method to update field if value is not null
    private <T> void updateField(T value, java.util.function.Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    @Transactional
    public void deleteVehicle(Long id) {
        log.info("Deleting vehicle ID: {}", id);
        Vehicle vehicle = getVehicleById(id);
        vehicleRepository.delete(vehicle);
    }

    public List<Vehicle> getVehiclesByStatus(String status) {
        log.debug("Fetching vehicles by status: {}", status);
        return vehicleRepository.findByStatus(status);
    }

    public List<Vehicle> getActiveVehicles() {
        log.debug("Fetching active vehicles");
        return vehicleRepository.findByStatusIn(List.of("ACTIVE", "AVAILABLE"));
    }
}
