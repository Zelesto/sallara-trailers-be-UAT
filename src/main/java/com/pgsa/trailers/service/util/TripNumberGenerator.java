package com.pgsa.trailers.service.util;

import com.pgsa.trailers.service.SequenceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Year;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripNumberGenerator {

    private final SequenceService sequenceService;
    private static final String TABLE_NAME = "trip";
    private static final String PREFIX = "TRP";
    private static final int PADDING = 3;

    @PostConstruct
    public void init() {
        log.info("✅ TripNumberGenerator initialized with:");
        log.info("   - Table Name: {}", TABLE_NAME);
        log.info("   - Prefix: {}", PREFIX);
        log.info("   - Padding: {}", PADDING);
        log.info("   - Current Year: {}", Year.now().getValue());
    }

    /**
     * Generate a trip number in format: TRP-2026-001
     * NEVER returns null - always returns a String
     */
    public String generate() {
        try {
            String year = String.valueOf(Year.now().getValue());
            log.debug("🔢 Generating trip number for year: {}", year);
            
            // Use SequenceService to generate formatted sequence
            String tripNumber = sequenceService.generateFormattedSequence(
                TABLE_NAME, 
                PREFIX, 
                year, 
                PADDING
            );
            
            // Safety check - if tripNumber is null, use fallback
            if (tripNumber == null || tripNumber.trim().isEmpty()) {
                log.warn("⚠️ SequenceService returned null or empty, using timestamp fallback");
                tripNumber = generateFallback();
            }
            
            log.info("✅ Generated trip number: {}", tripNumber);
            return tripNumber;
            
        } catch (Exception e) {
            log.error("❌ Error generating trip number: {}", e.getMessage(), e);
            // Ultimate fallback - NEVER return null
            String fallback = generateFallback();
            log.warn("⚠️ Using fallback trip number: {}", fallback);
            return fallback;
        }
    }

    /**
     * Generate a fallback trip number using timestamp
     */
    private String generateFallback() {
        return String.format("TRP-FALLBACK-%d", System.currentTimeMillis());
    }
}
