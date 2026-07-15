// src/main/java/com/pgsa/trailers/service/util/TripNumberGenerator.java
package com.pgsa.trailers.service.util;

import com.pgsa.trailers.entity.Sequence;
import com.pgsa.trailers.repository.SequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripNumberGenerator {

    private final SequenceRepository sequenceRepository;

    @Transactional
    public String generate() {
        try {
            int year = Year.now().getValue();
            String prefix = "TRP-" + year + "-";
            
            // Get or create sequence for trips
            Sequence sequence = sequenceRepository.findByTableNameAndYear("trip", year)
                    .orElseGet(() -> {
                        log.info("📝 Creating new sequence for trip in year {}", year);
                        Sequence newSeq = new Sequence();
                        newSeq.setTableName("trip");
                        newSeq.setYear(year);
                        newSeq.setNextNumber(1L);
                        newSeq.setCreatedAt(LocalDateTime.now());
                        newSeq.setUpdatedAt(LocalDateTime.now());
                        return sequenceRepository.save(newSeq);
                    });
            
            Long currentNumber = sequence.getNextNumber();
            
            // Increment for next time
            sequence.setNextNumber(currentNumber + 1);
            sequence.setUpdatedAt(LocalDateTime.now());
            sequenceRepository.save(sequence);
            
            // Format: TRP-2026-001 (3 digits)
            String tripNumber = prefix + String.format("%03d", currentNumber);
            log.info("✅ Generated trip number: {}", tripNumber);
            return tripNumber;
            
        } catch (Exception e) {
            log.error("❌ Error generating trip number from sequence: {}", e.getMessage(), e);
            // Fallback: use timestamp
            String fallback = "TRP-" + System.currentTimeMillis();
            log.warn("⚠️ Using fallback trip number: {}", fallback);
            return fallback;
        }
    }
}
