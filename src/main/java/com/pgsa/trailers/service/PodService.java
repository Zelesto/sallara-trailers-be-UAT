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
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PodService {

    private final PodRepository podRepository;
    private final TripRepository tripRepository;
    
    private final String uploadDir = "uploads/pods/";

    public PodResponseDTO createPod(PodRequestDTO request) {
        Pod pod = Pod.builder()
                .tripId(request.getTripId())
                .customerName(request.getCustomerName())
                .deliveryDate(request.getDeliveryDate())
                .status(request.getStatus() != null ? request.getStatus() : "PENDING")
                .documentType(request.getDocumentType())
                .fileSize(request.getFileSize())
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .notes(request.getNotes())
                .uploadedBy(request.getUploadedBy())
                .uploadedAt(LocalDateTime.now())
                .source("UPLOADED")
                .build();

        Pod saved = podRepository.save(pod);
        log.info("POD created with ID: {}", saved.getId());
        return mapToResponse(saved);
    }

    /**
     * Scan a new POD from driver
     */
    public PodResponseDTO scanPod(Long tripId, String driverName, String deliveryDate, 
                                   String customerName, String notes, MultipartFile file) {
        // Validate trip exists
        if (!tripRepository.existsById(tripId)) {
            throw new RuntimeException("Trip not found with id: " + tripId);
        }

        // Parse delivery date
        LocalDate parsedDeliveryDate = LocalDate.parse(deliveryDate, DateTimeFormatter.ISO_LOCAL_DATE);

        // Create POD
        Pod pod = Pod.builder()
                .tripId(tripId)
                .driverName(driverName)
                .deliveryDate(parsedDeliveryDate)
                .customerName(customerName != null ? customerName : "Adhoc Customer")
                .notes(notes != null ? notes : "Scanned from driver")
                .status("SCANNED")
                .source("SCANNED")
                .uploadedBy("Driver")
                .uploadedAt(LocalDateTime.now())
                .build();

        // Save file
        if (file != null && !file.isEmpty()) {
            String filePath = savePodFile(file, pod);
            pod.setFileUrl(filePath);
            pod.setDocumentType(getFileExtension(file.getOriginalFilename()));
            pod.setFileSize(formatFileSize(file.getSize()));
            pod.setFileName(file.getOriginalFilename());
        }

        Pod savedPod = podRepository.save(pod);
        log.info("POD scanned from driver with ID: {}", savedPod.getId());
        return mapToResponse(savedPod);
    }

    /**
     * Debrief a POD
     */
    public PodResponseDTO debriefPod(Long id, DebriefRequestDTO debriefRequest) {
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));

        // Update POD with debrief information
        pod.setStatus(debriefRequest.getStatus());
        pod.setNotes(debriefRequest.getNotes() != null ? debriefRequest.getNotes() : pod.getNotes());
        pod.setReceivedBy(debriefRequest.getReceivedBy());
        pod.setQualityRating(debriefRequest.getQualityRating());
        pod.setIssuesFound(debriefRequest.getIssuesFound());
        pod.setAdditionalInfo(debriefRequest.getAdditionalInfo());
        pod.setDeliveryCondition(debriefRequest.getDeliveryCondition());
        pod.setDebriefNotes(debriefRequest.getDebriefNotes());
        pod.setDebriefedBy(debriefRequest.getDebriefedBy() != null ? 
                debriefRequest.getDebriefedBy() : "System");
        pod.setDebriefedAt(LocalDateTime.now());
        pod.setUpdatedAt(LocalDateTime.now());

        Pod updatedPod = podRepository.save(pod);
        log.info("POD {} debriefed with status: {}", id, debriefRequest.getStatus());
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
        
        // Initial creation
        history.add(StatusHistoryDTO.builder()
                .status(pod.getStatus() != null ? pod.getStatus() : "CREATED")
                .notes("POD created")
                .updatedBy(pod.getUploadedBy() != null ? pod.getUploadedBy() : "System")
                .timestamp(pod.getCreatedAt() != null ? pod.getCreatedAt() : LocalDateTime.now())
                .build());

        // If status was updated and different from initial
        if (pod.getUpdatedAt() != null && pod.getCreatedAt() != null && 
            pod.getUpdatedAt().isAfter(pod.getCreatedAt())) {
            history.add(StatusHistoryDTO.builder()
                    .status(pod.getStatus())
                    .notes("Status updated to: " + pod.getStatus())
                    .updatedBy(pod.getUpdatedBy() != null ? pod.getUpdatedBy() : "System")
                    .timestamp(pod.getUpdatedAt())
                    .build());
        }

        // If verified
        if (pod.getVerifiedAt() != null) {
            history.add(StatusHistoryDTO.builder()
                    .status("VERIFIED")
                    .notes("POD verified")
                    .updatedBy(pod.getVerifiedBy())
                    .timestamp(pod.getVerifiedAt())
                    .build());
        }

        // If rejected
        if (pod.getRejectedAt() != null) {
            history.add(StatusHistoryDTO.builder()
                    .status("REJECTED")
                    .notes("POD rejected: " + (pod.getRejectionReason() != null ? pod.getRejectionReason() : ""))
                    .updatedBy(pod.getRejectedBy())
                    .timestamp(pod.getRejectedAt())
                    .build());
        }

        // If debriefed
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
    public Resource downloadPodDocument(Long id) {
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));

        if (pod.getFileUrl() == null) {
            throw new RuntimeException("No document associated with this POD");
        }

        try {
            Path filePath = Paths.get(pod.getFileUrl());
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Document file not found: " + pod.getFileUrl());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error accessing document file", e);
        }
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
     * Save POD file
     */
    private String savePodFile(MultipartFile file, Pod pod) {
        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String filename = UUID.randomUUID().toString() + "_" + 
                            (pod.getId() != null ? pod.getId() : System.currentTimeMillis()) + 
                            "." + extension;
            
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            return filePath.toString();
        } catch (IOException e) {
            log.error("Failed to save POD document", e);
            throw new RuntimeException("Failed to save POD document", e);
        }
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "pdf";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toUpperCase();
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

    @Transactional(readOnly = true)
    public PodResponseDTO getPodById(Long id) {
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));
        return mapToResponse(pod);
    }

    @Transactional(readOnly = true)
    public PodResponseDTO getPodByNumber(String podNumber) {
        Pod pod = podRepository.findByPodNumber(podNumber)
                .orElseThrow(() -> new RuntimeException("POD not found with number: " + podNumber));
        return mapToResponse(pod);
    }

    @Transactional(readOnly = true)
    public List<PodResponseDTO> getPodsByTrip(Long tripId) {
        return podRepository.findByTripId(tripId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PodResponseDTO> getPodsByTripPaginated(Long tripId, Pageable pageable) {
        return podRepository.findByTripId(tripId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<PodResponseDTO> getAllPods(Pageable pageable) {
        return podRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<PodResponseDTO> searchPods(String searchTerm, Pageable pageable) {
        return podRepository.searchPods(searchTerm, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<PodResponseDTO> getPodsByStatus(String status, Pageable pageable) {
        return podRepository.findByStatus(status, pageable)
                .map(this::mapToResponse);
    }

    public PodResponseDTO updatePod(Long id, PodRequestDTO request) {
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

        Pod updated = podRepository.save(pod);
        log.info("POD updated with ID: {}", updated.getId());
        return mapToResponse(updated);
    }

    public PodResponseDTO updatePodStatus(Long id, String status) {
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));

        pod.setStatus(status);
        Pod updated = podRepository.save(pod);
        log.info("POD {} status updated to: {}", id, status);
        return mapToResponse(updated);
    }

    public PodResponseDTO verifyPod(Long id, String verifiedBy) {
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));

        pod.setStatus("VERIFIED");
        pod.setVerifiedBy(verifiedBy);
        pod.setVerifiedAt(LocalDateTime.now());
        Pod updated = podRepository.save(pod);
        log.info("POD {} verified by: {}", id, verifiedBy);
        return mapToResponse(updated);
    }

    public PodResponseDTO rejectPod(Long id, String rejectedBy, String reason) {
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));

        pod.setStatus("REJECTED");
        pod.setRejectedBy(rejectedBy);
        pod.setRejectedAt(LocalDateTime.now());
        pod.setRejectionReason(reason);
        Pod updated = podRepository.save(pod);
        log.info("POD {} rejected by: {}, reason: {}", id, rejectedBy, reason);
        return mapToResponse(updated);
    }

    public void deletePod(Long id) {
        if (!podRepository.existsById(id)) {
            throw new RuntimeException("POD not found with ID: " + id);
        }
        podRepository.deleteById(id);
        log.info("POD deleted with ID: {}", id);
    }

    public PodStatistics getPodStatistics() {
    long total = podRepository.count();
    long pending = podRepository.countByStatus("PENDING");
    long delivered = podRepository.countByStatus("DELIVERED");
    long verified = podRepository.countByStatus("VERIFIED");
    long rejected = podRepository.countByStatus("REJECTED");
    long scanned = podRepository.countBySource("SCANNED");
    
    // Get pending debrief count (PENDING or SCANNED status)
    long pendingDebrief = podRepository.countPendingDebrief();
    
    // Get today's scans
    LocalDate today = LocalDate.now();
    long scannedToday = podRepository.countScannedSince(today);

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
     * Helper method to get trip number from trip ID
     */
    private String getTripNumber(Long tripId) {
        if (tripId == null) {
            return null;
        }
        try {
            return tripRepository.findTripNumberById(tripId)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Could not find trip number for trip ID: {}", tripId, e);
            return null;
        }
    }

    private PodResponseDTO mapToResponse(Pod pod) {
        // Get trip number from trip ID
        String tripNumber = getTripNumber(pod.getTripId());

        return PodResponseDTO.builder()
                .id(pod.getId())
                .podNumber(pod.getPodNumber())
                .tripId(pod.getTripId())
                .tripNumber(tripNumber)
                .customerName(pod.getCustomerName())
                .driverName(pod.getDriverName())
                .deliveryDate(pod.getDeliveryDate())
                .status(pod.getStatus())
                .source(pod.getSource())
                .documentType(pod.getDocumentType())
                .fileSize(pod.getFileSize())
                .fileUrl(pod.getFileUrl())
                .fileName(pod.getFileName())
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
    }
}
