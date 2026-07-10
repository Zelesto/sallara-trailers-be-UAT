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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    
    private final String uploadDir = "uploads/pods/";

  

public PodResponseDTO createPod(PodRequestDTO request, MultipartFile file) {
    log.info("Creating POD with request: {}", request);
    
    if (request == null) {
        throw new RuntimeException("PodRequestDTO cannot be null");
    }
    
    if (request.getTripId() == null) {
        throw new RuntimeException("Trip ID is required");
    }
    
    Pod pod = Pod.builder()
            .tripId(request.getTripId())
            .customerName(request.getCustomerName() != null ? request.getCustomerName() : "N/A")
            .deliveryDate(request.getDeliveryDate())
            .status(request.getStatus() != null ? request.getStatus() : "PENDING")
            .documentType(request.getDocumentType())
            .notes(request.getNotes())
            .uploadedBy(request.getUploadedBy() != null ? request.getUploadedBy() : "System")
            .uploadedAt(LocalDateTime.now())
            .source("UPLOADED")
            .build();
    
    Pod savedPod = podRepository.save(pod);
    log.info("POD saved with ID: {}", savedPod.getId());
    
    if (file != null && !file.isEmpty()) {
        try {
            String fileUrl = storageService.uploadFile(file, savedPod.getPodNumber());
            savedPod.setFileUrl(fileUrl);
            savedPod.setFileName(file.getOriginalFilename());
            savedPod.setFileSize(formatFileSize(file.getSize()));
            savedPod.setDocumentType(getFileExtension(file.getOriginalFilename()));
            savedPod = podRepository.save(savedPod);
            log.info("File uploaded for POD: {}", savedPod.getPodNumber());
        } catch (Exception e) {
            log.error("Failed to upload file for POD: {}", savedPod.getPodNumber(), e);
            // Don't throw, just log - the POD is already created
        }
    }
    
    log.info("POD created with ID: {}", savedPod.getId());
    return mapToResponse(savedPod);
}
    public PodResponseDTO scanPod(Long tripId, String driverName, String deliveryDate, 
                                   String customerName, String notes, MultipartFile file) {
        if (!tripRepository.existsById(tripId)) {
            throw new RuntimeException("Trip not found with id: " + tripId);
        }

        LocalDate parsedDeliveryDate = LocalDate.parse(deliveryDate, DateTimeFormatter.ISO_LOCAL_DATE);

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

        Pod savedPod = podRepository.save(pod);

        if (file != null && !file.isEmpty()) {
            try {
                String fileUrl = storageService.uploadFile(file, savedPod.getPodNumber());
                savedPod.setFileUrl(fileUrl);
                savedPod.setFileName(file.getOriginalFilename());
                savedPod.setFileSize(formatFileSize(file.getSize()));
                savedPod.setDocumentType(getFileExtension(file.getOriginalFilename()));
                savedPod = podRepository.save(savedPod);
                log.info("Scanned file uploaded for POD: {}", savedPod.getPodNumber());
            } catch (Exception e) {
                log.error("Failed to upload scanned file for POD: {}", savedPod.getPodNumber(), e);
                throw new RuntimeException("Failed to upload scanned file: " + e.getMessage(), e);
            }
        }

        log.info("POD scanned from driver with ID: {}", savedPod.getId());
        return mapToResponse(savedPod);
    }

    public PodResponseDTO debriefPod(Long id, DebriefRequestDTO debriefRequest) {
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));

        pod.setStatus(debriefRequest.getStatus());
        pod.setNotes(debriefRequest.getNotes() != null ? debriefRequest.getNotes() : pod.getNotes());
        pod.setReceivedBy(debriefRequest.getReceivedBy());
        pod.setQualityRating(debriefRequest.getQualityRating());
        if (debriefRequest.getIssuesFound() != null && !debriefRequest.getIssuesFound().isEmpty()) {
        // Option 1: Join the list as a comma-separated string
        pod.setIssuesFound(String.join(", ", debriefRequest.getIssuesFound()));
    } else {
        pod.setIssuesFound(null);
    }
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

    @Transactional(readOnly = true)
    public String downloadPodDocument(Long id) {
        return getPodFileUrl(id);
    }
    
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

    @Transactional(readOnly = true)
    public String getPodFilename(Long id) {
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));
        return pod.getFileName() != null ? pod.getFileName() : "pod-document.pdf";
    }

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

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "pdf";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toUpperCase();
    }

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

    @Transactional(readOnly = true)
    public Page<PodResponseDTO> getPodsByTripPaginated(Long tripId, Pageable pageable) {
        return podRepository.findByTripId(tripId, pageable)
                .map(this::mapToResponseSafe);
    }

    /**
     * Get all PODs with pagination - FIXED: Non-transactional to prevent rollback
     */
    public Page<PodResponseDTO> getAllPods(Pageable pageable) {
        log.info("Fetching all PODs with pageable: {}", pageable);
        try {
            Page<Pod> podPage = podRepository.findAll(pageable);
            log.info("Found {} PODs total, {} in this page", podPage.getTotalElements(), podPage.getNumberOfElements());
            
            if (podPage.isEmpty()) {
                return Page.empty(pageable);
            }
            
            // Map each pod safely
            List<PodResponseDTO> dtos = new ArrayList<>();
            for (Pod pod : podPage.getContent()) {
                PodResponseDTO dto = mapToResponseSafe(pod);
                if (dto != null) {
                    dtos.add(dto);
                }
            }
            
            return new PageImpl<>(dtos, pageable, podPage.getTotalElements());
            
        } catch (Exception e) {
            log.error("Error fetching PODs: {}", e.getMessage(), e);
            return Page.empty(pageable);
        }
    }

    @Transactional(readOnly = true)
    public Page<PodResponseDTO> searchPods(String searchTerm, Pageable pageable) {
        return podRepository.searchPods(searchTerm, pageable)
                .map(this::mapToResponseSafe);
    }

    @Transactional(readOnly = true)
    public Page<PodResponseDTO> getPodsByStatus(String status, Pageable pageable) {
        return podRepository.findByStatus(status, pageable)
                .map(this::mapToResponseSafe);
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
     * Safely map Pod to response DTO - handles all exceptions
     */
    private PodResponseDTO mapToResponseSafe(Pod pod) {
        if (pod == null) {
            return null;
        }
        
        try {
            String tripNumber = getTripNumber(pod.getTripId());
            
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
                    //.issuesFound(pod.getIssuesFound())
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
