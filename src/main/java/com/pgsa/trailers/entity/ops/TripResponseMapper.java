package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.dto.TripMetricsResponse;
import com.pgsa.trailers.dto.TripResponse;
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

        // ======================== CUSTOMER - SAFE LAZY LOADING ========================
        try {
            if (trip.getCustomer() != null) {
                response.setCustomerId(trip.getCustomer().getId());
                response.setCustomerName(trip.getCustomer().getName());
                response.setCustomerCode(trip.getCustomer().getCustomerCode() != null 
                    ? trip.getCustomer().getCustomerCode() 
                    : null);
            } else if (trip.getCustomerId() != null) {
                response.setCustomerId(trip.getCustomerId());
            }
        } catch (Exception e) {
            log.debug("Could not load customer for trip: {}", trip.getId());
            if (trip.getCustomerId() != null) {
                response.setCustomerId(trip.getCustomerId());
            }
        }

        // ======================== LOAD ========================
        try {
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
        } catch (Exception e) {
            log.debug("Could not load load details for trip: {}", trip.getId());
            if (trip.getLoadId() != null) {
                response.setLoadId(trip.getLoadId());
                response.setLoadNumber(trip.getLoadNumber());
                response.setLoadType(trip.getLoadType());
                response.setLoadDescription(trip.getLoadDescription());
                response.setLoadStatus(trip.getLoadStatus());
            }
        }

        // ======================== LOCATIONS ========================
        response.setOriginLocation(trip.getOriginLocation());
        response.setDestinationLocation(trip.getDestinationLocation());

        // Origin details
        response.setOriginStreetAddress(trip.getOriginStreetAddress());
        response.setOriginCity(trip.getOriginCity());
        response.setOriginZipCode(trip.getOriginZipCode());
        response.setOriginProvince(trip.getOriginProvince());
        response.setOriginLatitude(trip.getOriginLatitude());
        response.setOriginLongitude(trip.getOriginLongitude());

        // Destination details
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

        // ======================== AUDIT ========================
        response.setCreatedAt(trip.getCreatedAt());
        response.setUpdatedAt(trip.getUpdatedAt());
        response.setCreatedBy(trip.getCreatedBy());
        response.setUpdatedBy(trip.getUpdatedBy());

        // ======================== VEHICLE - SAFE LAZY LOADING ========================
        try {
            if (trip.getVehicle() != null) {
                response.setVehicleId(trip.getVehicle().getId());
                response.setVehicleRegistration(
                    trip.getVehicle().getRegistrationNumber() != null 
                        ? trip.getVehicle().getRegistrationNumber() 
                        : null
                );
                response.setVehicleMake(trip.getVehicle().getMake());
                response.setVehicleModel(trip.getVehicle().getModel());
            }
        } catch (Exception e) {
            log.debug("Could not load vehicle for trip: {}", trip.getId());
            // Vehicle might not be loaded, leave as null
        }

        // ======================== DRIVER - SAFE LAZY LOADING ========================
        try {
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
            }
        } catch (Exception e) {
            log.debug("Could not load driver for trip: {}", trip.getId());
            // Driver might not be loaded, leave as null
        }

        // ======================== SUPERVISOR - SAFE LAZY LOADING ========================
        try {
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
        } catch (Exception e) {
            log.debug("Could not load supervisor for trip: {}", trip.getId());
            // Supervisor might not be loaded, leave as null
        }

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

        // ======================== COSTS ========================
        response.setTollCost(trip.getTollCost());
        response.setOtherExpenses(trip.getOtherExpenses());
        response.setCostAmount(trip.getCostAmount());
        response.setRevenueAmount(trip.getRevenueAmount());

        // ======================== FUEL ========================
        response.setFuelConsumedLiters(trip.getFuelConsumedLiters());

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
        response.setIsActive(trip.getIsActive());
        response.setLastStatusUpdate(trip.getLastStatusUpdate());

        // ======================== METRICS ========================
        try {
            if (trip.getMetrics() != null) {
                response.setMetrics(toMetricsResponse(trip.getMetrics()));
            }
        } catch (Exception e) {
            log.debug("Could not load metrics for trip: {}", trip.getId());
            // Metrics might not be loaded, leave as null
        }

        return response;
    }

    private TripMetricsResponse toMetricsResponse(TripMetrics metrics) {
        if (metrics == null) {
            return null;
        }

        TripMetricsResponse dto = new TripMetricsResponse();

        // ======================== BASIC METRICS ========================
        dto.setTotalDistanceKm(metrics.getTotalDistanceKm());
        dto.setTotalDurationHours(metrics.getTotalDurationHours());
        dto.setIdleTimeHours(metrics.getIdleTimeHours());
        dto.setAverageSpeedKmh(metrics.getAverageSpeedKmh());
        dto.setFuelUsedLiters(metrics.getFuelUsedLiters());

        // ======================== INCIDENT & TASKS ========================
        dto.setIncidentCount(metrics.getIncidentCount());
        dto.setTasksCompleted(metrics.getTasksCompleted());

        // ======================== FINANCIAL ========================
        dto.setRevenueAmount(metrics.getRevenueAmount());
        dto.setCostAmount(metrics.getCostAmount());

        // ======================== VARIANCE ========================
        dto.setOriginCityTravelTimeHours(metrics.getOriginCityTravelTimeHours());
        dto.setDestinationCityTravelTimeHours(metrics.getDestinationCityTravelTimeHours());
        dto.setPlannedVsActualDistanceVarianceKm(metrics.getPlannedVsActualDistanceVarianceKm());
        dto.setPlannedVsActualDurationVarianceHours(metrics.getPlannedVsActualDurationVarianceHours());
        dto.setGeocodingConfidenceScore(metrics.getGeocodingConfidenceScore());

        // ======================== AUDIT ========================
        dto.setCalculatedAt(metrics.getCalculatedAt());
        dto.setCreatedAt(metrics.getCreatedAt());
        dto.setUpdatedAt(metrics.getUpdatedAt());

        return dto;
    }
}
