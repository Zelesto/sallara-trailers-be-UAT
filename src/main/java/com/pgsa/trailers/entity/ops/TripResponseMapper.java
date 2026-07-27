package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.dto.*;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.assets.Vehicle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Slf4j
public class TripResponseMapper {

    public TripResponse toResponse(Trip trip) {
        if (trip == null) {
            return null;
        }

        TripResponse response = new TripResponse();

        // ======================== IDENTITY ========================
        response.setId(trip.getId());
        response.setTripNumber(trip.getTripNumber());
        response.setTripType(trip.getTripType());

        // ======================== CUSTOMER ========================
        response.setCustomer(toCustomerDTO(trip.getCustomer()));
        if (trip.getCustomer() != null) {
            response.setCustomerId(trip.getCustomer().getId());
            response.setCustomerName(trip.getCustomer().getName());
            response.setCustomerCode(trip.getCustomer().getCustomerCode());
        } else if (trip.getCustomerId() != null) {
            response.setCustomerId(trip.getCustomerId());
        }

        // ======================== LOAD ========================
        response.setLoad(toLoadDTO(trip.getLoad()));
        if (trip.getLoad() != null) {
            response.setLoadId(trip.getLoad().getLoadNumber());
            response.setLoadNumber(trip.getLoad().getLoadNumber());
            response.setLoadType(trip.getLoad().getCommodityType());
            response.setLoadDescription(trip.getLoad().getDescription());
            response.setLoadStatus(trip.getLoad().getStatus() != null 
                ? trip.getLoad().getStatus().name() 
                : null);
        } else if (trip.getLoadId() != null) {
            response.setLoadId(trip.getLoadId());
            response.setLoadNumber(trip.getLoadNumber());
            response.setLoadType(trip.getLoadType());
            response.setLoadDescription(trip.getLoadDescription());
            response.setLoadStatus(trip.getLoadStatus());
        }

        // ======================== VEHICLE ========================
        response.setVehicle(toVehicleDTO(trip.getVehicle()));
        if (trip.getVehicle() != null) {
            response.setVehicleId(trip.getVehicle().getId());
            response.setVehicleRegistration(trip.getVehicle().getRegistrationNumber());
            response.setVehicleMake(trip.getVehicle().getMake());
            response.setVehicleModel(trip.getVehicle().getModel());
        }

        // ======================== DRIVER ========================
        response.setDriver(toDriverDTO(trip.getDriver()));
        if (trip.getDriver() != null) {
            response.setDriverId(trip.getDriver().getId());
            String firstName = trip.getDriver().getFirstName() != null 
                ? trip.getDriver().getFirstName() 
                : "";
            String lastName = trip.getDriver().getLastName() != null 
                ? trip.getDriver().getLastName() 
                : "";
            String fullName = (firstName + " " + lastName).trim();
            response.setDriverName(fullName.isEmpty() ? null : fullName);
            response.setDriverLicenseNumber(trip.getDriver().getLicenseNumber());
        }

        // ======================== SUPERVISOR ========================
        response.setSupervisor(toDriverDTO(trip.getSupervisor()));
        if (trip.getSupervisor() != null) {
            response.setSupervisorId(trip.getSupervisor().getId());
            String firstName = trip.getSupervisor().getFirstName() != null 
                ? trip.getSupervisor().getFirstName() 
                : "";
            String lastName = trip.getSupervisor().getLastName() != null 
                ? trip.getSupervisor().getLastName() 
                : "";
            String fullName = (firstName + " " + lastName).trim();
            response.setSupervisorName(fullName.isEmpty() ? null : fullName);
        }

        // ======================== LOCATIONS ========================
        response.setOriginLocation(trip.getOriginLocation());
        response.setOriginStreetAddress(trip.getOriginStreetAddress());
        response.setOriginCity(trip.getOriginCity());
        response.setOriginZipCode(trip.getOriginZipCode());
        response.setOriginProvince(trip.getOriginProvince());
        response.setOriginLatitude(trip.getOriginLatitude());
        response.setOriginLongitude(trip.getOriginLongitude());

        response.setDestinationLocation(trip.getDestinationLocation());
        response.setDestinationStreetAddress(trip.getDestinationStreetAddress());
        response.setDestinationCity(trip.getDestinationCity());
        response.setDestinationZipCode(trip.getDestinationZipCode());
        response.setDestinationProvince(trip.getDestinationProvince());
        response.setDestinationLatitude(trip.getDestinationLatitude());
        response.setDestinationLongitude(trip.getDestinationLongitude());

        // ======================== DATES ========================
        response.setPlannedStartDate(trip.getPlannedStartDate());
        response.setPlannedEndDate(trip.getPlannedEndDate());
        response.setActualStartDate(trip.getActualStartDate());
        response.setActualEndDate(trip.getActualEndDate());

        // ======================== STATUS ========================
        response.setStatus(trip.getStatus());
        response.setApprovalStatus(trip.getApprovalStatus());
        response.setPriority(trip.getPriority());
        response.setApprovedAt(trip.getApprovedAt());

        // ======================== AUDIT ========================
        response.setCreatedAt(trip.getCreatedAt());
        response.setUpdatedAt(trip.getUpdatedAt());
        response.setCreatedBy(trip.getCreatedBy());
        response.setUpdatedBy(trip.getUpdatedBy());
        response.setLastStatusUpdate(trip.getLastStatusUpdate());

        // ======================== CARGO ========================
        response.setCommodityType(trip.getCommodityType());
        response.setCargoDescription(trip.getCargoDescription());
        response.setCargoWeight(trip.getCargoWeight());
        response.setCargoValue(trip.getCargoValue());
        response.setPalletCount(trip.getPalletCount());
        response.setContainerNumber(trip.getContainerNumber());

        // ======================== PLANNING ========================
        response.setPlannedDistanceKm(trip.getPlannedDistanceKm());
        response.setPlannedDurationHours(trip.getPlannedDurationHours());
        response.setEstimatedDurationHours(trip.getEstimatedDurationHours());

        // ======================== EXECUTION ========================
        response.setActualStartOdometer(trip.getActualStartOdometer());
        response.setActualEndOdometer(trip.getActualEndOdometer());
        response.setActualDistanceKm(trip.getActualDistanceKm());
        response.setActualDurationHours(trip.getActualDurationHours());

        // ======================== METRICS ========================
        response.setDistanceKm(trip.getActualDistanceKm());
        response.setFuelConsumedLiters(trip.getFuelConsumedLiters());

        // ======================== COSTS ========================
        response.setTollCost(trip.getTollCost());
        response.setOtherExpenses(trip.getOtherExpenses());
        response.setCostAmount(trip.getCostAmount());
        response.setRevenueAmount(trip.getRevenueAmount());

        // ======================== ROUTE ========================
        response.setGpsStartLocation(trip.getGpsStartLocation());
        response.setGpsEndLocation(trip.getGpsEndLocation());
        response.setRouteDetails(trip.getRouteDetails());
        response.setCheckpoints(trip.getCheckpoints());

        // ======================== NOTES ========================
        response.setNotes(trip.getNotes());
        response.setSpecialInstructions(trip.getSpecialInstructions());
        response.setDriverNotes(trip.getDriverNotes());

        // ======================== REFERENCES ========================
        response.setReferenceNumber(trip.getReferenceNumber());
        response.setPurchaseOrderNumber(trip.getPurchaseOrderNumber());

        // ======================== DEPOT TRACKING ========================
        response.setFromDepotKm(trip.getFromDepotKm());
        response.setToDepotKm(trip.getToDepotKm());
        response.setDepartedFrom(trip.getDepartedFrom());
        response.setDepartureLocation(trip.getDepartureLocation());
        response.setIsFromDepot(trip.getIsFromDepot());

        // ======================== OPERATIONS ========================
        response.setIncidentsLogged(trip.getIncidentsLogged());
        response.setCancellationReason(trip.getCancellationReason());
        response.setCancelledAt(trip.getCancelledAt());

        // ======================== METRICS ========================
        if (trip.getMetrics() != null) {
            response.setMetrics(toTripMetricsResponse(trip.getMetrics()));
        }

        return response;
    }

    // ======================== DTO CONVERTERS ========================

    /**
     * Convert Customer entity to CustomerDTO
     */
    private CustomerDTO toCustomerDTO(Customer customer) {
        if (customer == null) {
            return null;
        }

        CustomerDTO dto = new CustomerDTO();
        dto.setId(customer.getId());
        dto.setCustomerCode(customer.getCustomerCode());
        dto.setName(customer.getName());
        dto.setRegistrationNumber(customer.getRegistrationNumber());
        dto.setVatNumber(customer.getVatNumber());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setAddressLine1(customer.getAddressLine1());
        dto.setAddressLine2(customer.getAddressLine2());
        dto.setCity(customer.getCity());
        dto.setProvince(customer.getProvince());
        dto.setPostalCode(customer.getPostalCode());
        dto.setCountry(customer.getCountry());
        dto.setContactPerson(customer.getContactPerson());
        dto.setContactPhone(customer.getContactPhone());
        dto.setContactEmail(customer.getContactEmail());
        dto.setPaymentTerms(customer.getPaymentTerms());
        dto.setCreditLimit(customer.getCreditLimit());
        dto.setIsActive(customer.getIsActive());
        dto.setNotes(customer.getNotes());
        dto.setCreatedAt(customer.getCreatedAt());
        dto.setCreatedBy(customer.getCreatedBy());
        dto.setUpdatedAt(customer.getUpdatedAt());
        dto.setUpdatedBy(customer.getUpdatedBy());

        return dto;
    }

    /**
     * Convert Vehicle entity to VehicleDTO
     */
    private VehicleDTO toVehicleDTO(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }

        VehicleDTO dto = new VehicleDTO();
        dto.setId(vehicle.getId());
        dto.setRegistrationNumber(vehicle.getRegistrationNumber());
        dto.setMake(vehicle.getMake());
        dto.setModel(vehicle.getModel());
        dto.setYear(vehicle.getYear());
        dto.setLicensePlate(vehicle.getLicensePlate());
        dto.setVin(vehicle.getVin());
        dto.setVehicleType(vehicle.getVehicleType());
        dto.setFuelType(vehicle.getFuelType());
        dto.setStatus(vehicle.getStatus());
        dto.setCurrentOdometer(vehicle.getCurrentOdometer());
        dto.setLastServiceDate(vehicle.getLastServiceDate());
        dto.setNextServiceDue(vehicle.getNextServiceDue());
        dto.setIsActive(vehicle.getIsActive());
        dto.setNotes(vehicle.getNotes());

        return dto;
    }

    /**
     * Convert Driver entity to DriverDTO
     */
    private DriverDTO toDriverDTO(Driver driver) {
        if (driver == null) {
            return null;
        }

        DriverDTO dto = new DriverDTO();
        dto.setId(driver.getId());
        dto.setFirstName(driver.getFirstName());
        dto.setLastName(driver.getLastName());
        dto.setLicenseNumber(driver.getLicenseNumber());
        dto.setLicenseClass(driver.getLicenseClass());
        dto.setLicenseExpiry(driver.getLicenseExpiry());
        dto.setPhone(driver.getPhone());
        dto.setEmail(driver.getEmail());
        dto.setAddress(driver.getAddress());
        dto.setHireDate(driver.getHireDate());
        dto.setEmployeeNumber(driver.getEmployeeNumber());
        dto.setStatus(driver.getStatus());
        dto.setIsActive(driver.getIsActive());
        dto.setNotes(driver.getNotes());

        return dto;
    }

    /**
     * Convert Load entity to LoadDTO
     */
    private LoadDTO toLoadDTO(Load load) {
        if (load == null) {
            return null;
        }

        LoadDTO dto = new LoadDTO();
        dto.setId(load.getId());
        dto.setLoadNumber(load.getLoadNumber());
        dto.setReferenceNumber(load.getReferenceNumber());
        dto.setCustomerId(load.getCustomerId());
        dto.setDescription(load.getDescription());
        dto.setCommodityType(load.getCommodityType());
        dto.setStatus(load.getStatus());
        dto.setTripsCount(load.getTripsCount());
        dto.setOriginLocation(load.getOriginLocation());
        dto.setDestinationLocation(load.getDestinationLocation());
        dto.setTotalFromDepotKm(load.getTotalFromDepotKm());
        dto.setTotalToDepotKm(load.getTotalToDepotKm());
        dto.setCreatedAt(load.getCreatedAt());
        dto.setCreatedBy(load.getCreatedBy());
        dto.setUpdatedAt(load.getUpdatedAt());
        dto.setUpdatedBy(load.getUpdatedBy());
        dto.setLastStatusUpdate(load.getLastStatusUpdate());

        return dto;
    }

    /**
     * Convert TripMetrics entity to TripMetricsResponse
     */
    private TripMetricsResponse toTripMetricsResponse(TripMetrics metrics) {
        if (metrics == null) {
            return null;
        }

        TripMetricsResponse dto = new TripMetricsResponse();

        // Basic metrics
        dto.setTotalDistanceKm(metrics.getTotalDistanceKm());
        dto.setTotalDurationHours(metrics.getTotalDurationHours());
        dto.setIdleTimeHours(metrics.getIdleTimeHours());
        dto.setAverageSpeedKmh(metrics.getAverageSpeedKmh());
        dto.setFuelUsedLiters(metrics.getFuelUsedLiters());

        // Incident & tasks
        dto.setIncidentCount(metrics.getIncidentCount());
        dto.setTasksCompleted(metrics.getTasksCompleted());

        // Financial
        dto.setRevenueAmount(metrics.getRevenueAmount());
        dto.setCostAmount(metrics.getCostAmount());

        // Variance
        dto.setOriginCityTravelTimeHours(metrics.getOriginCityTravelTimeHours());
        dto.setDestinationCityTravelTimeHours(metrics.getDestinationCityTravelTimeHours());
        dto.setPlannedVsActualDistanceVarianceKm(metrics.getPlannedVsActualDistanceVarianceKm());
        dto.setPlannedVsActualDurationVarianceHours(metrics.getPlannedVsActualDurationVarianceHours());
        dto.setGeocodingConfidenceScore(metrics.getGeocodingConfidenceScore());

        // Audit
        dto.setCreatedAt(metrics.getCreatedAt());
        dto.setUpdatedAt(metrics.getUpdatedAt());

        return dto;
    }
}
