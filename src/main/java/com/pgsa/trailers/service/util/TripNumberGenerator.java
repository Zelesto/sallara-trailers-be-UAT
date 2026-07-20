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
        log.info("✅ TripNumberGenerator initialized");
        log.info("   - Table Name: {}", TABLE_NAME);
        log.info("   - Prefix: {}", PREFIX);
        log.info("   - Padding: {}", PADDING);
        log.info("   - SequenceService: {}", sequenceService != null ? "injected" : "NULL!");
    }

    /**
     * Generate a trip number in format: TRP-2026-001
     * NEVER returns null - always returns a String
     */
    public String generate() {
        log.info("🔢 generate() called - START");
        
        try {
            String year = String.valueOf(Year.now().getValue());
            log.info("🔢 Current year: {}", year);
            
            log.info("🔢 Calling sequenceService.generateFormattedSequence with:");
            log.info("   - tableName: {}", TABLE_NAME);
            log.info("   - prefix: {}", PREFIX);
            log.info("   - year: {}", year);
            log.info("   - padLength: {}", PADDING);
            
            String tripNumber = sequenceService.generateFormattedSequence(
                TABLE_NAME, 
                PREFIX, 
                year, 
                PADDING
            );
            
            log.info("🔢 sequenceService returned: '{}'", tripNumber);
            
            if (tripNumber == null || tripNumber.trim().isEmpty()) {
                log.error("❌ tripNumber is null or empty!");
                String fallback = "TRP-FALLBACK-" + System.currentTimeMillis();
                log.info("🔄 Using fallback: {}", fallback);
                return fallback;
            }
            
            log.info("✅ Generated trip number: {}", tripNumber);
            return tripNumber;
            
        } catch (Exception e) {
            log.error("❌ Exception in generate(): {}", e.getMessage(), e);
            String fallback = "TRP-FALLBACK-" + System.currentTimeMillis();
            log.info("🔄 Using fallback due to exception: {}", fallback);
            return fallback;
        }
    }
}
