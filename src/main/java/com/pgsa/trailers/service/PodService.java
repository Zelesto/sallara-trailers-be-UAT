// src/main/java/com/pgsa/trailers/service/PodService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.PodRequestDTO;
import com.pgsa.trailers.dto.PodResponseDTO;
import com.pgsa.trailers.dto.PodStatistics;
import com.pgsa.trailers.dto.DebriefRequestDTO;
import com.pgsa.trailers.dto.StatusHistoryDTO;
import com.pgsa.trailers.entity.ops.Pod;
import com.pgsa.trailers.repository.PodRepository;
import com.pgsa.trailers.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PodService {

    private final PodRepository podRepository;
    private final TripRepository tripRepository;
    private final SupabaseStorageService storageService;
    private final FileConversionService conversionService;
    
    private final String uploadDir = "uploads/pods/";

    /**
     * Get current logged in username
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.warn("Could not get current username: {}", e.getMessage());
        }
        return "System";
    }

    /**
     * Validate file type
     */
    private boolean isValidFileType(String contentType) {
        if (contentType == null) return false;
        
        List<String> validTypes = Arrays.asList(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
        
        return validTypes.stream().anyMatch(contentType::equalsIgnoreCase);
    }

    /**
     * Validate file size (max 10MB)
     */
    private boolean isValidFileSize(MultipartFile file) {
        return file.getSize() <= 10 * 1024 * 1024; // 10MB
    }

    /**
     * Get POD entity by ID (for internal use)
     */
    public Pod getPodEntity(Long id) {
        return podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));
    }

    /**
     * Generate document reference
     */
    private String generateDocumentReference(String podNumber, String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return String.format("%s-%s.%s", podNumber, UUID.randomUUID().toString().substring(0, 8), extension);
    }

    /**
     * Create a new POD with support for appending to existing PODs
     */
    public PodResponseDTO createPod(PodRequestDTO request, MultipartFile file) {
        log.info("Creating POD with request: {}", request);
        log.info("File present: {}, File name: {}, File size: {}", 
            file != null, file != null ? file.getOriginalFilename() : "null", 
            file != null ? file.getSize() : 0);
        
        String currentUser = getCurrentUsername();
        
        // Check if POD already exists for this trip
        if (request.getTripId() != null) {
            List<Pod> existingPods = podRepository.findByTripId(request.getTripId());
            if (!existingPods.isEmpty()) {
                log.info("Found {} existing PODs for trip {}, will append", existingPods.size(), request.getTripId());
            }
        }
        
        // Create POD
        Pod pod = Pod.builder()
                .tripId(request.getTripId())
                .customerName(request.getCustomerName() != null ? request.getCustomerName() : "Adhoc Customer")
                .deliveryDate(request.getDeliveryDate() != null ? request.getDeliveryDate() : LocalDate.now())
                .status(request.getStatus() != null ? request.getStatus() : "UPLOADING")
                .documentType("PDF")
                .notes(request.getNotes())
                .uploadedBy(currentUser)
                .uploadedAt(LocalDateTime.now())
                .updatedBy(currentUser)
                .updatedAt(LocalDateTime.now())
                .createdBy(currentUser)
                .source("UPLOADED")
                .build();
        
        Pod savedPod = podRepository.save(pod);
        
        // Handle file upload
        if (file == null || file.isEmpty()) {
            log.warn("No file provided for POD: {}", savedPod.getPodNumber());
            savedPod.setStatus("MISSING_FILE");
            savedPod.setNotes((savedPod.getNotes() != null ? savedPod.getNotes() + " " : "") + 
                "WARNING: No file uploaded");
            savedPod.setUpdatedBy(currentUser);
            savedPod.setUpdatedAt(LocalDateTime.now());
            savedPod = podRepository.save(savedPod);
            return mapToResponse(savedPod);
        }
        
        try {
            log.info("Attempting to upload file for POD: {}, File: {}", savedPod.getPodNumber(), file.getOriginalFilename());
            
            String fileUrl = storageService.uploadAndConvertFile(file, savedPod.getPodNumber(), conversionService);
            
            if (fileUrl != null && !fileUrl.isEmpty()) {
                // Store document reference
                String documentReference = generateDocumentReference(savedPod.getPodNumber(), file.getOriginalFilename());
                
                savedPod.setFileUrl(fileUrl);
                savedPod.setFileName(savedPod.getPodNumber() + ".pdf");
                savedPod.setFileSize(formatFileSize(file.getSize()));
                savedPod.setDocumentType("PDF");
                savedPod.setDocumentReference(documentReference);
                savedPod.setStatus(request.getStatus() != null ? request.getStatus() : "PENDING");
                savedPod.setUpdatedBy(currentUser);
                savedPod.setUpdatedAt(LocalDateTime.now());
                savedPod = podRepository.save(savedPod);
                log.info("✅ File uploaded successfully for POD: {} with reference: {}", savedPod.getPodNumber(), documentReference);
            } else {
                log.error("❌ File upload returned null URL for POD: {}", savedPod.getPodNumber());
                savedPod.setStatus("UPLOAD_FAILED");
                savedPod.setNotes((savedPod.getNotes() != null ? savedPod.getNotes() + " " : "") + 
                    "ERROR: File upload failed - no URL returned");
                savedPod.setUpdatedBy(currentUser);
                savedPod.setUpdatedAt(LocalDateTime.now());
                savedPod = podRepository.save(savedPod);
            }
            
        } catch (Exception e) {
            log.error("❌ Exception during file upload for POD: {}", savedPod.getPodNumber(), e);
            savedPod.setStatus("UPLOAD_FAILED");
            savedPod.setNotes((savedPod.getNotes() != null ? savedPod.getNotes() + " " : "") + 
                "ERROR: " + e.getMessage());
            savedPod.setUpdatedBy(currentUser);
            savedPod.setUpdatedAt(LocalDateTime.now());
            savedPod = podRepository.save(savedPod);
        }
        
        log.info("POD created with ID: {}, Status: {}", savedPod.getId(), savedPod.getStatus());
        return mapToResponse(savedPod);
    }

    /**
     * Append a new POD document to an existing trip
     */
    public PodResponseDTO appendPodToTrip(Long tripId, MultipartFile file, String notes) {
        log.info("Appending POD document to trip: {}", tripId);
        
        String currentUser = getCurrentUsername();
        
        // Validate trip exists
        if (!tripRepository.existsById(tripId)) {
            throw new RuntimeException("Trip not found with id: " + tripId);
        }
        
        // Get existing PODs for this trip
        List<Pod> existingPods = podRepository.findByTripId(tripId);
        String customerName = existingPods.isEmpty() ? "Adhoc Customer" : existingPods.get(0).getCustomerName();
        int appendCount = existingPods.size() + 1;
        
        // Create new POD for appending
        Pod pod = Pod.builder()
                .tripId(tripId)
                .customerName(customerName)
                .deliveryDate(LocalDate.now())
                .status("PENDING")
                .documentType("PDF")
                .notes(notes != null ? notes : "Appended document #" + appendCount)
                .uploadedBy(currentUser)
                .uploadedAt(LocalDateTime.now())
                .updatedBy(currentUser)
                .updatedAt(LocalDateTime.now())
                .createdBy(currentUser)
                .source("APPENDED")
                .build();
        
        Pod savedPod = podRepository.save(pod);
        
        // Upload file
        if (file != null && !file.isEmpty()) {
            try {
                String fileUrl = storageService.uploadAndConvertFile(file, savedPod.getPodNumber(), conversionService);
                
                if (fileUrl != null && !fileUrl.isEmpty()) {
                    String documentReference = generateDocumentReference(savedPod.getPodNumber(), file.getOriginalFilename());
                    
                    savedPod.setFileUrl(fileUrl);
                    savedPod.setFileName(savedPod.getPodNumber() + ".pdf");
                    savedPod.setFileSize(formatFileSize(file.getSize()));
                    savedPod.setDocumentType("PDF");
                    savedPod.setDocumentReference(documentReference);
                    savedPod.setUpdatedBy(currentUser);
                    savedPod.setUpdatedAt(LocalDateTime.now());
                    savedPod = podRepository.save(savedPod);
                    log.info("✅ Appended file uploaded successfully for POD: {} with reference: {}", savedPod.getPodNumber(), documentReference);
                } else {
                    log.error("❌ File upload returned null URL for appended POD: {}", savedPod.getPodNumber());
                    savedPod.setStatus("UPLOAD_FAILED");
                    savedPod.setUpdatedBy(currentUser);
                    savedPod.setUpdatedAt(LocalDateTime.now());
                    savedPod = podRepository.save(savedPod);
                }
            } catch (Exception e) {
                log.error("❌ Error uploading appended file: {}", e.getMessage(), e);
                savedPod.setStatus("UPLOAD_FAILED");
                savedPod.setUpdatedBy(currentUser);
                savedPod.setUpdatedAt(LocalDateTime.now());
                savedPod = podRepository.save(savedPod);
            }
        } else {
            savedPod.setStatus("MISSING_FILE");
            savedPod.setNotes("No file provided for appended document");
            savedPod.setUpdatedBy(currentUser);
            savedPod.setUpdatedAt(LocalDateTime.now());
            savedPod = podRepository.save(savedPod);
        }
        
        return mapToResponse(savedPod);
    }

    /**
     * Scan a new POD from driver with support for appending
     */
    public PodResponseDTO scanPod(Long tripId, String driverName, String deliveryDate, 
                                   String customerName, String notes, MultipartFile file) {
        log.info("Scanning POD from driver for trip: {}", tripId);
        
        String currentUser = getCurrentUsername();
        
        // Validate trip exists
        if (!tripRepository.existsById(tripId)) {
            throw new RuntimeException("Trip not found with id: " + tripId);
        }

        // Check for existing PODs
        List<Pod> existingPods = podRepository.findByTripId(tripId);
        if (!existingPods.isEmpty()) {
            log.info("Found {} existing PODs for trip {}, appending scanned document", existingPods.size(), tripId);
        }

        // Parse delivery date
        LocalDate parsedDeliveryDate = deliveryDate != null ? 
            LocalDate.parse(deliveryDate, DateTimeFormatter.ISO_LOCAL_DATE) : LocalDate.now();

        // Create POD
        Pod pod = Pod.builder()
                .tripId(tripId)
                .driverName(driverName != null ? driverName : "Unknown Driver")
                .deliveryDate(parsedDeliveryDate)
                .customerName(customerName != null ? customerName : "Adhoc Customer")
                .notes(notes != null ? notes : "Scanned from driver" + (existingPods.isEmpty() ? "" : " (Appended)"))
                .status("SCANNED")
                .source("SCANNED")
                .uploadedBy(currentUser)
                .uploadedAt(LocalDateTime.now())
                .updatedBy(currentUser)
                .updatedAt(LocalDateTime.now())
                .createdBy(currentUser)
                .documentType("PDF")
                .build();

        Pod savedPod = podRepository.save(pod);

        // Upload and convert file
        if (file != null && !file.isEmpty()) {
            try {
                String fileUrl = storageService.uploadAndConvertFile(file, savedPod.getPodNumber(), conversionService);
                if (fileUrl != null && !fileUrl.isEmpty()) {
                    String documentReference = generateDocumentReference(savedPod.getPodNumber(), file.getOriginalFilename());
                    
                    savedPod.setFileUrl(fileUrl);
                    savedPod.setFileName(savedPod.getPodNumber() + ".pdf");
                    savedPod.setFileSize(formatFileSize(file.getSize()));
                    savedPod.setDocumentType("PDF");
                    savedPod.setDocumentReference(documentReference);
                    savedPod.setUpdatedBy(currentUser);
                    savedPod.setUpdatedAt(LocalDateTime.now());
                    savedPod = podRepository.save(savedPod);
                    log.info("Scanned file uploaded and converted for POD: {} with reference: {}", savedPod.getPodNumber(), documentReference);
                } else {
                    log.error("File upload returned null URL for scanned POD: {}", savedPod.getPodNumber());
                    savedPod.setStatus("UPLOAD_FAILED");
                    savedPod.setUpdatedBy(currentUser);
                    savedPod.setUpdatedAt(LocalDateTime.now());
                    savedPod = podRepository.save(savedPod);
                }
            } catch (Exception e) {
                log.error("Failed to upload scanned file for POD: {}", savedPod.getPodNumber(), e);
                savedPod.setStatus("UPLOAD_FAILED");
                savedPod.setUpdatedBy(currentUser);
                savedPod.setUpdatedAt(LocalDateTime.now());
                savedPod = podRepository.save(savedPod);
            }
        } else {
            log.warn("No file provided for scanned POD: {}", savedPod.getPodNumber());
            savedPod.setStatus("MISSING_FILE");
            savedPod.setUpdatedBy(currentUser);
            savedPod.setUpdatedAt(LocalDateTime.now());
            savedPod = podRepository.save(savedPod);
        }

        log.info("POD scanned from driver with ID: {}", savedPod.getId());
        return mapToResponse(savedPod);
    }

    /**
     * Re-upload file for existing POD
     */
    public PodResponseDTO reuploadFile(Long id, MultipartFile file) {
        log.info("========================================");
        log.info("📤 Re-uploading file for POD ID: {}", id);
        log.info("========================================");
        
        String currentUser = getCurrentUsername();
        
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));
        
        log.info("POD Details - Number: {}, Current Status: {}, File URL: {}", 
            pod.getPodNumber(), pod.getStatus(), pod.getFileUrl());
        
        if (file == null || file.isEmpty()) {
            log.error("❌ File is null or empty");
            throw new RuntimeException("File is required");
        }
        
        log.info("File Details - Name: {}, Size: {} bytes, Type: {}", 
            file.getOriginalFilename(), file.getSize(), file.getContentType());
        
        try {
            // Delete old file if exists
            if (pod.getFileUrl() != null && !pod.getFileUrl().isEmpty()) {
                log.info("Deleting old file: {}", pod.getFileUrl());
                try {
                    storageService.deleteFile(pod.getFileUrl());
                    log.info("✅ Old file deleted successfully");
                } catch (Exception e) {
                    log.warn("⚠️ Failed to delete old file: {}", e.getMessage());
                }
            }
            
            // Upload new file
            log.info("📤 Uploading new file...");
            String fileUrl = storageService.uploadAndConvertFile(file, pod.getPodNumber(), conversionService);
            
            if (fileUrl == null || fileUrl.isEmpty()) {
                log.error("❌ File upload returned null URL");
                throw new RuntimeException("File upload failed - no URL returned");
            }
            
            log.info("✅ File uploaded successfully: {}", fileUrl);
            
            // Generate new document reference
            String documentReference = generateDocumentReference(pod.getPodNumber(), file.getOriginalFilename());
            
            // Update POD with new file info
            pod.setFileUrl(fileUrl);
            pod.setFileName(pod.getPodNumber() + ".pdf");
            pod.setFileSize(formatFileSize(file.getSize()));
            pod.setDocumentType("PDF");
            pod.setDocumentReference(documentReference);
            pod.setStatus("PENDING");
            pod.setUpdatedBy(currentUser);
            pod.setUpdatedAt(LocalDateTime.now());
            pod.setNotes((pod.getNotes() != null ? pod.getNotes() + " " : "") + 
                "File re-uploaded on " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + 
                " by " + currentUser);
            
            Pod updatedPod = podRepository.save(pod);
            log.info("✅ POD {} updated successfully with new file and reference: {}", pod.getPodNumber(), documentReference);
            log.info("========================================");
            
            return mapToResponse(updatedPod);
            
        } catch (Exception e) {
            log.error("❌ Failed to re-upload file for POD: {}", pod.getPodNumber(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    /**
     * Fix missing files - Mark all PODs without files
     */
    @Transactional
    public int fixMissingFiles() {
        log.info("🔧 Starting batch fix for PODs without files");
        
        List<Pod> podsWithoutFiles = podRepository.findByFileUrlIsNullOrFileUrlIsEmpty();
        int count = podsWithoutFiles.size();
        
        log.info("Found {} PODs without files", count);
        
        String currentUser = getCurrentUsername();
        
        for (Pod pod : podsWithoutFiles) {
            try {
                // Mark as missing file
                pod.setStatus("MISSING_FILE");
                pod.setNotes((pod.getNotes() != null ? pod.getNotes() + " " : "") + 
                    "MIGRATION: File missing. Please re-upload.");
                pod.setUpdatedAt(LocalDateTime.now());
                pod.setUpdatedBy(currentUser);
                podRepository.save(pod);
                
                log.info("✅ Marked POD {} (ID: {}) as MISSING_FILE", pod.getPodNumber(), pod.getId());
            } catch (Exception e) {
                log.error("❌ Failed to update POD {}: {}", pod.getPodNumber(), e.getMessage());
            }
        }
        
        log.info("✅ Batch fix complete. Updated {} PODs", count);
        return count;
    }

    /**
     * Debrief a POD with default values and current user tracking
     */
    /**
 * Debrief a POD with default values and current user tracking
 */
public PodResponseDTO debriefPod(Long id, DebriefRequestDTO debriefRequest) {
    log.info("Debriefing POD: {}", id);
    
    String currentUser = getCurrentUsername();
    
    Pod pod = podRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));

    // Set status with default if not provided
    pod.setStatus(debriefRequest.getStatus() != null ? debriefRequest.getStatus() : "DELIVERED");
    
    // Set notes with default if not provided
    pod.setNotes(debriefRequest.getNotes() != null ? debriefRequest.getNotes() : pod.getNotes());
    
    // Set received by with default if not provided
    pod.setReceivedBy(debriefRequest.getReceivedBy() != null ? debriefRequest.getReceivedBy() : currentUser);
    
    // Set quality rating with default
    pod.setQualityRating(debriefRequest.getQualityRating() != null ? debriefRequest.getQualityRating() : 3);
    
    // Set issues found - now String directly
    if (debriefRequest.getIssuesFound() != null && !debriefRequest.getIssuesFound().isEmpty()) {
        pod.setIssuesFound(debriefRequest.getIssuesFound());
    } else {
        pod.setIssuesFound("None");
    }
    
    // Set additional info with default
    pod.setAdditionalInfo(debriefRequest.getAdditionalInfo() != null ? debriefRequest.getAdditionalInfo() : "N/A");
    
    // Set delivery condition with default
    pod.setDeliveryCondition(debriefRequest.getDeliveryCondition() != null ? 
        debriefRequest.getDeliveryCondition() : "Good");
    
    // Set debrief notes with default "No Endorsements"
    pod.setDebriefNotes(debriefRequest.getDebriefNotes() != null && !debriefRequest.getDebriefNotes().isEmpty() ? 
        debriefRequest.getDebriefNotes() : "No Endorsements");
    
    // Set debriefed by to current user
    pod.setDebriefedBy(currentUser);
    pod.setDebriefedAt(LocalDateTime.now());
    
    // Set updated by to current user
    pod.setUpdatedBy(currentUser);
    pod.setUpdatedAt(LocalDateTime.now());

    Pod updatedPod = podRepository.save(pod);
    log.info("POD {} debriefed with status: {} by: {}", id, pod.getStatus(), currentUser);
    return mapToResponse(updatedPod);
}

    /**
     * Get POD status history
     */
    @Transactional(readOnly = true)
    public List<StatusHistoryDTO> getPodStatusHistory(Long id) {
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));

        List<StatusHistoryDTO> history = new ArrayList<>();
        
        history.add(StatusHistoryDTO.builder()
                .status(pod.getStatus() != null ? pod.getStatus() : "CREATED")
                .notes("POD created")
                .updatedBy(pod.getUploadedBy() != null ? pod.getUploadedBy() : "System")
                .timestamp(pod.getCreatedAt() != null ? pod.getCreatedAt() : LocalDateTime.now())
                .build());

        if (pod.getUpdatedAt() != null && pod.getCreatedAt() != null && 
            pod.getUpdatedAt().isAfter(pod.getCreatedAt())) {
            history.add(StatusHistoryDTO.builder()
                    .status(pod.getStatus())
                    .notes("Status updated to: " + pod.getStatus())
                    .updatedBy(pod.getUpdatedBy() != null ? pod.getUpdatedBy() : "System")
                    .timestamp(pod.getUpdatedAt())
                    .build());
        }

        if (pod.getVerifiedAt() != null) {
            history.add(StatusHistoryDTO.builder()
                    .status("VERIFIED")
                    .notes("POD verified")
                    .updatedBy(pod.getVerifiedBy())
                    .timestamp(pod.getVerifiedAt())
                    .build());
        }

        if (pod.getRejectedAt() != null) {
            history.add(StatusHistoryDTO.builder()
                    .status("REJECTED")
                    .notes("POD rejected: " + (pod.getRejectionReason() != null ? pod.getRejectionReason() : ""))
                    .updatedBy(pod.getRejectedBy())
                    .timestamp(pod.getRejectedAt())
                    .build());
        }

        if (pod.getDebriefedAt() != null) {
            history.add(StatusHistoryDTO.builder()
                    .status(pod.getStatus())
                    .notes(pod.getDebriefNotes() != null ? pod.getDebriefNotes() : "POD debriefed")
                    .updatedBy(pod.getDebriefedBy())
                    .timestamp(pod.getDebriefedAt())
                    .build());
        }

        return history;
    }

    /**
     * Download POD document
     */
    @Transactional(readOnly = true)
    public String downloadPodDocument(Long id) {
        return getPodFileUrl(id);
    }
    
    /**
     * Get POD file URL
     */
    @Transactional(readOnly = true)
    public String getPodFileUrl(Long id) {
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));
        
        if (pod.getFileUrl() == null || pod.getFileUrl().isEmpty()) {
            log.warn("No file URL found for POD ID: {}", id);
            return null;
        }
        
        log.info("File URL for POD {}: {}", id, pod.getFileUrl());
        return pod.getFileUrl();
    }

    /**
     * Get POD filename
     */
    @Transactional(readOnly = true)
    public String getPodFilename(Long id) {
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));
        return pod.getFileName() != null ? pod.getFileName() : "pod-document.pdf";
    }

    /**
     * Get POD content type
     */
    @Transactional(readOnly = true)
    public String getPodContentType(Long id) {
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));
        
        String extension = pod.getDocumentType() != null ? pod.getDocumentType().toLowerCase() : "pdf";
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "pdf";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Format file size
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Get POD by ID
     */
    @Transactional(readOnly = true)
    public PodResponseDTO getPodById(Long id) {
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));
        return mapToResponse(pod);
    }

    /**
     * Get POD by number
     */
    @Transactional(readOnly = true)
    public PodResponseDTO getPodByNumber(String podNumber) {
        Pod pod = podRepository.findByPodNumber(podNumber)
                .orElseThrow(() -> new RuntimeException("POD not found with number: " + podNumber));
        return mapToResponse(pod);
    }

    /**
     * Get PODs by Trip
     */
    @Transactional(readOnly = true)
    public List<PodResponseDTO> getPodsByTrip(Long tripId) {
        try {
            List<Pod> pods = podRepository.findByTripId(tripId);
            if (pods == null || pods.isEmpty()) {
                return new ArrayList<>();
            }
            return pods.stream()
                    .map(this::mapToResponseSafe)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching PODs by trip {}: {}", tripId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get PODs by Trip with pagination
     */
    @Transactional(readOnly = true)
    public Page<PodResponseDTO> getPodsByTripPaginated(Long tripId, Pageable pageable) {
        return podRepository.findByTripId(tripId, pageable)
                .map(this::mapToResponseSafe);
    }

    /**
     * Get all PODs with pagination
     */
    public Page<PodResponseDTO> getAllPods(Pageable pageable) {
    log.info("Fetching all PODs with pageable: {}", pageable);
    try {
        Page<Pod> podPage = podRepository.findAll(pageable);
        log.info("Found {} PODs total, {} in this page", podPage.getTotalElements(), podPage.getNumberOfElements());
        
        if (podPage.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // Map each pod safely with try-catch for each item
        List<PodResponseDTO> dtos = new ArrayList<>();
        for (Pod pod : podPage.getContent()) {
            try {
                PodResponseDTO dto = mapToResponseSafe(pod);
                if (dto != null) {
                    dtos.add(dto);
                } else {
                    log.warn("Failed to map POD {} to DTO, skipping", pod.getId());
                }
            } catch (Exception e) {
                log.error("Error mapping POD {}: {}", pod.getId(), e.getMessage(), e);
                // Continue with next pod
            }
        }
        
        return new PageImpl<>(dtos, pageable, podPage.getTotalElements());
        
    } catch (Exception e) {
        log.error("Error fetching PODs: {}", e.getMessage(), e);
        // Return empty page instead of throwing
        return Page.empty(pageable);
    }
}

    /**
     * Search PODs
     */
    @Transactional(readOnly = true)
    public Page<PodResponseDTO> searchPods(String searchTerm, Pageable pageable) {
        return podRepository.searchPods(searchTerm, pageable)
                .map(this::mapToResponseSafe);
    }

    /**
     * Get PODs by status
     */
    @Transactional(readOnly = true)
    public Page<PodResponseDTO> getPodsByStatus(String status, Pageable pageable) {
        return podRepository.findByStatus(status, pageable)
                .map(this::mapToResponseSafe);
    }

    /**
     * Update POD
     */
    public PodResponseDTO updatePod(Long id, PodRequestDTO request) {
        String currentUser = getCurrentUsername();
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));

        pod.setTripId(request.getTripId());
        pod.setCustomerName(request.getCustomerName());
        pod.setDeliveryDate(request.getDeliveryDate());
        pod.setStatus(request.getStatus());
        pod.setDocumentType(request.getDocumentType());
        pod.setFileSize(request.getFileSize());
        pod.setFileUrl(request.getFileUrl());
        pod.setFileName(request.getFileName());
        pod.setNotes(request.getNotes());
        pod.setUpdatedBy(currentUser);
        pod.setUpdatedAt(LocalDateTime.now());

        Pod updated = podRepository.save(pod);
        log.info("POD updated with ID: {} by: {}", updated.getId(), currentUser);
        return mapToResponse(updated);
    }

    /**
     * Update POD status
     */
    public PodResponseDTO updatePodStatus(Long id, String status) {
        String currentUser = getCurrentUsername();
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));

        pod.setStatus(status);
        pod.setUpdatedBy(currentUser);
        pod.setUpdatedAt(LocalDateTime.now());
        Pod updated = podRepository.save(pod);
        log.info("POD {} status updated to: {} by: {}", id, status, currentUser);
        return mapToResponse(updated);
    }

    /**
     * Verify POD
     */
    public PodResponseDTO verifyPod(Long id, String verifiedBy) {
        String currentUser = getCurrentUsername();
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));

        pod.setStatus("VERIFIED");
        pod.setVerifiedBy(verifiedBy != null ? verifiedBy : currentUser);
        pod.setVerifiedAt(LocalDateTime.now());
        pod.setUpdatedBy(currentUser);
        pod.setUpdatedAt(LocalDateTime.now());
        Pod updated = podRepository.save(pod);
        log.info("POD {} verified by: {}", id, verifiedBy != null ? verifiedBy : currentUser);
        return mapToResponse(updated);
    }

    /**
     * Reject POD
     */
    public PodResponseDTO rejectPod(Long id, String rejectedBy, String reason) {
        String currentUser = getCurrentUsername();
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));

        pod.setStatus("REJECTED");
        pod.setRejectedBy(rejectedBy != null ? rejectedBy : currentUser);
        pod.setRejectedAt(LocalDateTime.now());
        pod.setRejectionReason(reason != null ? reason : "No reason provided");
        pod.setUpdatedBy(currentUser);
        pod.setUpdatedAt(LocalDateTime.now());
        Pod updated = podRepository.save(pod);
        log.info("POD {} rejected by: {}, reason: {}", id, rejectedBy != null ? rejectedBy : currentUser, reason);
        return mapToResponse(updated);
    }

    /**
     * Delete POD
     */
    public void deletePod(Long id) {
        if (!podRepository.existsById(id)) {
            throw new RuntimeException("POD not found with ID: " + id);
        }
        podRepository.deleteById(id);
        log.info("POD deleted with ID: {}", id);
    }

    /**
     * Get POD statistics
     */
    public PodStatistics getPodStatistics() {
        long total = podRepository.count();
        long pending = podRepository.countByStatus("PENDING");
        long delivered = podRepository.countByStatus("DELIVERED");
        long verified = podRepository.countByStatus("VERIFIED");
        long rejected = podRepository.countByStatus("REJECTED");
        long scanned = podRepository.countBySource("SCANNED");
        long appended = podRepository.countBySource("APPENDED");
        
        long pendingDebrief = podRepository.countPendingDebrief();
        
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        long scannedToday = podRepository.countScannedSince(startOfDay);

        return PodStatistics.builder()
                .total(total)
                .pending(pending)
                .delivered(delivered)
                .verified(verified)
                .rejected(rejected)
                .scanned(scanned)
                .pendingDebrief(pendingDebrief)
                .scannedToday(scannedToday)
                .build();
    }

    /**
     * Get trip number safely using the repository method
     */
    private String getTripNumber(Long tripId) {
        if (tripId == null) {
            return null;
        }
        try {
            Optional<String> result = tripRepository.findTripNumberById(tripId);
            return result.orElse(null);
        } catch (Exception e) {
            log.warn("Could not find trip number for trip ID: {}", tripId, e);
            return "TRIP-" + tripId;
        }
    }

    /**
     * Convert issuesFound from String to List<String>
     */
   private List<String> convertIssuesToList(String issuesFound) {
    if (issuesFound == null || issuesFound.isEmpty()) {
        return null;
    }
    
    String trimmed = issuesFound.trim();
    if (trimmed.equals("None") || trimmed.equals("N/A") || trimmed.equals("null")) {
        return null;
    }
    
    // If it contains commas, split it
    if (trimmed.contains(",")) {
        List<String> result = Arrays.stream(trimmed.split("\\s*,\\s*"))
                .filter(s -> !s.isEmpty() && !s.equals("None") && !s.equals("N/A") && !s.equals("null"))
                .collect(Collectors.toList());
        return result.isEmpty() ? null : result;
    }
    
    // Single issue
    return List.of(trimmed);
}

    /**
     * Safely map Pod to response DTO - handles all exceptions
     */
    private PodResponseDTO mapToResponseSafe(Pod pod) {
    if (pod == null) {
        return null;
    }
    
    try {
        String tripNumber = null;
        try {
            tripNumber = getTripNumber(pod.getTripId());
        } catch (Exception e) {
            log.warn("Could not get trip number for trip {}: {}", pod.getTripId(), e.getMessage());
        }
        
        // Convert issuesFound from String to List<String>
        List<String> issuesList = null;
        try {
            issuesList = convertIssuesToList(pod.getIssuesFound());
        } catch (Exception e) {
            log.warn("Could not convert issuesFound for POD {}: {}", pod.getId(), e.getMessage());
        }
        
        return PodResponseDTO.builder()
                .id(pod.getId())
                .podNumber(pod.getPodNumber() != null ? pod.getPodNumber() : "N/A")
                .tripId(pod.getTripId())
                .tripNumber(tripNumber)
                .customerName(pod.getCustomerName() != null ? pod.getCustomerName() : "N/A")
                .driverName(pod.getDriverName())
                .deliveryDate(pod.getDeliveryDate())
                .status(pod.getStatus() != null ? pod.getStatus() : "PENDING")
                .source(pod.getSource() != null ? pod.getSource() : "UPLOADED")
                .documentType(pod.getDocumentType())
                .fileSize(pod.getFileSize())
                .fileUrl(pod.getFileUrl())
                .fileName(pod.getFileName())
                .documentReference(pod.getDocumentReference())
                .notes(pod.getNotes())
                .uploadedBy(pod.getUploadedBy())
                .uploadedAt(pod.getUploadedAt())
                .verifiedBy(pod.getVerifiedBy())
                .verifiedAt(pod.getVerifiedAt())
                .rejectedBy(pod.getRejectedBy())
                .rejectedAt(pod.getRejectedAt())
                .rejectionReason(pod.getRejectionReason())
                .debriefedAt(pod.getDebriefedAt())
                .debriefedBy(pod.getDebriefedBy())
                .receivedBy(pod.getReceivedBy())
                .qualityRating(pod.getQualityRating())
                 .issuesFound(pod.getIssuesFound())
                .deliveryCondition(pod.getDeliveryCondition())
                .debriefNotes(pod.getDebriefNotes())
                .additionalInfo(pod.getAdditionalInfo())
                .createdAt(pod.getCreatedAt())
                .createdBy(pod.getCreatedBy())
                .updatedAt(pod.getUpdatedAt())
                .updatedBy(pod.getUpdatedBy())
                .build();
                
    } catch (Exception e) {
        log.error("Error in mapToResponseSafe for pod {}: {}", pod.getId(), e.getMessage(), e);
        // Return a minimal DTO instead of null to avoid breaking the list
        return PodResponseDTO.builder()
                .id(pod.getId())
                .podNumber(pod.getPodNumber() != null ? pod.getPodNumber() : "N/A")
                .status("ERROR")
                .build();
    }
}

    /**
     * Original mapToResponse method - delegates to safe version
     */
    private PodResponseDTO mapToResponse(Pod pod) {
        return mapToResponseSafe(pod);
    }
}
