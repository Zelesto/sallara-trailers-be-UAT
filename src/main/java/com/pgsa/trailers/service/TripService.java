package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.CreateTripRequest;
import com.pgsa.trailers.dto.TripResponse;
import com.pgsa.trailers.dto.UpdateTripRequest;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.entity.ops.Customer;
import com.pgsa.trailers.entity.ops.CreateTripMapper;
import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.entity.ops.TripResponseMapper;
import com.pgsa.trailers.entity.ops.auto.TripCompletedEvent;
import com.pgsa.trailers.entity.ops.auto.TripPlannedEvent;
import com.pgsa.trailers.entity.ops.auto.TripStartedEvent;
import com.pgsa.trailers.enums.LoadStatus;
import com.pgsa.trailers.enums.TripStatus;
import com.pgsa.trailers.entity.suppliers.TripValidationException;
import com.pgsa.trailers.repository.CustomerRepository;
import com.pgsa.trailers.repository.DriverRepository;
import com.pgsa.trailers.repository.LoadRepository;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.repository.VehicleRepository;
import com.pgsa.trailers.service.util.TripNumberGenerator;
import com.pgsa.trailers.service.util.TripValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final TripMetricsService tripMetricsService;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final CustomerRepository customerRepository;
    private final LoadRepository loadRepository;
    private final TripNumberGenerator tripNumberGenerator;
    private final CreateTripMapper createTripMapper;
    private final TripResponseMapper tripResponseMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TripValidator tripValidator;
    private final SequenceService sequenceService;

    /* ========================
       CREATE
       ======================== */
    @Transactional
    public TripResponse createTrip(CreateTripRequest request, Long userId) {

        log.debug("Creating trip for vehicle: {}, user: {}", request.getVehicleId(), userId);
        log.info("📝 Creating trip with reference number: {}", request.getReferenceNumber());

        tripValidator.validateCreateRequest(request);

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new TripValidationException(
                        "Vehicle not found with ID: " + request.getVehicleId()));

        Driver driver = null;
        if (request.getDriverId() != null) {
            driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new TripValidationException(
                            "Driver not found with ID: " + request.getDriverId()));
        }

        Driver supervisor = null;
        if (request.getSupervisorId() != null) {
            supervisor = driverRepository.findById(request.getSupervisorId())
                    .orElseThrow(() -> new TripValidationException(
                            "Supervisor not found with ID: " + request.getSupervisorId()));
        }

        // Validate customer if provided
        Customer customer = null;
        if (request.getCustomerId() != null && request.getCustomerId() > 0) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new TripValidationException(
                            "Customer not found with ID: " + request.getCustomerId()));
        }

        Trip trip = createTripMapper.toEntity(request);

        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setSupervisor(supervisor);
        
        // Set customer
        if (customer != null) {
            trip.setCustomerId(customer.getId());
        }

        /* ========================
           DEPOT TRACKING
           ======================== */
        if (request.getFromDepotKm() != null) {
            trip.setFromDepotKm(request.getFromDepotKm());
        }
        if (request.getToDepotKm() != null) {
            trip.setToDepotKm(request.getToDepotKm());
        }
        if (request.getDepartedFrom() != null) {
            trip.setDepartedFrom(request.getDepartedFrom());
        }
        if (request.getDepartureLocation() != null) {
            trip.setDepartureLocation(request.getDepartureLocation());
        }
        trip.setIsFromDepot(request.getIsFromDepot() != null ? request.getIsFromDepot() : false);

        // ======================== LOAD HANDLING ========================
        Load load = null;
        
        // Check if loadId is provided (String - loadNumber)
        if (request.getLoadId() != null && !request.getLoadId().isEmpty()) {
            log.info("📦 Using provided loadId: {}", request.getLoadId());
            load = loadRepository.findByLoadNumber(request.getLoadId())
                    .orElseThrow(() -> new TripValidationException(
                            "Load not found with number: " + request.getLoadId()));
        } 
        // If referenceNumber is provided, find or create a load
        else if (request.getReferenceNumber() != null && !request.getReferenceNumber().trim().isEmpty()) {
    String referenceNumber = request.getReferenceNumber().trim();
    log.info("📦 Looking for load with reference number: {}", referenceNumber);
    
    Optional<Load> existingLoad = loadRepository.findByReferenceNumber(referenceNumber);
    
    if (existingLoad.isPresent()) {
        load = existingLoad.get();
        log.info("📦 Found existing load with Ref# {}: {}", referenceNumber, load.getLoadNumber());
    } else {
        log.info("📦 Creating new load for Ref#: {}", referenceNumber);
        load = new Load();
        load.setLoadNumber(generateLoadNumber());
        load.setReferenceNumber(referenceNumber);
        load.setCustomerId(request.getCustomerId());
        load.setDescription(request.getCargoDescription() != null ? 
            request.getCargoDescription() : "Load for Ref# " + referenceNumber);
        load.setCommodityType(request.getCommodityType());
        
        // FIX: Use LoadStatus enum directly
        load.setStatus(LoadStatus.PENDING); // This sets the enum value
        
        load.setTripsCount(0);
        load.setCreatedBy(userId != null ? String.valueOf(userId) : "System");
        load.setCreatedAt(LocalDateTime.now());
        load.setUpdatedAt(LocalDateTime.now());
        load.setLastStatusUpdate(LocalDateTime.now());
        load.setAuditTrail("{}");
        
        load.setOriginLocation(request.getOriginLocation());
        load.setDestinationLocation(request.getDestinationLocation());
        
        if (request.getFromDepotKm() != null) {
            load.setTotalFromDepotKm(request.getFromDepotKm());
        }
        if (request.getToDepotKm() != null) {
            load.setTotalToDepotKm(request.getToDepotKm());
        }
        
        load = loadRepository.save(load);
        log.info("✅ Created new load: {} for Ref#: {}", load.getLoadNumber(), referenceNumber);
    }
}
        
        // Associate load with trip if found/created
        if (load != null) {
            trip.setLoad(load);
            trip.setLoadId(load.getLoadNumber());  // String
            trip.setLoadNumber(load.getLoadNumber());
            trip.setLoadType(load.getCommodityType());
            trip.setLoadDescription(load.getDescription());
            trip.setLoadStatus(load.getStatus() != null ? load.getStatus() : "PENDING");
            
            if (load.getTrips() == null) {
                load.setTrips(new ArrayList<>());
            }
            load.getTrips().add(trip);
            load.setTripsCount(load.getTrips().size());
            load.setUpdatedAt(LocalDateTime.now());
            load.setLastStatusUpdate(LocalDateTime.now());
            
            // Recalculate depot totals for the load
            load.recalculateDepotTotals();
            
            loadRepository.save(load);
            
            log.info("✅ Trip associated with load: {}", load.getLoadNumber());
        } else {
            log.info("ℹ️ No load associated with this trip");
        }

        trip.setTripNumber(sequenceService.generateFormattedSequence("trip", "TRP"));
        trip.setStatus(request.getStatus() != null ? request.getStatus() : TripStatus.DRAFT);
        trip.setCreatedBy(userId);
        trip.setLastStatusUpdate(LocalDateTime.now());

        // Save trip
        Trip saved = tripRepository.save(trip);

        log.info("✅ Created trip with ID: {}, Number: {}, Customer: {}, Load: {}",
                saved.getId(),
                saved.getTripNumber(),
                customer != null ? customer.getName() : "None",
                load != null ? load.getLoadNumber() : "None"
        );

        // Create initial metrics record
        tripMetricsService.initializeMetrics(saved.getId());

        if (saved.getStatus() == TripStatus.PLANNED) {
            eventPublisher.publishEvent(new TripPlannedEvent(saved.getId()));
        }

        return tripResponseMapper.toResponse(saved);
    }

    /**
     * Generate a unique load number
     */
    private String generateLoadNumber() {
        return "LOAD-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    /* ========================
       START TRIP
       ======================== */
    @Transactional
    public TripResponse startTrip(Long tripId, BigDecimal actualStartOdometer, Long userId) {
        log.debug("Starting trip ID: {} with odometer: {}", tripId, actualStartOdometer);
        
        Trip trip = findTripOrThrow(tripId);
        
        tripValidator.validateCanStart(trip, actualStartOdometer);

        trip.setActualStartOdometer(actualStartOdometer);
        trip.setActualStartDate(LocalDateTime.now());
        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setLastStatusUpdate(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now());
        trip.setUpdatedBy(userId);

        Trip updated = tripRepository.save(trip);
        
        eventPublisher.publishEvent(new TripStartedEvent(tripId));
        log.info("Trip {} started", tripId);

        return tripResponseMapper.toResponse(updated);
    }

    /* ========================
       END TRIP
       ======================== */
    @Transactional
    public TripResponse endTrip(Long tripId, BigDecimal actualEndOdometer, Long userId) {
        log.debug("Ending trip ID: {} with odometer: {}", tripId, actualEndOdometer);
        
        Trip trip = findTripOrThrow(tripId);
        
        tripValidator.validateCanEnd(trip, actualEndOdometer);

        BigDecimal startOdo = trip.getActualStartOdometer();
        
        trip.setActualEndOdometer(actualEndOdometer);
        trip.setActualEndDate(LocalDateTime.now());
        trip.setStatus(TripStatus.COMPLETED);
        trip.setLastStatusUpdate(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now());
        trip.setUpdatedBy(userId);
        trip.setActualDistanceKm(actualEndOdometer.subtract(startOdo));
        
        if (trip.getActualStartDate() != null && trip.getActualEndDate() != null) {
            long hours = java.time.Duration.between(trip.getActualStartDate(), trip.getActualEndDate()).toHours();
            trip.setActualDurationHours(BigDecimal.valueOf(hours));
        }

        Trip updated = tripRepository.save(trip);
        
        eventPublisher.publishEvent(new TripCompletedEvent(tripId));
        log.info("Trip {} completed. Distance: {} km", tripId, trip.getActualDistanceKm());

        return tripResponseMapper.toResponse(updated);
    }

    /* ========================
       READ
       ======================== */
    @Transactional(readOnly = true)
    public TripResponse getTrip(Long id) {
        Trip trip = findTripOrThrow(id);
        return tripResponseMapper.toResponse(trip);
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> listTrips(Pageable pageable) {
        return tripRepository.findAll(pageable)
                .map(tripResponseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> getTripsByCustomer(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new TripValidationException("Customer not found with ID: " + customerId);
        }
        return tripRepository.findByCustomerId(customerId, Pageable.unpaged())
                .getContent()
                .stream()
                .map(tripResponseMapper::toResponse)
                .collect(Collectors.toList());
    }

    // loadId is String
    @Transactional(readOnly = true)
    public List<TripResponse> getTripsByLoad(String loadId) {
        return tripRepository.findByLoadId(loadId)
                .stream()
                .map(tripResponseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> getTripsByCustomerPaginated(Long customerId, Pageable pageable) {
        if (!customerRepository.existsById(customerId)) {
            throw new TripValidationException("Customer not found with ID: " + customerId);
        }
        return tripRepository.findByCustomerId(customerId, pageable)
                .map(tripResponseMapper::toResponse);
    }

    /* ========================
       STATUS UPDATE
       ======================== */
    @Transactional
    public TripResponse updateTripStatus(Long tripId, TripStatus newStatus, Long userId) {
        log.debug("Updating trip {} status to: {}", tripId, newStatus);
        
        Trip trip = findTripOrThrow(tripId);
        
        tripValidator.validateStatusTransition(trip.getStatus(), newStatus);
        
        TripStatus oldStatus = trip.getStatus();
        trip.setStatus(newStatus);
        trip.setLastStatusUpdate(LocalDateTime.now());
        trip.setUpdatedBy(userId);
        
        if (newStatus == TripStatus.CANCELLED) {
            trip.setCancelledAt(LocalDateTime.now());
        }
        
        if (newStatus == TripStatus.COMPLETED && oldStatus != TripStatus.COMPLETED) {
            trip.calculateActualDistance();
            if (trip.getActualStartDate() != null && trip.getActualEndDate() != null) {
                long hours = java.time.Duration.between(trip.getActualStartDate(), trip.getActualEndDate()).toHours();
                trip.setActualDurationHours(BigDecimal.valueOf(hours));
            }
        }

        Trip saved = tripRepository.save(trip);

        switch (newStatus) {
            case PLANNED -> eventPublisher.publishEvent(new TripPlannedEvent(tripId));
            case IN_PROGRESS -> eventPublisher.publishEvent(new TripStartedEvent(tripId));
            case COMPLETED -> eventPublisher.publishEvent(new TripCompletedEvent(tripId));
            default -> log.debug("Status changed from {} to {}, no event published", oldStatus, newStatus);
        }
        
        log.info("Trip {} status changed from {} to {}", tripId, oldStatus, newStatus);

        return tripResponseMapper.toResponse(saved);
    }

    /* ========================
       CUSTOMER & LOAD MANAGEMENT
       ======================== */
    
    /**
     * Assign customer to a trip
     */
    @Transactional
    public TripResponse assignCustomerToTrip(Long tripId, Long customerId, Long userId) {
        log.debug("Assigning customer {} to trip {}", customerId, tripId);
        
        Trip trip = findTripOrThrow(tripId);
        
        if (customerId != null) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new TripValidationException("Customer not found with ID: " + customerId));
            trip.setCustomerId(customer.getId());
        } else {
            trip.setCustomerId(null);
        }
        
        trip.setUpdatedAt(LocalDateTime.now());
        trip.setUpdatedBy(userId);
        
        Trip updated = tripRepository.save(trip);
        log.info("Customer assigned to trip {}: {}", tripId, customerId);
        
        return tripResponseMapper.toResponse(updated);
    }

    /**
     * Assign load to a trip - loadId is String (loadNumber)
     */
    @Transactional
    public TripResponse assignLoadToTrip(Long tripId, String loadId, Long userId) {
        log.debug("Assigning load {} to trip {}", loadId, tripId);
        
        Trip trip = findTripOrThrow(tripId);
        
        if (loadId != null && !loadId.isEmpty()) {
            Load load = loadRepository.findByLoadNumber(loadId)
                    .orElseThrow(() -> new TripValidationException("Load not found with number: " + loadId));
            trip.setLoad(load);
            trip.setLoadId(load.getLoadNumber());
            trip.setLoadNumber(load.getLoadNumber());
            trip.setLoadType(load.getCommodityType());
            trip.setLoadDescription(load.getDescription());
            trip.setLoadStatus(load.getStatus() != null ? load.getStatus() : "PENDING");
        } else {
            trip.setLoad(null);
            trip.setLoadId(null);
            trip.setLoadNumber(null);
            trip.setLoadType(null);
            trip.setLoadDescription(null);
            trip.setLoadStatus(null);
        }
        
        trip.setUpdatedAt(LocalDateTime.now());
        trip.setUpdatedBy(userId);
        
        Trip updated = tripRepository.save(trip);
        log.info("Load assigned to trip {}: {}", tripId, loadId);
        
        return tripResponseMapper.toResponse(updated);
    }

    /**
     * Generate a new load ID
     */
    public String generateLoadId() {
        return "LD-" + System.currentTimeMillis();
    }

    /**
     * Update trip with customer and load information in one call - loadId is String
     */
    @Transactional
    public TripResponse updateTripCustomerAndLoad(Long tripId, Long customerId, String loadId, Long userId) {
        log.debug("Updating trip {} with customer: {} and load: {}", tripId, customerId, loadId);
        
        Trip trip = findTripOrThrow(tripId);
        
        // Update customer
        if (customerId != null) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new TripValidationException("Customer not found with ID: " + customerId));
            trip.setCustomerId(customer.getId());
        } else {
            trip.setCustomerId(null);
        }
        
        // Update load
        if (loadId != null && !loadId.isEmpty()) {
            Load load = loadRepository.findByLoadNumber(loadId)
                    .orElseThrow(() -> new TripValidationException("Load not found with number: " + loadId));
            trip.setLoad(load);
            trip.setLoadId(load.getLoadNumber());
            trip.setLoadNumber(load.getLoadNumber());
            trip.setLoadType(load.getCommodityType());
            trip.setLoadDescription(load.getDescription());
            trip.setLoadStatus(load.getStatus() != null ? load.getStatus() : "PENDING");
        } else {
            trip.setLoad(null);
            trip.setLoadId(null);
            trip.setLoadNumber(null);
            trip.setLoadType(null);
            trip.setLoadDescription(null);
            trip.setLoadStatus(null);
        }
        
        trip.setUpdatedAt(LocalDateTime.now());
        trip.setUpdatedBy(userId);
        
        Trip updated = tripRepository.save(trip);
        log.info("Trip {} updated with customer: {} and load: {}", tripId, customerId, loadId);
        
        return tripResponseMapper.toResponse(updated);
    }

    /* ========================
       INCIDENT RULE
       ======================== */
    public boolean canReportIncident(Trip trip) {
        return trip.isActive();
    }

    /* ========================
       DELETE
       ======================== */
    @Transactional
    public void deleteTrip(Long id) {
        log.debug("Deleting trip ID: {}", id);
        
        Trip trip = findTripOrThrow(id);
        
        if (trip.getStatus().isTerminal()) {
            throw new TripValidationException("Cannot delete trip with terminal status: " + trip.getStatus());
        }
        
        if (trip.getMetrics() != null) {
            trip.setMetrics(null);
        }
        
        tripRepository.delete(trip);
        log.info("Deleted trip ID: {}", id);
    }

    /* ========================
       UPDATE (PATCH SAFE)
       ======================== */
    @Transactional
    public TripResponse updateTrip(Long tripId, UpdateTripRequest request, Long userId) {
        log.debug("Updating trip ID: {} with request", tripId);
        
        Trip trip = findTripOrThrow(tripId);
        
        tripValidator.validateCanUpdate(trip);
        
        LocalDateTime now = LocalDateTime.now();

        if (request.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new TripValidationException("Vehicle not found with ID: " + request.getVehicleId()));
            trip.setVehicle(vehicle);
        }

        if (request.getDriverId() != null) {
            Driver driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new TripValidationException("Driver not found with ID: " + request.getDriverId()));
            trip.setDriver(driver);
        }

        if (request.getSupervisorId() != null) {
            Driver supervisor = driverRepository.findById(request.getSupervisorId())
                    .orElseThrow(() -> new TripValidationException("Supervisor not found with ID: " + request.getSupervisorId()));
            trip.setSupervisor(supervisor);
        }

        // Update customer if provided
        if (request.getCustomerId() != null && request.getCustomerId() > 0) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new TripValidationException("Customer not found with ID: " + request.getCustomerId()));
            trip.setCustomerId(customer.getId());
        }

        // Update load if provided - loadId is String (loadNumber)
        if (request.getLoadId() != null) {
            if (!request.getLoadId().isEmpty()) {
                Load load = loadRepository.findByLoadNumber(request.getLoadId())
                        .orElseThrow(() -> new TripValidationException("Load not found with number: " + request.getLoadId()));
                trip.setLoad(load);
                trip.setLoadId(load.getLoadNumber());
                trip.setLoadNumber(load.getLoadNumber());
                trip.setLoadType(load.getCommodityType());
                trip.setLoadDescription(load.getDescription());
                trip.setLoadStatus(load.getStatus() != null ? load.getStatus() : "PENDING");
            } else {
                trip.setLoad(null);
                trip.setLoadId(null);
                trip.setLoadNumber(null);
                trip.setLoadType(null);
                trip.setLoadDescription(null);
                trip.setLoadStatus(null);
            }
        }

        /* ========================
           DEPOT TRACKING
           ======================== */
        if (request.getFromDepotKm() != null) {
            trip.setFromDepotKm(request.getFromDepotKm());
        }
        if (request.getToDepotKm() != null) {
            trip.setToDepotKm(request.getToDepotKm());
        }
        if (request.getDepartedFrom() != null) {
            trip.setDepartedFrom(request.getDepartedFrom());
        }
        if (request.getDepartureLocation() != null) {
            trip.setDepartureLocation(request.getDepartureLocation());
        }
        if (request.getIsFromDepot() != null) {
            trip.setIsFromDepot(request.getIsFromDepot());
        }

        Optional.ofNullable(request.getOriginLocation()).ifPresent(trip::setOriginLocation);
        Optional.ofNullable(request.getDestinationLocation()).ifPresent(trip::setDestinationLocation);
        
        Optional.ofNullable(request.getOriginStreetAddress()).ifPresent(trip::setOriginStreetAddress);
        Optional.ofNullable(request.getOriginCity()).ifPresent(trip::setOriginCity);
        Optional.ofNullable(request.getOriginZipCode()).ifPresent(trip::setOriginZipCode);
        Optional.ofNullable(request.getOriginProvince()).ifPresent(trip::setOriginProvince);

        Optional.ofNullable(request.getDestinationStreetAddress()).ifPresent(trip::setDestinationStreetAddress);
        Optional.ofNullable(request.getDestinationCity()).ifPresent(trip::setDestinationCity);
        Optional.ofNullable(request.getDestinationZipCode()).ifPresent(trip::setDestinationZipCode);
        Optional.ofNullable(request.getDestinationProvince()).ifPresent(trip::setDestinationProvince);

        Optional.ofNullable(request.getActualStartDate()).ifPresent(trip::setActualStartDate);
        Optional.ofNullable(request.getActualEndDate()).ifPresent(trip::setActualEndDate);

        Optional.ofNullable(request.getActualStartOdometer()).ifPresent(trip::setActualStartOdometer);
        Optional.ofNullable(request.getActualEndOdometer()).ifPresent(trip::setActualEndOdometer);
        
        if (request.getActualStartOdometer() != null && request.getActualEndOdometer() != null) {
            trip.setActualDistanceKm(request.getActualEndOdometer().subtract(request.getActualStartOdometer()));
        }

        Optional.ofNullable(request.getPlannedStartDate()).ifPresent(trip::setPlannedStartDate);
        Optional.ofNullable(request.getPlannedEndDate()).ifPresent(trip::setPlannedEndDate);
        Optional.ofNullable(request.getPlannedDistanceKm()).ifPresent(trip::setPlannedDistanceKm);
        Optional.ofNullable(request.getEstimatedDurationHours()).ifPresent(trip::setEstimatedDurationHours);

        Optional.ofNullable(request.getTollCost()).ifPresent(trip::setTollCost);
        Optional.ofNullable(request.getOtherExpenses()).ifPresent(trip::setOtherExpenses);
        
        Optional.ofNullable(request.getFuelConsumedLiters()).ifPresent(trip::setFuelConsumedLiters);
        Optional.ofNullable(request.getDriverNotes()).ifPresent(trip::setDriverNotes);

        if (request.getStatus() != null && request.getStatus() != trip.getStatus()) {
            tripValidator.validateStatusTransition(trip.getStatus(), request.getStatus());
            trip.setStatus(request.getStatus());
            trip.setLastStatusUpdate(now);
            
            if (request.getStatus() == TripStatus.CANCELLED) {
                trip.setCancelledAt(now);
            }
        }

        trip.setUpdatedAt(now);
        trip.setUpdatedBy(userId);
        
        trip.updateOriginLocationFromComponents();
        trip.updateDestinationLocationFromComponents();

        // If trip has a load, recalculate depot totals
        if (trip.getLoad() != null) {
            trip.getLoad().recalculateDepotTotals();
            loadRepository.save(trip.getLoad());
        }

        Trip saved = tripRepository.save(trip);
        log.info("Updated trip ID: {}", tripId);

        return tripResponseMapper.toResponse(saved);
    }

    /* ========================
       SEARCH
       ======================== */

    /**
     * Search trips with pagination
     */
    @Transactional(readOnly = true)
    public Page<TripResponse> searchTrips(String searchTerm, Pageable pageable) {
        log.debug("Searching trips with term: {}", searchTerm);
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return tripRepository.findAllOrderByIdDesc(pageable)
                    .map(tripResponseMapper::toResponse);
        }
        
        return tripRepository.searchTrips(searchTerm.trim(), pageable)
                .map(tripResponseMapper::toResponse);
    }

    /**
     * Search trips with filters
     */
    @Transactional(readOnly = true)
    public Page<TripResponse> searchTripsWithFilters(
            String searchTerm, 
            TripStatus status, 
            String city, 
            String customer,
            Pageable pageable) {
        
        log.debug("Searching trips with filters - term: {}, status: {}, city: {}, customer: {}", 
                searchTerm, status, city, customer);
        
        return tripRepository.findWithFiltersOrderByIdDesc(searchTerm, status, city, customer, pageable)
                .map(tripResponseMapper::toResponse);
    }

    /**
     * Search trips without pagination
     */
    @Transactional(readOnly = true)
    public List<TripResponse> searchTripsSimple(String searchTerm) {
        log.debug("Simple search trips with term: {}", searchTerm);
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return tripRepository.findAllOrderByIdDesc()
                    .stream()
                    .map(tripResponseMapper::toResponse)
                    .collect(Collectors.toList());
        }
        
        return tripRepository.searchTripsOrderByIdDesc(searchTerm.trim())
                .stream()
                .map(tripResponseMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get active trips
     */
    @Transactional(readOnly = true)
    public List<TripResponse> getActiveTrips() {
        return tripRepository.findActiveTripsOrderByIdDesc()
                .stream()
                .map(tripResponseMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get currently running trips
     */
    @Transactional(readOnly = true)
    public List<TripResponse> getCurrentlyRunningTrips() {
        return tripRepository.findCurrentlyRunningTripsOrderByIdDesc()
                .stream()
                .map(tripResponseMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    /* ========================
       PRIVATE HELPERS
       ======================== */
    private Trip findTripOrThrow(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new TripValidationException("Trip not found with ID: " + id));
    }
}
