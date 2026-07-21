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
public class LoadNumberGenerator {

    private final SequenceService sequenceService;
    private static final String TABLE_NAME = "load";
    private static final String PREFIX = "LOAD";
    private static final int PADDING = 3;

    @PostConstruct
    public void init() {
        log.info("✅ LoadNumberGenerator initialized");
        log.info("   - Table Name: {}", TABLE_NAME);
        log.info("   - Prefix: {}", PREFIX);
        log.info("   - Padding: {}", PADDING);
        log.info("   - SequenceService: {}", sequenceService != null ? "injected" : "NULL!");
    }

    /**
     * Generate a load number in format: LOAD-2026-001
     * NEVER returns null - always returns a String
     */
    public String generate() {
        log.info("🔢 LoadNumberGenerator.generate() called");
        
        try {
            String year = String.valueOf(Year.now().getValue());
            log.info("🔢 Current year: {}", year);
            
            log.info("🔢 Calling sequenceService.generateFormattedSequence with:");
            log.info("   - tableName: {}", TABLE_NAME);
            log.info("   - prefix: {}", PREFIX);
            log.info("   - year: {}", year);
            log.info("   - padLength: {}", PADDING);
            
            String loadNumber = sequenceService.generateFormattedSequence(
                TABLE_NAME, 
                PREFIX, 
                year, 
                PADDING
            );
            
            log.info("🔢 sequenceService returned: '{}'", loadNumber);
            
            if (loadNumber == null || loadNumber.trim().isEmpty()) {
                log.error("❌ loadNumber is null or empty!");
                String fallback = "LOAD-FALLBACK-" + System.currentTimeMillis();
                log.info("🔄 Using fallback: {}", fallback);
                return fallback;
            }
            
            log.info("✅ Generated load number: {}", loadNumber);
            return loadNumber;
            
        } catch (Exception e) {
            log.error("❌ Exception in LoadNumberGenerator.generate(): {}", e.getMessage(), e);
            String fallback = "LOAD-FALLBACK-" + System.currentTimeMillis();
            log.info("🔄 Using fallback due to exception: {}", fallback);
            return fallback;
        }
    }
}
