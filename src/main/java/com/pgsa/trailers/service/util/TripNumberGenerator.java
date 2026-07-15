// src/main/java/com/pgsa/trailers/service/util/TripNumberGenerator.java
package com.pgsa.trailers.service.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class TripNumberGenerator {

    // Simple counter to ensure uniqueness within the same millisecond
    private static long counter = 0;

    public String generate() {
        try {
            // Format: TRP-20260715-001
            String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            // Simple counter approach - increment and get value
            long count = ++counter;
            String sequencePart = String.format("%03d", count % 1000);
            
            String tripNumber = String.format("TRP-%s-%s", datePart, sequencePart);
            log.info("✅ Generated trip number: {}", tripNumber);
            return tripNumber;
            
        } catch (Exception e) {
            log.error("❌ Error generating trip number: {}", e.getMessage(), e);
            // Ultimate fallback
            String fallback = "TRP-" + System.currentTimeMillis();
            log.warn("⚠️ Using fallback trip number: {}", fallback);
            return fallback;
        }
    }
}
