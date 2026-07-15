// src/main/java/com/pgsa/trailers/service/SequenceService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.Sequence;
import com.pgsa.trailers.repository.SequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SequenceService {

    private final SequenceRepository sequenceRepository;

    /**
     * Get the next sequence number for a given table name and year
     * 
     * @param tableName The name of the table (e.g., "trip", "load", "pod")
     * @param year The year (defaults to current year if null)
     * @return The next sequence number
     */
    @Transactional
    public Long getNextSequenceNumber(String tableName, Integer year) {
        if (year == null) {
            year = LocalDateTime.now().getYear();
        }

        log.info("🔢 Getting next sequence number for {} in year {}", tableName, year);

        // Try to find existing sequence
        Optional<Sequence> existing = sequenceRepository.findByTableNameAndYear(tableName, year);

        if (existing.isPresent()) {
            // Increment the next number
            sequenceRepository.incrementNextNumber(tableName, year);
            
            // Get the new number
            Optional<Long> nextNumber = sequenceRepository.findNextNumberByTableNameAndYear(tableName, year);
            
            if (nextNumber.isPresent()) {
                log.info("✅ Next sequence number for {} in year {}: {}", tableName, year, nextNumber.get());
                return nextNumber.get();
            }
            
            // Fallback: get from sequence object
            Sequence sequence = existing.get();
            Long currentNumber = sequence.getNextNumber();
            sequence.setNextNumber(currentNumber + 1);
            sequence.setUpdatedAt(LocalDateTime.now());
            sequenceRepository.save(sequence);
            return currentNumber;
        } else {
            // Create new sequence
            Sequence newSequence = Sequence.builder()
                    .tableName(tableName)
                    .year(year)
                    .nextNumber(2L) // Start at 2 because we're returning 1
                    .build();
            sequenceRepository.save(newSequence);
            log.info("✅ Created new sequence for {} in year {}: 1", tableName, year);
            return 1L;
        }
    }

    /**
     * Get the next sequence number with default year (current year)
     */
    @Transactional
    public Long getNextSequenceNumber(String tableName) {
        return getNextSequenceNumber(tableName, LocalDateTime.now().getYear());
    }

    /**
     * Generate a formatted sequence number (e.g., "TRP-2026-001")
     * 
     * @param tableName The table name
     * @param prefix The prefix (e.g., "TRP", "LOAD", "POD")
     * @param year The year
     * @param padLength The number of digits to pad (default 3)
     * @return Formatted sequence number
     */
    public String generateFormattedSequence(String tableName, String prefix, Integer year, Integer padLength) {
        if (year == null) {
            year = LocalDateTime.now().getYear();
        }
        if (padLength == null) {
            padLength = 3;
        }

        Long nextNumber = getNextSequenceNumber(tableName, year);
        String paddedNumber = String.format("%0" + padLength + "d", nextNumber);
        return String.format("%s-%d-%s", prefix, year, paddedNumber);
    }

    /**
     * Generate a formatted sequence number with default year and padding
     */
    public String generateFormattedSequence(String tableName, String prefix) {
        return generateFormattedSequence(tableName, prefix, null, 3);
    }

    /**
     * Reset a sequence to a specific number (for administrative purposes)
     */
    @Transactional
    public void resetSequence(String tableName, Integer year, Long resetValue) {
        if (year == null) {
            year = LocalDateTime.now().getYear();
        }

        Optional<Sequence> existing = sequenceRepository.findByTableNameAndYear(tableName, year);
        
        if (existing.isPresent()) {
            Sequence sequence = existing.get();
            sequence.setNextNumber(resetValue);
            sequence.setUpdatedAt(LocalDateTime.now());
            sequenceRepository.save(sequence);
            log.info("🔄 Reset sequence for {} in year {} to {}", tableName, year, resetValue);
        } else {
            Sequence newSequence = Sequence.builder()
                    .tableName(tableName)
                    .year(year)
                    .nextNumber(resetValue)
                    .build();
            sequenceRepository.save(newSequence);
            log.info("🔄 Created sequence for {} in year {} with value {}", tableName, year, resetValue);
        }
    }

    /**
     * Check if a sequence exists
     */
    public boolean sequenceExists(String tableName, Integer year) {
        if (year == null) {
            year = LocalDateTime.now().getYear();
        }
        return sequenceRepository.findByTableNameAndYear(tableName, year).isPresent();
    }

    /**
     * Get the current sequence number without incrementing
     */
    public Long getCurrentSequenceNumber(String tableName, Integer year) {
        if (year == null) {
            year = LocalDateTime.now().getYear();
        }
        
        Optional<Sequence> existing = sequenceRepository.findByTableNameAndYear(tableName, year);
        if (existing.isPresent()) {
            return existing.get().getNextNumber() - 1; // Subtract 1 because nextNumber is the next to be used
        }
        return 0L;
    }
}
