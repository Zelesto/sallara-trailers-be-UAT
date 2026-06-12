package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.dto.CreateTripRequest;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.assets.Vehicle;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CreateTripMapper {

    public Trip toEntity(CreateTripRequest request,
                         Vehicle vehicle,
                         Driver driver,
                         Driver supervisor,
                         Long createdBy) {

        if (request == null) {
            return null;
        }

        Trip trip = new Trip();

        /* ========================
           REQUIRED RELATIONSHIPS
           ======================== */
        trip.setVehicle(vehicle); // REQUIRED (nullable = false)
        trip.setDriver(driver);
        trip.setSupervisor(supervisor);

        /* ========================
           IDENTITY
           ======================== */
        trip.setTripType(request.getTripType());

        /* ========================
           WORKFLOW
           ======================== */
        trip.setStatus(request.getStatus());
        trip.setApprovalStatus(request.getApprovalStatus());
        trip.setLastStatusUpdate(LocalDateTime.now());

        /* ========================
           PLANNING
           ======================== */
        trip.setPlannedStartDate(request.getPlannedStartDate());
        trip.setPlannedEndDate(request.getPlannedEndDate());
        trip.setPlannedDistanceKm(request.getPlannedDistanceKm());
        trip.setPlannedDurationHours(request.getPlannedDurationHours());
        trip.setEstimatedDurationHours(request.getEstimatedDurationHours());

        /* ========================
           COSTS
           ======================== */
        trip.setTollCost(request.getTollCost());
        trip.setOtherExpenses(request.getOtherExpenses());

        /* ========================
           CARGO
           ======================== */
        trip.setDistanceKm(request.getDistanceKm());
        trip.setFuelConsumedLiters(request.getFuelConsumedLiters());

        /* ========================
           CARGO DETAILS (if still in DTO)
           ======================== */
        trip.setCargoWeight(request.getCargoWeight());
        trip.setCargoValue(request.getCargoValue());
        trip.setCargoDescription(request.getCargoDescription());
        trip.setCommodityType(request.getCommodityType());
        trip.setPalletCount(request.getPalletCount());
        trip.setContainerNumber(request.getContainerNumber());

        /* ========================
           ORIGIN
           ======================== */
        trip.setOriginStreetAddress(request.getOriginStreetAddress());
        trip.setOriginCity(request.getOriginCity());
        trip.setOriginZipCode(request.getOriginZipCode());
        trip.setOriginProvince(request.getOriginProvince());
        trip.setOriginLatitude(request.getOriginLatitude());
        trip.setOriginLongitude(request.getOriginLongitude());

        String originLocation = request.getOriginLocation();
        trip.setOriginLocation(
                originLocation != null
                        ? originLocation
                        : buildOriginLocation(request)
        );

        /* ========================
           DESTINATION
           ======================== */
        trip.setDestinationStreetAddress(request.getDestinationStreetAddress());
        trip.setDestinationCity(request.getDestinationCity());
        trip.setDestinationZipCode(request.getDestinationZipCode());
        trip.setDestinationProvince(request.getDestinationProvince());
        trip.setDestinationLatitude(request.getDestinationLatitude());
        trip.setDestinationLongitude(request.getDestinationLongitude());

        String destinationLocation = request.getDestinationLocation();
        trip.setDestinationLocation(
                destinationLocation != null
                        ? destinationLocation
                        : buildDestinationLocation(request)
        );

        /* ========================
           EXECUTION (initial state = null)
           ======================== */
        trip.setActualStartDate(null);
        trip.setActualEndDate(null);
        trip.setActualStartOdometer(null);
        trip.setActualEndOdometer(null);
        trip.setActualDistanceKm(null);
        trip.setActualDurationHours(null);

        /* ========================
           NOTES
           ======================== */
        trip.setNotes(request.getNotes());
        trip.setSpecialInstructions(request.getSpecialInstructions());
        trip.setDriverNotes(request.getDriverNotes());

        /* ========================
           REFERENCES
           ======================== */
        trip.setReferenceNumber(request.getReferenceNumber());
        trip.setPurchaseOrderNumber(request.getPurchaseOrderNumber());

        /* ========================
           INCIDENTS / CANCELLATION
           ======================== */
        trip.setIncidentsLogged(
                request.getIncidentsLogged() != null ? request.getIncidentsLogged() : 0
        );

        trip.setCancellationReason(request.getCancellationReason());
        trip.setCancelledAt(null);

        /* ========================
           ROUTE / GPS
           ======================== */
        trip.setGpsStartLocation(request.getGpsStartLocation());
        trip.setGpsEndLocation(request.getGpsEndLocation());
        trip.setRouteDetails(request.getRouteDetails());
        trip.setCheckpoints(request.getCheckpoints());

        /* ========================
           AUDIT
           ======================== */
        LocalDateTime now = LocalDateTime.now();

        trip.setCreatedAt(now);
        trip.setUpdatedAt(now);
        trip.setCreatedBy(createdBy);
        trip.setUpdatedBy(createdBy);
        trip.setAuditTrail(request.getAuditTrail());

        return trip;
    }

    /* =========================================================
       Helper: Origin Location Builder
       ========================================================= */
    private String buildOriginLocation(CreateTripRequest request) {
        StringBuilder sb = new StringBuilder();

        if (request.getOriginStreetAddress() != null) sb.append(request.getOriginStreetAddress());
        if (request.getOriginCity() != null) sb.append(", ").append(request.getOriginCity());
        if (request.getOriginZipCode() != null) sb.append(" ").append(request.getOriginZipCode());
        if (request.getOriginProvince() != null) sb.append(", ").append(request.getOriginProvince());

        return sb.toString().trim();
    }

    /* =========================================================
       Helper: Destination Location Builder
       ========================================================= */
    private String buildDestinationLocation(CreateTripRequest request) {
        StringBuilder sb = new StringBuilder();

        if (request.getDestinationStreetAddress() != null) sb.append(request.getDestinationStreetAddress());
        if (request.getDestinationCity() != null) sb.append(", ").append(request.getDestinationCity());
        if (request.getDestinationZipCode() != null) sb.append(" ").append(request.getDestinationZipCode());
        if (request.getDestinationProvince() != null) sb.append(", ").append(request.getDestinationProvince());

        return sb.toString().trim();
    }
}
