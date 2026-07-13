// src/main/java/com/pgsa/trailers/controller/PodController.java
package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.PodRequestDTO;
import com.pgsa.trailers.dto.PodResponseDTO;
import com.pgsa.trailers.dto.PodStatistics;
import com.pgsa.trailers.dto.DebriefRequestDTO;
import com.pgsa.trailers.dto.StatusHistoryDTO;
import com.pgsa.trailers.service.PodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/pods")
@RequiredArgsConstructor
@Slf4j
public class PodController {

    private final PodService podService;

    // ============================================
    // CREATE POD - JSON ONLY (No File)
    // ============================================

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PodResponseDTO> createPodJson(@Valid @RequestBody PodRequestDTO request) {
        log.info("Creating new POD from JSON: {}", request);
        try {
            if (request.getTripId() == null) {
                log.error("Trip ID is required");
                return ResponseEntity.badRequest().build();
            }
            if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) {
                log.error("Customer Name is required");
                return ResponseEntity.badRequest().build();
            }
            if (request.getDeliveryDate() == null) {
                log.error("Delivery Date is required");
                return ResponseEntity.badRequest().build();
            }
            PodResponseDTO response = podService.createPod(request, null);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating POD from JSON: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Add to PodController.java

@PostMapping("/{id}/reupload")
public ResponseEntity<?> reuploadFile(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file) {
    try {
        log.info("Received re-upload request for POD ID: {}", id);
        
        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is required");
        }
        
        // Validate file size (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest().body("File size exceeds 10MB limit");
        }
        
        // Validate file type (optional)
        String contentType = file.getContentType();
        if (contentType != null && !isValidFileType(contentType)) {
            return ResponseEntity.badRequest().body("Invalid file type. Supported: PDF, JPG, PNG, DOC, DOCX");
        }
        
        PodResponseDTO updatedPod = podService.reuploadFile(id, file);
        return ResponseEntity.ok(updatedPod);
        
    } catch (RuntimeException e) {
        log.error("Error re-uploading file for POD {}: {}", id, e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Failed to upload file: " + e.getMessage());
    } catch (Exception e) {
        log.error("Unexpected error re-uploading file for POD {}: {}", id, e, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Unexpected error: " + e.getMessage());
    }
}

private boolean isValidFileType(String contentType) {
    if (contentType == null) return false;
    
    String lowerContentType = contentType.toLowerCase();
    return lowerContentType.contains("pdf") ||
           lowerContentType.contains("jpeg") ||
           lowerContentType.contains("jpg") ||
           lowerContentType.contains("png") ||
           lowerContentType.contains("msword") ||
           lowerContentType.contains("wordprocessingml");
}
    
    // ============================================
    // CREATE POD - WITH FILE UPLOAD
    // ============================================

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PodResponseDTO> createPodWithFile(
            @RequestPart("podData") @Valid PodRequestDTO request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("Creating new POD with file: {}", file != null ? file.getOriginalFilename() : "no file");
        
        try {
            if (request == null) {
                log.error("PodRequestDTO is null");
                return ResponseEntity.badRequest().build();
            }
            if (request.getTripId() == null) {
                log.error("Trip ID is required");
                return ResponseEntity.badRequest().build();
            }
            if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) {
                log.error("Customer Name is required");
                return ResponseEntity.badRequest().build();
            }
            if (request.getDeliveryDate() == null) {
                log.error("Delivery Date is required");
                return ResponseEntity.badRequest().build();
            }
            
            // Validate file if present
            if (file != null && !file.isEmpty()) {
                String contentType = file.getContentType();
                long fileSize = file.getSize();
                log.info("File: {}, Type: {}, Size: {} bytes", file.getOriginalFilename(), contentType, fileSize);
                
                // Check file size (limit to 10MB)
                if (fileSize > 10 * 1024 * 1024) {
                    log.error("File size exceeds 10MB limit");
                    return ResponseEntity.badRequest().build();
                }
            }
            
            PodResponseDTO response = podService.createPod(request, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating POD with file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================
    // CREATE POD - FALLBACK (Any content type)
    // ============================================

    @PostMapping
    public ResponseEntity<PodResponseDTO> createPodFallback(@RequestBody PodRequestDTO request) {
        log.info("Creating new POD from fallback: {}", request);
        try {
            if (request.getTripId() == null) {
                log.error("Trip ID is required");
                return ResponseEntity.badRequest().build();
            }
            if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) {
                log.error("Customer Name is required");
                return ResponseEntity.badRequest().build();
            }
            if (request.getDeliveryDate() == null) {
                log.error("Delivery Date is required");
                return ResponseEntity.badRequest().build();
            }
            PodResponseDTO response = podService.createPod(request, null);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating POD from fallback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================
    // SCAN POD
    // ============================================

    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PodResponseDTO> scanPod(
            @RequestParam("tripId") Long tripId,
            @RequestParam("driverName") String driverName,
            @RequestParam("deliveryDate") String deliveryDate,
            @RequestParam(value = "customerName", required = false) String customerName,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam("file") MultipartFile file) {
        log.info("Scanning POD from driver for trip: {}", tripId);
        try {
            if (file == null || file.isEmpty()) {
                log.error("File is required for scanning");
                return ResponseEntity.badRequest().build();
            }
            PodResponseDTO response = podService.scanPod(tripId, driverName, deliveryDate, customerName, notes, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error scanning POD: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================
    // DEBRIEF POD
    // ============================================

    @PostMapping("/{id}/debrief")
    public ResponseEntity<PodResponseDTO> debriefPod(
            @PathVariable Long id,
            @Valid @RequestBody DebriefRequestDTO debriefRequest) {
        log.info("Debriefing POD: {}", id);
        try {
            PodResponseDTO response = podService.debriefPod(id, debriefRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error debriefing POD {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================
    // GET PODS
    // ============================================

    @GetMapping("/{id}")
    public ResponseEntity<PodResponseDTO> getPodById(@PathVariable Long id) {
        log.info("Fetching POD by ID: {}", id);
        try {
            PodResponseDTO response = podService.getPodById(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching POD {}: {}", id, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/number/{podNumber}")
    public ResponseEntity<PodResponseDTO> getPodByNumber(@PathVariable String podNumber) {
        log.info("Fetching POD by number: {}", podNumber);
        try {
            PodResponseDTO response = podService.getPodByNumber(podNumber);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching POD by number {}: {}", podNumber, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<PodResponseDTO>> getPodsByTrip(@PathVariable Long tripId) {
        log.info("Fetching PODs by trip: {}", tripId);
        try {
            List<PodResponseDTO> responses = podService.getPodsByTrip(tripId);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error fetching PODs by trip {}: {}", tripId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/trip/{tripId}/page")
    public ResponseEntity<Page<PodResponseDTO>> getPodsByTripPaginated(
            @PathVariable Long tripId,
            @PageableDefault(size = 10, sort = "deliveryDate", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching paginated PODs by trip: {}", tripId);
        try {
            Page<PodResponseDTO> responses = podService.getPodsByTripPaginated(tripId, pageable);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error fetching paginated PODs by trip {}: {}", tripId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<Page<PodResponseDTO>> getAllPods(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Received request for all PODs with pageable: {}", pageable);
        try {
            Page<PodResponseDTO> result = podService.getAllPods(pageable);
            log.info("Returning {} PODs from total of {}", result.getNumberOfElements(), result.getTotalElements());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error in getAllPods: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Page.empty(pageable));
        }
    }

    // ============================================
    // SEARCH & FILTER PODS
    // ============================================

    @GetMapping("/search")
    public ResponseEntity<Page<PodResponseDTO>> searchPods(
            @RequestParam String q,
            @PageableDefault(size = 10) Pageable pageable) {
        log.info("Searching PODs with query: {}", q);
        try {
            Page<PodResponseDTO> results = podService.searchPods(q, pageable);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error searching PODs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<PodResponseDTO>> getPodsByStatus(
            @PathVariable String status,
            @PageableDefault(size = 10) Pageable pageable) {
        log.info("Fetching PODs by status: {}", status);
        try {
            Page<PodResponseDTO> results = podService.getPodsByStatus(status, pageable);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error fetching PODs by status {}: {}", status, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================
    // STATUS HISTORY
    // ============================================

    @GetMapping("/{id}/status-history")
    public ResponseEntity<List<StatusHistoryDTO>> getPodStatusHistory(@PathVariable Long id) {
        log.info("Fetching status history for POD: {}", id);
        try {
            List<StatusHistoryDTO> history = podService.getPodStatusHistory(id);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching status history for POD {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================
    // DOWNLOAD & VIEW DOCUMENTS
    // ============================================

    @GetMapping("/{id}/download")
public ResponseEntity<?> downloadPod(@PathVariable Long id) {
    try {
        log.info("📥 Download request for POD ID: {}", id);
        
        // Get the POD
        Pod pod = podService.getPodEntity(id);
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
        
        log.info("📥 File URL: {}", fileUrl);
        
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

private String getContentType(String documentType) {
    if (documentType == null) return "application/pdf";
    return switch (documentType.toLowerCase()) {
        case "pdf" -> "application/pdf";
        case "jpg", "jpeg" -> "image/jpeg";
        case "png" -> "image/png";
        case "doc" -> "application/msword";
        case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        default -> "application/octet-stream";
    };
}

    @GetMapping("/{id}/view")
    public ResponseEntity<?> viewPodDocument(@PathVariable Long id) {
        log.info("Viewing POD document: {}", id);
        try {
            String fileUrl = podService.getPodFileUrl(id);
            if (fileUrl == null || fileUrl.isEmpty()) {
                log.warn("No file URL found for POD: {}", id);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(fileUrl);
        } catch (Exception e) {
            log.error("Error viewing POD document {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to view document: " + e.getMessage());
        }
    }

    // ============================================
    // UPDATE POD
    // ============================================

    @PutMapping("/{id}")
    public ResponseEntity<PodResponseDTO> updatePod(
            @PathVariable Long id,
            @Valid @RequestBody PodRequestDTO request) {
        log.info("Updating POD: {}", id);
        try {
            PodResponseDTO response = podService.updatePod(id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating POD {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PodResponseDTO> updatePodStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        log.info("Updating POD {} status to: {}", id, status);
        try {
            PodResponseDTO response = podService.updatePodStatus(id, status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating POD {} status: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================
    // VERIFY & REJECT POD
    // ============================================

    @PostMapping("/{id}/verify")
    public ResponseEntity<PodResponseDTO> verifyPod(
            @PathVariable Long id,
            @RequestParam String verifiedBy) {
        log.info("Verifying POD {} by: {}", id, verifiedBy);
        try {
            PodResponseDTO response = podService.verifyPod(id, verifiedBy);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error verifying POD {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<PodResponseDTO> rejectPod(
            @PathVariable Long id,
            @RequestParam String rejectedBy,
            @RequestParam String reason) {
        log.info("Rejecting POD {} by: {}, reason: {}", id, rejectedBy, reason);
        try {
            PodResponseDTO response = podService.rejectPod(id, rejectedBy, reason);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error rejecting POD {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================
    // DELETE POD
    // ============================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePod(@PathVariable Long id) {
        log.info("Deleting POD: {}", id);
        try {
            podService.deletePod(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting POD {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================
    // STATISTICS
    // ============================================

    @GetMapping("/statistics")
    public ResponseEntity<PodStatistics> getPodStatistics() {
        log.info("Fetching POD statistics");
        try {
            PodStatistics statistics = podService.getPodStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Error fetching POD statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
