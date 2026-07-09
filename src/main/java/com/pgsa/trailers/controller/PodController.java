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

    // ✅ FIX: Accept file parameter
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PodResponseDTO> createPod(
            @RequestPart("podData") @Valid PodRequestDTO request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("Creating new POD with file: {}", file != null ? file.getOriginalFilename() : "no file");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(podService.createPod(request, file));
    }

    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PodResponseDTO> scanPod(
            @RequestParam("tripId") Long tripId,
            @RequestParam("driverName") String driverName,
            @RequestParam("deliveryDate") String deliveryDate,
            @RequestParam(value = "customerName", required = false) String customerName,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam("file") MultipartFile file) {
        log.info("Scanning POD from driver for trip: {}", tripId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(podService.scanPod(tripId, driverName, deliveryDate, customerName, notes, file));
    }

    @PostMapping("/{id}/debrief")
    public ResponseEntity<PodResponseDTO> debriefPod(
            @PathVariable Long id,
            @Valid @RequestBody DebriefRequestDTO debriefRequest) {
        log.info("Debriefing POD: {}", id);
        return ResponseEntity.ok(podService.debriefPod(id, debriefRequest));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PodResponseDTO> getPodById(@PathVariable Long id) {
        return ResponseEntity.ok(podService.getPodById(id));
    }

    @GetMapping("/number/{podNumber}")
    public ResponseEntity<PodResponseDTO> getPodByNumber(@PathVariable String podNumber) {
        return ResponseEntity.ok(podService.getPodByNumber(podNumber));
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<PodResponseDTO>> getPodsByTrip(@PathVariable Long tripId) {
        return ResponseEntity.ok(podService.getPodsByTrip(tripId));
    }

    @GetMapping("/trip/{tripId}/page")
    public ResponseEntity<Page<PodResponseDTO>> getPodsByTripPaginated(
            @PathVariable Long tripId,
            @PageableDefault(size = 10, sort = "deliveryDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(podService.getPodsByTripPaginated(tripId, pageable));
    }

    @GetMapping
    public ResponseEntity<Page<PodResponseDTO>> getAllPods(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(podService.getAllPods(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PodResponseDTO>> searchPods(
            @RequestParam String q,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(podService.searchPods(q, pageable));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<PodResponseDTO>> getPodsByStatus(
            @PathVariable String status,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(podService.getPodsByStatus(status, pageable));
    }

    @GetMapping("/{id}/status-history")
    public ResponseEntity<List<StatusHistoryDTO>> getPodStatusHistory(@PathVariable Long id) {
        return ResponseEntity.ok(podService.getPodStatusHistory(id));
    }

    // ✅ FIX: Use getPodFileUrl instead of downloadPodDocument
    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadPodDocument(@PathVariable Long id) {
        log.info("Downloading POD document: {}", id);
        try {
            String fileUrl = podService.getPodFileUrl(id);
            if (fileUrl == null) {
                return ResponseEntity.notFound().build();
            }
            // Return the file URL instead of the resource
            return ResponseEntity.ok(fileUrl);
        } catch (Exception e) {
            log.error("Error downloading POD document: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to download document: " + e.getMessage());
        }
    }

    // ✅ FIX: Use getPodFileUrl instead of downloadPodDocument
    @GetMapping("/{id}/view")
    public ResponseEntity<?> viewPodDocument(@PathVariable Long id) {
        log.info("Viewing POD document: {}", id);
        try {
            String fileUrl = podService.getPodFileUrl(id);
            if (fileUrl == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(fileUrl);
        } catch (Exception e) {
            log.error("Error viewing POD document: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to view document: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<PodResponseDTO> updatePod(
            @PathVariable Long id,
            @Valid @RequestBody PodRequestDTO request) {
        return ResponseEntity.ok(podService.updatePod(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PodResponseDTO> updatePodStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(podService.updatePodStatus(id, status));
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<PodResponseDTO> verifyPod(
            @PathVariable Long id,
            @RequestParam String verifiedBy) {
        return ResponseEntity.ok(podService.verifyPod(id, verifiedBy));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<PodResponseDTO> rejectPod(
            @PathVariable Long id,
            @RequestParam String rejectedBy,
            @RequestParam String reason) {
        return ResponseEntity.ok(podService.rejectPod(id, rejectedBy, reason));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePod(@PathVariable Long id) {
        podService.deletePod(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistics")
    public ResponseEntity<PodStatistics> getPodStatistics() {
        return ResponseEntity.ok(podService.getPodStatistics());
    }
}
