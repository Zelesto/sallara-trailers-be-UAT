package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.Sequence;
import com.pgsa.trailers.repository.SequenceRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SequenceService {

    private final SequenceRepository sequenceRepository;

    @PostConstruct
    @Transactional
    public void initSequences() {
        try {
            String currentYear = String.valueOf(Year.now().getValue());
            log.info("🔧 Initializing sequences for year: {}", currentYear);
            
            // Check if trip sequence exists for current year
            if (!sequenceRepository.findByTableNameAndYear("trip", currentYear).isPresent()) {
                log.info("📝 Creating initial sequence for trip in year {}", currentYear);
                Sequence sequence = Sequence.builder()
                        .tableName("trip")
                        .year(currentYear)
                        .nextNumber(1L)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                sequenceRepository.save(sequence);
                log.info("✅ Initial sequence created for trip in year {}", currentYear);
            } else {
                log.info("✅ Sequence already exists for trip in year {}", currentYear);
            }
            
            // Do the same for other sequences
            String[] otherTables = {"load", "pod", "customer", "invoice"};
            for (String tableName : otherTables) {
                if (!sequenceRepository.findByTableNameAndYear(tableName, currentYear).isPresent()) {
                    log.info("📝 Creating sequence for {} in year {}", tableName, currentYear);
                    Sequence sequence = Sequence.builder()
                            .tableName(tableName)
                            .year(currentYear)
                            .nextNumber(1L)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    sequenceRepository.save(sequence);
                    log.info("✅ Sequence created for {} in year {}", tableName, currentYear);
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to initialize sequences: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate a formatted sequence number (e.g., "TRP-2026-001")
     * NEVER returns null - always returns a String
     */
    @Transactional
    public String generateFormattedSequence(String tableName, String prefix, String year, Integer padLength) {
        try {
            if (year == null || year.trim().isEmpty()) {
                year = String.valueOf(Year.now().getValue());
            }
            if (padLength == null) {
                padLength = 3;
            }

            Long nextNumber = getNextSequenceNumber(tableName, year);
            
            if (nextNumber == null) {
                log.warn("⚠️ getNextSequenceNumber returned null, using fallback");
                nextNumber = System.currentTimeMillis() % 1000;
            }
            
            String paddedNumber = String.format("%0" + padLength + "d", nextNumber);
            String result = String.format("%s-%s-%s", prefix, year, paddedNumber);
            
            log.info("✅ Generated formatted sequence: {}", result);
            return result;
            
        } catch (Exception e) {
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
    public Long getNextSequenceNumber(String tableName, String year) {
        try {
            if (year == null || year.trim().isEmpty()) {
                year = String.valueOf(Year.now().getValue());
            }

            log.info("🔢 Getting next sequence number for {} in year {}", tableName, year);

            // Try to find existing sequence
            Sequence sequence = sequenceRepository.findByTableNameAndYear(tableName, year)
                    .orElseGet(() -> {
                        // If not found, create a new one
                        log.info("📝 Creating new sequence for {} in year {}", tableName, year);
                        Sequence newSeq = Sequence.builder()
                                .tableName(tableName)
                                .year(year)
                                .nextNumber(1L)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
                        return sequenceRepository.save(newSeq);
                    });

            Long currentNumber = sequence.getNextNumber();
            log.info("📊 Current sequence value for {}: {}", tableName, currentNumber);
            
            // Increment for next time
            sequence.setNextNumber(currentNumber + 1);
            sequence.setUpdatedAt(LocalDateTime.now());
            sequenceRepository.save(sequence);
            
            log.info("✅ Next sequence number for {} in year {}: {}", tableName, year, currentNumber);
            return currentNumber;
            
        } catch (Exception e) {
            log.error("❌ Failed to get sequence number, using timestamp fallback", e);
            return System.currentTimeMillis() % 1000;
        }
    }
}
