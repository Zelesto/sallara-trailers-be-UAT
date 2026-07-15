package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.Sequence;
import com.pgsa.trailers.repository.SequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;

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

            // Try to find existing sequence
            Sequence sequence = sequenceRepository.findByTableNameAndYear(finalTableName, finalYear)
                    .orElseGet(() -> {
                        // If not found, try to find by table name and check if year has comma
                        log.info("📝 Sequence not found for {} in year {}, checking for comma values...", finalTableName, finalYear);
                        
                        // Get all sequences for this table and try to find one with matching year (handling comma)
                        var allSequences = sequenceRepository.findByTableName(finalTableName);
                        
                        for (Sequence seq : allSequences) {
                            Integer seqYear = seq.getYear();
                            // Check if the year matches when both are converted to strings and commas removed
                            String seqYearStr = seqYear.toString().replace(",", "");
                            String targetYearStr = finalYear.toString().replace(",", "");
                            
                            if (seqYearStr.equals(targetYearStr)) {
                                log.info("✅ Found sequence with year: {} (stored as {})", seqYear, seqYearStr);
                                return seq;
                            }
                        }
                        
                        // If still not found, create a new one
                        log.info("📝 Creating new sequence for {} in year {}", finalTableName, finalYear);
                        Sequence newSeq = new Sequence();
                        newSeq.setTableName(finalTableName);
                        newSeq.setYear(finalYear);
                        newSeq.setNextNumber(1L);
                        newSeq.setCreatedAt(LocalDateTime.now());
                        newSeq.setUpdatedAt(LocalDateTime.now());
                        return sequenceRepository.save(newSeq);
                    });

            // Double-check: if we found a sequence but the year has a comma, fix it
            String seqYearStr = sequence.getYear().toString();
            if (seqYearStr.contains(",")) {
                log.warn("⚠️ Found sequence year with comma: {}, converting to {}", seqYearStr, seqYearStr.replace(",", ""));
                sequence.setYear(Integer.parseInt(seqYearStr.replace(",", "")));
                sequenceRepository.save(sequence);
            }

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
            // Return a timestamp-based number as fallback
            return System.currentTimeMillis() % 1000;
        }
    }
}
