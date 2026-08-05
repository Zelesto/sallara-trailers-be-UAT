package com.pgsa.trailers.controller;

import com.pgsa.trailers.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final SupabaseStorageService storageService;
    
    // In-memory storage for document metadata (replace with database in production)
    private final Map<String, List<DocumentMetadata>> driverDocuments = new ConcurrentHashMap<>();

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("driverId") Long driverId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "description", required = false) String description) {
        
        try {
            log.info("📤 Uploading document for driver: {}", driverId);
            log.info("   File: {}, Size: {} bytes, Type: {}", 
                file.getOriginalFilename(), file.getSize(), documentType);
            
            // Generate unique filename
            String podNumber = String.format("DRIVER_%d_%d", driverId, System.currentTimeMillis());
            
            // Upload to Supabase
            String fileUrl = storageService.uploadFile(file, podNumber);
            
            // Create metadata
            DocumentMetadata metadata = DocumentMetadata.builder()
                    .id(UUID.randomUUID().toString())
                    .driverId(driverId)
                    .fileName(file.getOriginalFilename())
                    .fileUrl(fileUrl)
                    .documentType(documentType)
                    .description(description)
                    .fileSize(file.getSize())
                    .fileType(file.getContentType())
                    .uploadedAt(LocalDateTime.now())
                    .build();
            
            // Save metadata
            String key = String.valueOf(driverId);
            driverDocuments.computeIfAbsent(key, k -> new ArrayList<>()).add(metadata);
            
            log.info("✅ Document uploaded successfully: {}", metadata.getId());
            return ResponseEntity.ok(metadata);
            
        } catch (Exception e) {
            log.error("❌ Error uploading document: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to upload document: " + e.getMessage()));
        }
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<?> getDriverDocuments(@PathVariable Long driverId) {
        try {
            log.info("📥 Fetching documents for driver: {}", driverId);
            
            String key = String.valueOf(driverId);
            List<DocumentMetadata> documents = driverDocuments.getOrDefault(key, new ArrayList<>());
            
            log.info("✅ Found {} documents for driver {}", documents.size(), driverId);
            return ResponseEntity.ok(documents);
            
        } catch (Exception e) {
            log.error("❌ Error fetching documents: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch documents: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> deleteDocument(@PathVariable String documentId) {
        try {
            log.info("🗑️ Deleting document: {}", documentId);
            
            // Find and remove metadata
            for (List<DocumentMetadata> docs : driverDocuments.values()) {
                Optional<DocumentMetadata> found = docs.stream()
                        .filter(d -> d.getId().equals(documentId))
                        .findFirst();
                
                if (found.isPresent()) {
                    DocumentMetadata doc = found.get();
                    
                    // Delete from storage
                    storageService.deleteFile(doc.getFileUrl());
                    
                    // Remove from list
                    docs.remove(doc);
                    
                    log.info("✅ Document deleted: {}", documentId);
                    return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
                }
            }
            
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("❌ Error deleting document: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete document: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{documentId}")
    public ResponseEntity<?> downloadDocument(@PathVariable String documentId) {
        try {
            log.info("📥 Downloading document: {}", documentId);
            
            // Find metadata
            for (List<DocumentMetadata> docs : driverDocuments.values()) {
                Optional<DocumentMetadata> found = docs.stream()
                        .filter(d -> d.getId().equals(documentId))
                        .findFirst();
                
                if (found.isPresent()) {
                    DocumentMetadata doc = found.get();
                    
                    // Download from storage
                    byte[] fileData = storageService.downloadFile(doc.getFileUrl());
                    
                    if (fileData == null) {
                        return ResponseEntity.notFound().build();
                    }
                    
                    // Determine content type
                    String contentType = doc.getFileType();
                    if (contentType == null || contentType.isEmpty()) {
                        contentType = "application/octet-stream";
                    }
                    
                    log.info("✅ Document downloaded: {}, size: {} bytes", 
                        doc.getFileName(), fileData.length);
                    
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CONTENT_DISPOSITION, 
                                "attachment; filename=\"" + doc.getFileName() + "\"")
                            .body(fileData);
                }
            }
            
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("❌ Error downloading document: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to download document: " + e.getMessage()));
        }
    }

    @GetMapping("/{documentId}/url")
    public ResponseEntity<?> getDocumentUrl(@PathVariable String documentId) {
        try {
            log.info("🔗 Getting URL for document: {}", documentId);
            
            // Find metadata
            for (List<DocumentMetadata> docs : driverDocuments.values()) {
                Optional<DocumentMetadata> found = docs.stream()
                        .filter(d -> d.getId().equals(documentId))
                        .findFirst();
                
                if (found.isPresent()) {
                    DocumentMetadata doc = found.get();
                    // Generate signed URL (valid for 1 hour)
                    String signedUrl = storageService.generateSignedUrl(doc.getFileUrl());
                    
                    if (signedUrl == null) {
                        // If signed URL fails, return the public URL
                        signedUrl = doc.getFileUrl();
                    }
                    
                    log.info("✅ Generated URL for document: {}", documentId);
                    return ResponseEntity.ok(Map.of("url", signedUrl));
                }
            }
            
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("❌ Error generating URL: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate URL: " + e.getMessage()));
        }
    }

    // Inner class for document metadata
    @lombok.Data
    @lombok.Builder
    public static class DocumentMetadata {
        private String id;
        private Long driverId;
        private String fileName;
        private String fileUrl;
        private String documentType;
        private String description;
        private Long fileSize;
        private String fileType;
        private LocalDateTime uploadedAt;
    }
}
