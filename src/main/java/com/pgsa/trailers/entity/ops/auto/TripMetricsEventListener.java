package com.pgsa.trailers.entity.ops.auto;

import com.pgsa.trailers.dto.RouteCalculationRequestDTO;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.service.TripMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripMetricsEventListener {

    private final TripRepository tripRepository;
    private final TripMetricsService metricsService;

    @Async  // <-- ADD THIS
    @EventListener
    @Transactional
    public void onTripPlanned(TripPlannedEvent event) {
        try {
            Trip trip = loadTrip(event.getTripId());

            log.info("Calculating estimated metrics for trip {}", trip.getId());

            // Check if coordinates are already provided - skip if they are
            if (trip.getOriginLatitude() != null && trip.getOriginLongitude() != null &&
                trip.getDestinationLatitude() != null && trip.getDestinationLongitude() != null) {
                log.info("Trip {} already has coordinates, skipping route calculation", trip.getId());
                return;
            }

            metricsService.calculateAndSaveMetrics(
                    trip.getId(),
                    RouteCalculationRequestDTO.fromTrip(trip)
            );
            
            log.info("Route metrics calculated successfully for trip {}", trip.getId());
            
        } catch (Exception ex) {
            // CRITICAL: Don't let this exception bubble up
            // The trip is already created, we just log the error
            log.error("Failed to calculate metrics for trip {}: {}", 
                event.getTripId(), ex.getMessage(), ex);
        }
    }

    @Async  // <-- ADD THIS
    @EventListener
    @Transactional
    public void onTripCompleted(TripCompletedEvent event) {
        try {
            Trip trip = loadTrip(event.getTripId());

            log.info("Finalizing metrics for completed trip {}", trip.getId());

            metricsService.lockFinalMetrics(trip.getId());
            
        } catch (Exception ex) {
            log.error("Failed to finalize metrics for trip {}: {}", 
                event.getTripId(), ex.getMessage(), ex);
        }
    }

    private Trip loadTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));
    }
}
