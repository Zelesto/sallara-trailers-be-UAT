package com.pgsa.trailers.entity.ops.auto;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;
import java.time.LocalDateTime;

@Getter
public class TripCompletedEvent extends ApplicationEvent {

    private final Long tripId;
    private final LocalDateTime eventTime;
    private final String eventType = "TRIP_COMPLETED";

    public TripCompletedEvent(Long tripId) {
        super(tripId);
        this.tripId = tripId;
        this.eventTime = LocalDateTime.now();
    }

    public TripCompletedEvent(Long tripId, Clock clock) {
        super(tripId);
        this.tripId = tripId;
        this.eventTime = LocalDateTime.now(clock);
    }

    @Override
    public String toString() {
        return String.format("TripCompletedEvent{tripId=%d, eventTime=%s, eventType='%s'}", 
                tripId, eventTime, eventType);
    }
}
