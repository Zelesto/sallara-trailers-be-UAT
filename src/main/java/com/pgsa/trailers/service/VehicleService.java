package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    /**
     * Get all vehicles
     */
    public List<Vehicle> getAllVehicles() {
        log.debug("Fetching all vehicles");
        return vehicleRepository.findAll();
    }

    /**
     * Get vehicle by ID
     */
    public Vehicle getVehicleById(Long id) {
        log.debug("Fetching vehicle by ID: {}", id);
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with ID: " + id));
    }

    /**
     * Get vehicle by registration number
     */
    public Vehicle getVehicleByRegistration(String registrationNumber) {
        log.debug("Fetching vehicle by registration: {}", registrationNumber);
        return vehicleRepository.findByRegistrationNumber(registrationNumber)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with registration: " + registrationNumber));
    }

    /**
     * Create a new vehicle
     */
    @Transactional
    public Vehicle createVehicle(Vehicle vehicle) {
        log.info("Creating vehicle with registration: {}", vehicle.getRegistrationNumber());

        // Check if vehicle with same registration already exists
        if (vehicleRepository.findByRegistrationNumber(vehicle.getRegistrationNumber()).isPresent()) {
            throw new RuntimeException("Vehicle with registration " + vehicle.getRegistrationNumber() + " already exists");
        }

        return vehicleRepository.save(vehicle);
    }

    /**
     * Update vehicle
     */
    @Transactional
public Vehicle updateVehicle(Long id, VehicleDTO vehicleDTO) {
    log.info("Updating vehicle ID: {} with DTO: {}", id, vehicleDTO);

    Vehicle vehicle = getVehicleById(id);

    // Update basic fields - only if not null
    if (vehicleDTO.getRegistration_number() != null) {
        vehicle.setRegistrationNumber(vehicleDTO.getRegistration_number());
    }
    if (vehicleDTO.getVin() != null) {
        vehicle.setVin(vehicleDTO.getVin());
    }
    if (vehicleDTO.getMake() != null) {
        vehicle.setMake(vehicleDTO.getMake());
    }
    if (vehicleDTO.getModel() != null) {
        vehicle.setModel(vehicleDTO.getModel());
    }
    if (vehicleDTO.getYear() != null) {
        vehicle.setYear(vehicleDTO.getYear());
    }
    if (vehicleDTO.getFuelType() != null) {
        vehicle.setFuelType(vehicleDTO.getFuelType());
    }
    if (vehicleDTO.getCurrentMileage() != null) {
        vehicle.setCurrentMileage(vehicleDTO.getCurrentMileage());
    }
    if (vehicleDTO.getAvgConsumption() != null) {
        vehicle.setAvgConsumption(vehicleDTO.getAvgConsumption());
    }
    if (vehicleDTO.getCurrentOdometer() != null) {
        vehicle.setCurrentOdometer(vehicleDTO.getCurrentOdometer());
    }
    if (vehicleDTO.getStatus() != null) {
        vehicle.setStatus(VehicleStatus.valueOf(vehicleDTO.getStatus()));
    }

    // Update service-related fields
    if (vehicleDTO.getLastServiceDate() != null) {
        vehicle.setLastServiceDate(vehicleDTO.getLastServiceDate());
    }
    if (vehicleDTO.getLastServiceOdometer() != null) {
        vehicle.setLastServiceOdometer(vehicleDTO.getLastServiceOdometer());
    }
    if (vehicleDTO.getServiceIntervalDays() != null) {
        vehicle.setServiceIntervalDays(vehicleDTO.getServiceIntervalDays());
    }
    if (vehicleDTO.getServiceIntervalKm() != null) {
        vehicle.setServiceIntervalKm(vehicleDTO.getServiceIntervalKm());
    }
    if (vehicleDTO.getNextServiceDue() != null) {
        vehicle.setNextServiceDue(vehicleDTO.getNextServiceDue());
    }
    if (vehicleDTO.getNextServiceOdometer() != null) {
        vehicle.setNextServiceOdometer(vehicleDTO.getNextServiceOdometer());
    }
    if (vehicleDTO.getMaintenanceStatus() != null) {
        vehicle.setMaintenanceStatus(vehicleDTO.getMaintenanceStatus());
    }

    // Update insurance and roadworthy fields
    if (vehicleDTO.getInsurancePolicyNumber() != null) {
        vehicle.setInsurancePolicyNumber(vehicleDTO.getInsurancePolicyNumber());
    }
    if (vehicleDTO.getInsuranceExpiry() != null) {
        vehicle.setInsuranceExpiry(vehicleDTO.getInsuranceExpiry());
    }
    if (vehicleDTO.getRoadworthyExpiry() != null) {
        vehicle.setRoadworthyExpiry(vehicleDTO.getRoadworthyExpiry());
    }

    // Update other fields
    if (vehicleDTO.getFleetNumber() != null) {
        vehicle.setFleetNumber(vehicleDTO.getFleetNumber());
    }
    if (vehicleDTO.getGpsTrackerId() != null) {
        vehicle.setGpsTrackerId(vehicleDTO.getGpsTrackerId());
    }
    if (vehicleDTO.getIncidentsLogged() != null) {
        vehicle.setIncidentsLogged(vehicleDTO.getIncidentsLogged());
    }
    if (vehicleDTO.getNotes() != null) {
        vehicle.setNotes(vehicleDTO.getNotes());
    }
    if (vehicleDTO.getAuditTrail() != null) {
        vehicle.setAuditTrail(vehicleDTO.getAuditTrail());
    }
    if (vehicleDTO.getCategory() != null) {
        vehicle.setCategory(vehicleDTO.getCategory());
    }
    if (vehicleDTO.getVehicleType() != null) {
        vehicle.setVehicleType(vehicleDTO.getVehicleType());
    }
    if (vehicleDTO.getPurchaseDate() != null) {
        vehicle.setPurchaseDate(vehicleDTO.getPurchaseDate());
    }
    if (vehicleDTO.getPurchasePrice() != null) {
        vehicle.setPurchasePrice(vehicleDTO.getPurchasePrice());
    }
    if (vehicleDTO.getCurrentValue() != null) {
        vehicle.setCurrentValue(vehicleDTO.getCurrentValue());
    }

    // CRITICAL: Handle driver assignment properly
    if (vehicleDTO.getAssignedDriverId() != null) {
        if (vehicleDTO.getAssignedDriverId() > 0) {
            // Find and assign the driver
            Driver driver = driverRepository.findById(vehicleDTO.getAssignedDriverId())
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + vehicleDTO.getAssignedDriverId()));
            vehicle.setAssignedDriver(driver);
        } else {
            // If ID is 0 or negative, unassign the driver
            vehicle.setAssignedDriver(null);
        }
    }
    // If assignedDriverId is null, don't change the driver assignment

    // Recalculate next service date based on updated values
    vehicle.calculateNextService();

    return vehicleRepository.save(vehicle);
}

    /**
     * Delete vehicle
     */
    @Transactional
    public void deleteVehicle(Long id) {
        log.info("Deleting vehicle ID: {}", id);
        Vehicle vehicle = getVehicleById(id);
        vehicleRepository.delete(vehicle);
    }

    /**
     * Get vehicles by status
     */
    public List<Vehicle> getVehiclesByStatus(String status) {
        log.debug("Fetching vehicles by status: {}", status);
        return vehicleRepository.findByStatus(status);
    }

    /**
     * Get active vehicles
     */
    public List<Vehicle> getActiveVehicles() {
        log.debug("Fetching active vehicles");
        return vehicleRepository.findByStatusIn(List.of("ACTIVE", "AVAILABLE"));
    }
}
