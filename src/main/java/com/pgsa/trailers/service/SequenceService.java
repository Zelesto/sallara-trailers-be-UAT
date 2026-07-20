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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SequenceService {

    private final SequenceRepository sequenceRepository;

     @PostConstruct
    @Transactional
    public void initSequences() {
        try {
            int currentYear = Year.now().getValue();
            log.info("🔧 Initializing sequences for year: {}", currentYear);
            
            // First, fix any existing data with commas
            List<Sequence> allSequences = sequenceRepository.findAll();
            for (Sequence seq : allSequences) {
                String yearStr = seq.getYear().toString();
                if (yearStr.contains(",")) {
                    int fixedYear = Integer.parseInt(yearStr.replace(",", ""));
                    log.warn("⚠️ Fixing year for {} from {} to {}", seq.getTableName(), seq.getYear(), fixedYear);
                    seq.setYear(fixedYear);
                    sequenceRepository.save(seq);
                }
            }
            
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
            
            // Do the same for other sequences if needed
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
    public String generateFormattedSequence(String tableName, String prefix, Integer year, Integer padLength) {
        try {
            if (year == null) {
                year = Year.now().getValue();
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
            String result = String.format("%s-%d-%s", prefix, year, paddedNumber);
            
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
     * Handles year with comma (e.g., "2,026") by removing the comma
     */
   @Transactional
public Long getNextSequenceNumber(String tableName, Integer year) {
    try {
        if (year == null) {
            year = Year.now().getValue();
        }

        log.info("🔢 Getting next sequence number for {} in year {}", tableName, year);

        final String finalTableName = tableName;
        final Integer finalYear = year;

        // First, try to find existing sequence with exact match
        Optional<Sequence> exactMatch = sequenceRepository.findByTableNameAndYear(finalTableName, finalYear);
        
        Sequence sequence;
        if (exactMatch.isPresent()) {
            sequence = exactMatch.get();
            log.info("✅ Found exact match for {} in year {}", tableName, year);
        } else {
            // If not found, check for sequences with comma in year
            log.info("📝 No exact match found for {} in year {}, checking for comma values...", finalTableName, finalYear);
            
            // Get all sequences for this table
            List<Sequence> allSequences = sequenceRepository.findByTableName(finalTableName);
            
            // Try to find one with matching year (handling comma)
            String targetYearStr = finalYear.toString();
            Sequence foundSequence = null;
            
            for (Sequence seq : allSequences) {
                String seqYearStr = seq.getYear().toString().replace(",", "");
                if (seqYearStr.equals(targetYearStr)) {
                    foundSequence = seq;
                    log.info("✅ Found sequence with year: {} (stored as {})", seq.getYear(), seqYearStr);
                    
                    // Fix the year to remove comma
                    if (seq.getYear().toString().contains(",")) {
                        log.warn("⚠️ Fixing year from {} to {}", seq.getYear(), seqYearStr);
                        seq.setYear(Integer.parseInt(seqYearStr));
                        sequenceRepository.save(seq);
                    }
                    break;
                }
            }
            
            if (foundSequence != null) {
                sequence = foundSequence;
            } else {
                // If still not found, create a new one
                log.info("📝 Creating new sequence for {} in year {}", finalTableName, finalYear);
                Sequence newSeq = Sequence.builder()
                    .tableName(finalTableName)
                    .year(finalYear)
                    .nextNumber(1L)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
                sequence = sequenceRepository.save(newSeq);
                log.info("✅ Created new sequence: {} - {}", finalTableName, finalYear);
            }
        }

        // Get current number and increment
        Long currentNumber = sequence.getNextNumber();
        log.info("📊 Current sequence value for {}: {}", tableName, currentNumber);
        
        // Increment for next time
        sequence.setNextNumber(currentNumber + 1);
        sequence.setUpdatedAt(LocalDateTime.now());
        sequenceRepository.save(sequence);
        
        log.info("✅ Next sequence number for {} in year {}: {}", tableName, year, currentNumber);
        return currentNumber;
        
    } catch (Exception e) {
        log.error("❌ Failed to get sequence number: {}", e.getMessage(), e);
        // Return a timestamp-based number as fallback
        return System.currentTimeMillis() % 1000;
    }
}
}
