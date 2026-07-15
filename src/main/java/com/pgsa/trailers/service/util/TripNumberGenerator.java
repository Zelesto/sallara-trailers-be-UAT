package com.pgsa.trailers.service.util;

import com.pgsa.trailers.service.SequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Year;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripNumberGenerator {

    private final SequenceService sequenceService;

    /**
     * Generate a trip number in format: TRP-2026-001
     * NEVER returns null - always returns a String
     */
    public String generate() {
        try {
            int year = Year.now().getValue();
            String prefix = "TRP";
            
            // Use SequenceService to generate formatted sequence
            String tripNumber = sequenceService.generateFormattedSequence("trip", prefix, year, 3);
            
            // Safety check - if tripNumber is null, use fallback
            if (tripNumber == null || tripNumber.trim().isEmpty()) {
                log.warn("⚠️ SequenceService returned null or empty, using timestamp fallback");
                tripNumber = "TRP-" + System.currentTimeMillis();
            }
            
            log.info("✅ Generated trip number: {}", tripNumber);
            return tripNumber;
            
        } catch (Exception e) {
            log.error("❌ Error generating trip number: {}", e.getMessage(), e);
            // Ultimate fallback - NEVER return null
            String fallback = "TRP-" + System.currentTimeMillis();
            log.warn("⚠️ Using fallback trip number: {}", fallback);
            return fallback;
        }
    }
}
