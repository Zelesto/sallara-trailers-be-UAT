package com.pgsa.trailers.service.util;

import com.pgsa.trailers.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripNumberGenerator {

    private final TripRepository tripRepository;
    private AtomicLong sequence;

    /**
     * Initialize sequence from database on first use
     */
    private synchronized void initSequence() {
        if (sequence == null) {
            // Get the latest trip number and extract the sequence
            Long maxSequence = tripRepository.findMaxTripNumberSequence();
            long startSeq = (maxSequence != null) ? maxSequence + 1 : 1;
            sequence = new AtomicLong(startSeq);
            log.info("Initialized trip number sequence starting at: {}", startSeq);
        }
    }

    public String generate() {
        initSequence();
        long seq = sequence.getAndIncrement();
        int year = Year.now().getValue();
        String tripNumber = "TRP-" + year + "-" + String.format("%06d", seq);
        log.debug("Generated trip number: {}", tripNumber);
        return tripNumber;
    }
}
