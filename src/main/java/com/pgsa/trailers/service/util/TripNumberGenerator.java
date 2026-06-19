// src/main/java/com/pgsa/trailers/service/util/TripNumberGenerator.java
package com.pgsa.trailers.service.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripNumberGenerator {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public String generate() {
        int year = Year.now().getValue();
        String prefix = "TRP-" + year + "-";
        
        try {
            // Atomically get and increment the sequence
            Long nextNumber = jdbcTemplate.queryForObject(
                "INSERT INTO trip_number_sequence (year, next_number) VALUES (?, 1) " +
                "ON DUPLICATE KEY UPDATE next_number = next_number + 1 " +
                "SELECT next_number - 1 FROM trip_number_sequence WHERE year = ?",
                new Object[]{year, year},
                Long.class
            );
            
            String tripNumber = prefix + String.format("%06d", nextNumber);
            log.debug("Generated trip number: {}", tripNumber);
            return tripNumber;
            
        } catch (Exception e) {
            log.error("Error generating trip number: {}", e.getMessage());
            // Fallback: use timestamp
            String fallback = prefix + System.currentTimeMillis();
            log.warn("Using fallback trip number: {}", fallback);
            return fallback;
        }
    }
}
