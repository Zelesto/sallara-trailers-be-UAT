// src/main/java/com/pgsa/trailers/service/SequenceService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.Sequence;
import com.pgsa.trailers.repository.SequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SequenceService {

    private final SequenceRepository sequenceRepository;

    /**
     * Generate a formatted sequence number (e.g., "TRP-2026-001")
     * NEVER returns null - always returns a String
     */
    @Transactional
    public String generateFormattedSequence(String tableName, String prefix, Integer year, Integer padLength) {
        try {
            if (year == null) {
                year = LocalDateTime.now().getYear();
            }
            if (padLength == null) {
                padLength = 3;
            }

            Long nextNumber = getNextSequenceNumber(tableName, year);
            
            // If for some reason nextNumber is null, use a fallback
            if (nextNumber == null) {
                log.warn("⚠️ getNextSequenceNumber returned null, using fallback");
                nextNumber = System.currentTimeMillis() % 1000;
            }
            
            String paddedNumber = String.format("%0" + padLength + "d", nextNumber);
            String result = String.format("%s-%d-%s", prefix, year, paddedNumber);
            
            log.info("✅ Generated formatted sequence: {}", result);
            return result;
            
        } catch (Exception e) {
            // Ultimate fallback - never return null
            log.error("❌ Sequence generation failed, using timestamp fallback", e);
            return "TRP-" + System.currentTimeMillis();
        }
    }

    @Transactional
    public String generateFormattedSequence(String tableName, String prefix) {
        return generateFormattedSequence(tableName, prefix, null, 3);
    }

    /**
     * Get the next sequence number - never returns null
     */
    @Transactional
    public Long getNextSequenceNumber(String tableName, Integer year) {
        try {
            if (year == null) {
                year = LocalDateTime.now().getYear();
            }

            log.info("🔢 Getting next sequence number for {} in year {}", tableName, year);

            // Try to find existing sequence or create new one
            Sequence sequence = sequenceRepository.findByTableNameAndYear(tableName, year)
                    .orElseGet(() -> {
                        log.info("📝 Creating new sequence for {} in year {}", tableName, year);
                        Sequence newSeq = new Sequence();
                        newSeq.setTableName(tableName);
                        newSeq.setYear(year);
                        newSeq.setNextNumber(1L);
                        newSeq.setCreatedAt(LocalDateTime.now());
                        newSeq.setUpdatedAt(LocalDateTime.now());
                        return sequenceRepository.save(newSeq);
                    });

            Long currentNumber = sequence.getNextNumber();
            log.info("📊 Current sequence value: {}", currentNumber);
            
            // Increment for next time
            sequence.setNextNumber(currentNumber + 1);
            sequence.setUpdatedAt(LocalDateTime.now());
            sequenceRepository.save(sequence);
            
            log.info("✅ Next sequence number for {} in year {}: {}", tableName, year, currentNumber);
            return currentNumber;
            
        } catch (Exception e) {
            log.error("❌ Failed to get sequence number, using timestamp fallback", e);
            // Return a timestamp-based number as fallback
            return System.currentTimeMillis() % 1000;
        }
    }
}
