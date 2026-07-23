package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.*;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.entity.ops.TripResponseMapper;
import com.pgsa.trailers.entity.security.AppUser;
import com.pgsa.trailers.enums.TripStatus;
import com.pgsa.trailers.repository.AppUserRepository;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.service.TripService;
import com.pgsa.trailers.service.TripFinalisationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
@Slf4j
public class TripController {

    private final TripService tripService;
    private final TripFinalisationService tripFinalisationService;
    private final AppUserRepository appUserRepository;
    private final TripRepository tripRepository;
    private final TripResponseMapper tripResponseMapper;

    /* ========================
       CREATE
       ======================== */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER')")
    public ResponseEntity<TripResponse> createTrip(
            @RequestBody @Valid CreateTripRequest request,
            Authentication authentication
    ) {
        AppUser user = getAuthenticatedUser(authentication);
        log.debug("Creating trip for user: {}", user.getEmail());

        TripResponse response = tripService.createTrip(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /* ========================
       READ
       ======================== */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER', 'DRIVER')")
    public ResponseEntity<TripResponse> getTrip(@PathVariable Long id) {
        log.debug("Fetching trip id: {}", id);
        return ResponseEntity.ok(tripService.getTrip(id));
    }

    // ============================================================
    // FIX: Updated listTrips to handle status parameter
    // ============================================================
   @GetMapping
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER', 'DRIVER')")
public ResponseEntity<Page<TripResponse>> listTrips(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long customerId,
        @RequestParam(required = false) String city,
        @RequestParam(required = false) String customer,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortOrder,
        Pageable pageable
) {
    log.debug("Listing trips with status: {}, search: {}, customerId: {}, city: {}, customer: {}, pageable: {}", 
        status, search, customerId, city, customer, pageable);
    
    try {
        // Create a custom Pageable with sorting if needed
        Pageable effectivePageable = pageable;
        
        // If sortBy is provided, apply custom sorting
        if (sortBy != null && !sortBy.isEmpty()) {
            Sort.Direction direction = Sort.Direction.DESC;
            if (sortOrder != null && sortOrder.equalsIgnoreCase("ASC")) {
                direction = Sort.Direction.ASC;
            }
            effectivePageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(direction, sortBy)
            );
        }
        
        // Handle search
        if (search != null && !search.trim().isEmpty()) {
            return ResponseEntity.ok(tripService.searchTrips(search.trim(), effectivePageable));
        }
        
        // Handle customer filter (by ID)
        if (customerId != null) {
            return ResponseEntity.ok(tripService.getTripsByCustomerPaginated(customerId, effectivePageable));
        }
        
        // Handle customer filter (by name) - using the customer param from frontend
        if (customer != null && !customer.trim().isEmpty()) {
            // You might need to add this method to TripService
            // return ResponseEntity.ok(tripService.searchTripsByCustomerName(customer.trim(), effectivePageable));
            // For now, use the search as a workaround
            return ResponseEntity.ok(tripService.searchTrips(customer.trim(), effectivePageable));
        }
        
        // Handle status filter (including comma-separated multiple statuses)
        if (status != null && !status.trim().isEmpty()) {
            List<TripStatus> statuses = parseStatuses(status);
            if (!statuses.isEmpty()) {
                Page<Trip> trips = tripRepository.findByStatusIn(statuses, effectivePageable);
                return ResponseEntity.ok(trips.map(tripResponseMapper::toResponse));
            }
        }
        
        // Return all trips
        return ResponseEntity.ok(tripService.listTrips(effectivePageable));
        
    } catch (Exception e) {
        log.error("Error listing trips: {}", e.getMessage(), e);
        // Return empty page instead of throwing
        return ResponseEntity.ok(Page.empty(pageable));
    }
}

    // ============================================================
    // FIX: Updated getTripsWithoutLoad to handle status
    // ============================================================
    @GetMapping("/without-load")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER')")
    public ResponseEntity<Page<TripResponse>> getTripsWithoutLoad(
            @RequestParam(required = false) String status,
            Pageable pageable
    ) {
        log.info("Fetching trips without load assigned, status: {}", status);
        
        // Handle status filter
        if (status != null && !status.trim().isEmpty()) {
            List<TripStatus> statuses = parseStatuses(status);
            if (!statuses.isEmpty()) {
                Page<Trip> trips = tripRepository.findByLoadIdIsNullAndStatusIn(statuses, pageable);
                return ResponseEntity.ok(trips.map(tripResponseMapper::toResponse));
            }
        }
        
        Page<Trip> trips = tripRepository.findByLoadIdIsNull(pageable);
        Page<TripResponse> responses = trips.map(tripResponseMapper::toResponse);
        return ResponseEntity.ok(responses);
    }
    
    /* ========================
       FINALIZE TRIP
       ======================== */
    @PostMapping("/{id}/finalize")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER')")
    public ResponseEntity<Void> finalizeTrip(@PathVariable Long id) {
        log.info("📨 Received finalize request for trip: {}", id);
        try {
            tripFinalisationService.finalizeTrip(id);
            log.info("✅ Trip {} finalized successfully", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ Error finalizing trip {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /* ========================
       CAN FINALIZE CHECK
       ======================== */
    @GetMapping("/{id}/can-finalize")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER')")
    public ResponseEntity<Boolean> canFinalize(@PathVariable Long id) {
        log.info("📨 Checking if trip {} can be finalized", id);
        boolean canFinalize = tripFinalisationService.canFinalize(id);
        return ResponseEntity.ok(canFinalize);
    }

    /* ========================
       UPDATE STATUS
       ======================== */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER')")
    public ResponseEntity<TripResponse> updateTripStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication authentication
    ) {
        log.debug("Updating status for trip {} to {}", id, status);
        
        AppUser user = getAuthenticatedUser(authentication);
        
        TripStatus newStatus;
        try {
            newStatus = TripStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status value: " + status);
        }
        
        TripResponse response = tripService.updateTripStatus(id, newStatus, user.getId());
        return ResponseEntity.ok(response);
    }

    /* ========================
       UPDATE
       ======================== */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER')")
    public ResponseEntity<TripResponse> updateTrip(
            @PathVariable Long id,
            @RequestBody @Valid UpdateTripRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        TripResponse updated = tripService.updateTrip(id, request, user.getId());
        return ResponseEntity.ok(updated);
    }

    /* ========================
       START TRIP (ODO START)
       ======================== */
    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<TripResponse> startTrip(
            @PathVariable Long id,
            @RequestBody @Valid StartTripRequest request,
            Authentication authentication
    ) {
        AppUser user = getAuthenticatedUser(authentication);
        log.debug("Driver {} starting trip {} with odo {}",
                user.getId(), id, request.actualStartOdometer());

        TripResponse response = tripService.startTrip(
                id,
                request.actualStartOdometer(),
                user.getId()
        );

        return ResponseEntity.ok(response);
    }

    /* ========================
       END TRIP (ODO END)
       ======================== */
    @PostMapping("/{id}/end")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<TripResponse> endTrip(
            @PathVariable Long id,
            @RequestBody @Valid EndTripRequest request,
            Authentication authentication
    ) {
        AppUser user = getAuthenticatedUser(authentication);
        log.debug("Driver {} ending trip {} with odo {}",
                user.getId(), id, request.actualEndOdometer());

        TripResponse response = tripService.endTrip(
                id,
                request.actualEndOdometer(),
                user.getId()
        );

        return ResponseEntity.ok(response);
    }

    /* ========================
       DELETE
       ======================== */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrip(@PathVariable Long id) {
        log.debug("Deleting trip id: {}", id);
        tripService.deleteTrip(id);
        log.debug("Trip and associated metrics deleted for id: {}", id);
    }

    /* ========================
       SEARCH
       ======================== */
    @GetMapping("/trips/search")
    public ResponseEntity<Page<TripResponse>> searchTrips(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TripStatus status,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String customer) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        
        if (status != null || city != null || customer != null) {
            return ResponseEntity.ok(
                tripService.searchTripsWithFilters(searchTerm, status, city, customer, pageable)
            );
        }
        
        return ResponseEntity.ok(tripService.searchTrips(searchTerm, pageable));
    }

    @GetMapping("/trips/active")
    public ResponseEntity<List<TripResponse>> getActiveTrips() {
        return ResponseEntity.ok(tripService.getActiveTrips());
    }

    @GetMapping("/trips/running")
    public ResponseEntity<List<TripResponse>> getCurrentlyRunningTrips() {
        return ResponseEntity.ok(tripService.getCurrentlyRunningTrips());
    }
    
    /* ========================
       HELPER METHODS
       ======================== */
    private AppUser getAuthenticatedUser(Authentication authentication) {
        String email = authentication.getName();
        return appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    /**
     * Parse comma-separated status string into list of TripStatus enums
     */
    private List<TripStatus> parseStatuses(String status) {
        List<TripStatus> statuses = new ArrayList<>();
        if (status == null || status.trim().isEmpty()) {
            return statuses;
        }
        
        String[] statusArray = status.split(",");
        for (String s : statusArray) {
            try {
                statuses.add(TripStatus.valueOf(s.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status value: {}, skipping", s);
            }
        }
        return statuses;
    }
}
