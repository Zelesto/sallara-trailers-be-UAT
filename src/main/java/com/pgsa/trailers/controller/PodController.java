// src/main/java/com/pgsa/trailers/controller/PodController.java
package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.DebriefRequestDTO;
import com.pgsa.trailers.dto.PodRequestDTO;
import com.pgsa.trailers.dto.PodResponseDTO;
import com.pgsa.trailers.dto.PodStatistics;
import com.pgsa.trailers.dto.StatusHistoryDTO;
import com.pgsa.trailers.service.PodService;
import com.pgsa.trailers.service.SupabaseStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pods")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*", exposedHeaders = {"Content-Disposition", "Content-Type", "Content-Length"})
public class PodController {

    private final PodService podService;
    private final SupabaseStorageService storageService;

    /**
     * Create a new POD with optional file upload
     */
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<PodResponseDTO> createPod(
            @RequestPart(value = "podData", required = false) PodRequestDTO podRequest,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("📝 Creating new POD with file: {}", file != null ? file.getOriginalFilename() : "No file");
        
        try {
            PodResponseDTO createdPod = podService.createPod(podRequest, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPod);
        } catch (Exception e) {
            log.error("❌ Error creating POD: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Scan a new POD from driver
     */
    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PodResponseDTO> scanPod(
            @RequestParam("tripId") Long tripId,
            @RequestParam(value = "driverName", required = false) String driverName,
            @RequestParam("deliveryDate") String deliveryDate,
            @RequestParam(value = "customerName", required = false) String customerName,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam("file") MultipartFile file) {
        log.info("📸 Scanning POD from driver for trip: {}", tripId);
        
        try {
            PodResponseDTO scannedPod = podService.scanPod(tripId, driverName, deliveryDate, customerName, notes, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(scannedPod);
        } catch (Exception e) {
            log.error("❌ Error scanning POD: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Debrief a POD
     */
    @PostMapping("/{id}/debrief")
    public ResponseEntity<PodResponseDTO> debriefPod(
            @PathVariable Long id,
            @Valid @RequestBody DebriefRequestDTO debriefRequest) {
        log.info("📋 Debriefing POD: {}", id);
        
        try {
            PodResponseDTO debriefedPod = podService.debriefPod(id, debriefRequest);
            return ResponseEntity.ok(debriefedPod);
        } catch (Exception e) {
            log.error("❌ Error debriefing POD {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Re-upload file for existing POD
     */
    @PostMapping(value = "/{id}/reupload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PodResponseDTO> reuploadFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        log.info("📤 Re-uploading file for POD: {}", id);
        log.info("   File: {}, Size: {} bytes, Type: {}", 
            file.getOriginalFilename(), file.getSize(), file.getContentType());
        
        try {
            // Validate file
            if (file.isEmpty()) {
                log.error("❌ File is empty");
                return ResponseEntity.badRequest().build();
            }
            
            // Validate file size (max 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                log.error("❌ File size exceeds 10MB limit: {} bytes", file.getSize());
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(null);
            }
            
            // Validate file type
            String contentType = file.getContentType();
            if (contentType != null && !isValidFileType(contentType)) {
                log.error("❌ Invalid file type: {}", contentType);
                return ResponseEntity.badRequest().build();
            }
            
            PodResponseDTO updatedPod = podService.reuploadFile(id, file);
            return ResponseEntity.ok(updatedPod);
            
        } catch (Exception e) {
            log.error("❌ Error re-uploading file for POD {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
 * Append a document to an existing trip
 */
@PostMapping(value = "/append/{tripId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<PodResponseDTO> appendPodToTrip(
        @PathVariable Long tripId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "notes", required = false) String notes) {
    log.info("📎 Appending POD document to trip: {}", tripId);
    
    try {
        PodResponseDTO appendedPod = podService.appendPodToTrip(tripId, file, notes);
        return ResponseEntity.status(HttpStatus.CREATED).body(appendedPod);
    } catch (Exception e) {
        log.error("❌ Error appending POD to trip {}: {}", tripId, e.getMessage(), e);
        throw e;
    }
}

/**
 * Get all PODs for a trip with document references
 */
@GetMapping("/trip/{tripId}/documents")
public ResponseEntity<Map<String, Object>> getTripDocuments(@PathVariable Long tripId) {
    log.info("📋 Getting all documents for trip: {}", tripId);
    
    try {
        List<PodResponseDTO> pods = podService.getPodsByTrip(tripId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("tripId", tripId);
        response.put("totalDocuments", pods.size());
        response.put("documents", pods);
        
        // Get document references
        List<Map<String, String>> documentRefs = pods.stream()
            .filter(p -> p.getDocumentReference() != null)
            .map(p -> {
                Map<String, String> ref = new HashMap<>();
                ref.put("podNumber", p.getPodNumber());
                ref.put("documentReference", p.getDocumentReference());
                ref.put("fileName", p.getFileName());
                ref.put("status", p.getStatus());
                return ref;
            })
            .collect(Collectors.toList());
        
        response.put("documentReferences", documentRefs);
        
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        log.error("❌ Error getting trip documents: {}", e.getMessage(), e);
        throw e;
    }
}

/**
 * Update debrief notes with default
 */
@PatchMapping("/{id}/debrief-notes")
public ResponseEntity<PodResponseDTO> updateDebriefNotes(
        @PathVariable Long id,
        @RequestBody Map<String, String> request) {
    log.info("📝 Updating debrief notes for POD: {}", id);
    
    try {
        String debriefNotes = request.get("debriefNotes");
        if (debriefNotes == null || debriefNotes.isEmpty()) {
            debriefNotes = "No Endorsements";
        }
        
        Pod pod = podService.getPodEntity(id);
        pod.setDebriefNotes(debriefNotes);
        pod.setUpdatedBy(getCurrentUsername());
        pod.setUpdatedAt(LocalDateTime.now());
        Pod updatedPod = podRepository.save(pod);
        
        return ResponseEntity.ok(podService.mapToResponse(updatedPod));
    } catch (Exception e) {
        log.error("❌ Error updating debrief notes: {}", e.getMessage(), e);
        throw e;
    }
}
    

    /**
     * Download POD document
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadPod(@PathVariable Long id) {
        try {
            log.info("📥 Download request for POD ID: {}", id);
            
            // Get the POD
            PodResponseDTO pod = podService.getPodById(id);
            if (pod == null) {
                log.warn("❌ POD not found: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "POD not found"));
            }
            
            String fileUrl = pod.getFileUrl();
            if (fileUrl == null || fileUrl.isEmpty()) {
                log.warn("❌ No file URL for POD: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No file associated with this POD"));
            }
            
            log.info("📥 File URL from database: {}", fileUrl);
            
            // Check if file exists first
            if (!storageService.fileExists(fileUrl)) {
                log.warn("❌ File does not exist in storage: {}", fileUrl);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "File not found in storage"));
            }
            
            try {
                // Download from Supabase
                byte[] fileData = storageService.downloadFile(fileUrl);
                
                if (fileData == null || fileData.length == 0) {
                    log.warn("❌ File data is empty for POD: {}", id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "File data is empty"));
                }
                
                log.info("✅ File size: {} bytes", fileData.length);
                
                String fileName = pod.getFileName() != null ? pod.getFileName() : pod.getPodNumber() + ".pdf";
                String contentType = getContentType(pod.getDocumentType());
                
                // Return the actual file
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + fileName + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileData.length))
                    .header("Access-Control-Expose-Headers", "Content-Disposition, Content-Type, Content-Length")
                    .body(fileData);
                    
            } catch (Exception e) {
                log.error("❌ Error downloading file from storage: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to download file: " + e.getMessage()));
            }
            
        } catch (Exception e) {
            log.error("❌ Error in download endpoint: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Download failed: " + e.getMessage()));
        }
    }

    /**
     * Get POD file URL (for direct access)
     */
    @GetMapping("/{id}/file-url")
    public ResponseEntity<Map<String, String>> getPodFileUrl(@PathVariable Long id) {
        try {
            log.info("📤 Getting file URL for POD: {}", id);
            
            PodResponseDTO pod = podService.getPodById(id);
            if (pod == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "POD not found"));
            }
            
            String fileUrl = pod.getFileUrl();
            if (fileUrl == null || fileUrl.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No file URL available"));
            }
            
            return ResponseEntity.ok(Map.of(
                "fileUrl", fileUrl,
                "fileName", pod.getFileName() != null ? pod.getFileName() : pod.getPodNumber() + ".pdf"
            ));
            
        } catch (Exception e) {
            log.error("❌ Error getting file URL: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get file URL"));
        }
    }

    /**
     * Get POD by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PodResponseDTO> getPodById(@PathVariable Long id) {
        log.info("📋 Getting POD by ID: {}", id);
        
        try {
            PodResponseDTO pod = podService.getPodById(id);
            return ResponseEntity.ok(pod);
        } catch (Exception e) {
            log.error("❌ Error getting POD {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get POD by POD number
     */
    @GetMapping("/number/{podNumber}")
    public ResponseEntity<PodResponseDTO> getPodByNumber(@PathVariable String podNumber) {
        log.info("📋 Getting POD by number: {}", podNumber);
        
        try {
            PodResponseDTO pod = podService.getPodByNumber(podNumber);
            return ResponseEntity.ok(pod);
        } catch (Exception e) {
            log.error("❌ Error getting POD by number {}: {}", podNumber, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get PODs by Trip
     */
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<PodResponseDTO>> getPodsByTrip(@PathVariable Long tripId) {
        log.info("📋 Getting PODs for trip: {}", tripId);
        
        try {
            List<PodResponseDTO> pods = podService.getPodsByTrip(tripId);
            return ResponseEntity.ok(pods);
        } catch (Exception e) {
            log.error("❌ Error getting PODs for trip {}: {}", tripId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get PODs by Trip with pagination
     */
    @GetMapping("/trip/{tripId}/page")
    public ResponseEntity<Page<PodResponseDTO>> getPodsByTripPaginated(
            @PathVariable Long tripId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("📋 Getting paginated PODs for trip: {}, page: {}, size: {}", 
            tripId, pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<PodResponseDTO> pods = podService.getPodsByTripPaginated(tripId, pageable);
            return ResponseEntity.ok(pods);
        } catch (Exception e) {
            log.error("❌ Error getting paginated PODs for trip {}: {}", tripId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get all PODs with pagination
     */
    @GetMapping
    public ResponseEntity<Page<PodResponseDTO>> getAllPods(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("📋 Getting all PODs, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<PodResponseDTO> pods = podService.getAllPods(pageable);
            return ResponseEntity.ok(pods);
        } catch (Exception e) {
            log.error("❌ Error getting all PODs: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Search PODs
     */
    @GetMapping("/search")
    public ResponseEntity<Page<PodResponseDTO>> searchPods(
            @RequestParam("q") String searchTerm,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("🔍 Searching PODs for: {}", searchTerm);
        
        try {
            Page<PodResponseDTO> results = podService.searchPods(searchTerm, pageable);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("❌ Error searching PODs for '{}': {}", searchTerm, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get PODs by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<PodResponseDTO>> getPodsByStatus(
            @PathVariable String status,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("📋 Getting PODs by status: {}", status);
        
        try {
            Page<PodResponseDTO> pods = podService.getPodsByStatus(status, pageable);
            return ResponseEntity.ok(pods);
        } catch (Exception e) {
            log.error("❌ Error getting PODs by status '{}': {}", status, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Update POD
     */
    @PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<PodResponseDTO> updatePod(
            @PathVariable Long id,
            @RequestPart(value = "podData", required = false) PodRequestDTO podRequest,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("📝 Updating POD: {}", id);
        
        try {
            PodResponseDTO updatedPod = podService.updatePod(id, podRequest);
            return ResponseEntity.ok(updatedPod);
        } catch (Exception e) {
            log.error("❌ Error updating POD {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Update POD status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<PodResponseDTO> updatePodStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate) {
        log.info("📝 Updating POD status: {} to {}", id, statusUpdate.get("status"));
        
        try {
            String status = statusUpdate.get("status");
            if (status == null || status.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            PodResponseDTO updatedPod = podService.updatePodStatus(id, status);
            return ResponseEntity.ok(updatedPod);
        } catch (Exception e) {
            log.error("❌ Error updating POD status {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Verify POD
     */
    @PostMapping("/{id}/verify")
    public ResponseEntity<PodResponseDTO> verifyPod(
            @PathVariable Long id,
            @RequestBody Map<String, String> verifyRequest) {
        log.info("✅ Verifying POD: {}", id);
        
        try {
            String verifiedBy = verifyRequest.get("verifiedBy");
            if (verifiedBy == null || verifiedBy.isEmpty()) {
                verifiedBy = "System";
            }
            
            PodResponseDTO verifiedPod = podService.verifyPod(id, verifiedBy);
            return ResponseEntity.ok(verifiedPod);
        } catch (Exception e) {
            log.error("❌ Error verifying POD {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Reject POD
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<PodResponseDTO> rejectPod(
            @PathVariable Long id,
            @RequestBody Map<String, String> rejectRequest) {
        log.info("❌ Rejecting POD: {}", id);
        
        try {
            String rejectedBy = rejectRequest.get("rejectedBy");
            if (rejectedBy == null || rejectedBy.isEmpty()) {
                rejectedBy = "System";
            }
            
            String reason = rejectRequest.get("reason");
            PodResponseDTO rejectedPod = podService.rejectPod(id, rejectedBy, reason);
            return ResponseEntity.ok(rejectedPod);
        } catch (Exception e) {
            log.error("❌ Error rejecting POD {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get POD status history
     */
    @GetMapping("/{id}/status-history")
    public ResponseEntity<List<StatusHistoryDTO>> getPodStatusHistory(@PathVariable Long id) {
        log.info("📋 Getting status history for POD: {}", id);
        
        try {
            List<StatusHistoryDTO> history = podService.getPodStatusHistory(id);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("❌ Error getting status history for POD {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get POD statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<PodStatistics> getPodStatistics() {
        log.info("📊 Getting POD statistics");
        
        try {
            PodStatistics statistics = podService.getPodStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("❌ Error getting POD statistics: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Delete POD
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePod(@PathVariable Long id) {
        log.info("🗑️ Deleting POD: {}", id);
        
        try {
            podService.deletePod(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("❌ Error deleting POD {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Fix missing files - Mark all PODs without files
     */
    @PostMapping("/fix-missing-files")
    public ResponseEntity<Map<String, Object>> fixMissingFiles() {
        log.info("🔧 Starting batch fix for PODs without files");
        
        try {
            int count = podService.fixMissingFiles();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Fixed " + count + " PODs",
                "count", count
            ));
        } catch (Exception e) {
            log.error("❌ Failed to fix missing files: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "Failed: " + e.getMessage()
                ));
        }
    }

    /**
     * Get content type from document type
     */
    private String getContentType(String documentType) {
        if (documentType == null) return "application/pdf";
        return switch (documentType.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
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
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "POD Service"
        ));
    }
}
