// src/main/java/com/pgsa/trailers/service/inventory/StockMovementService.java
package com.pgsa.trailers.service.inventory;

import com.pgsa.trailers.dto.inventory.StockMovementRequestDTO;
import com.pgsa.trailers.dto.inventory.StockMovementResponseDTO;
import com.pgsa.trailers.entity.inventory.InventoryItem;
import com.pgsa.trailers.entity.inventory.StockMovement;
import com.pgsa.trailers.repository.inventory.InventoryItemRepository;
import com.pgsa.trailers.repository.inventory.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final InventoryItemRepository inventoryItemRepository;

    /**
     * Record a new stock movement
     */
    public StockMovementResponseDTO recordMovement(StockMovementRequestDTO request) {
        // Validate item exists
        InventoryItem item = inventoryItemRepository.findById(request.getItemId())
                .orElseThrow(() -> new RuntimeException("Inventory item not found with ID: " + request.getItemId()));

        // Validate movement type
        if (!request.getMovementType().matches("IN|OUT|ADJUSTMENT")) {
            throw new RuntimeException("Invalid movement type. Use IN, OUT, or ADJUSTMENT");
        }

        // Determine if approval is required
        boolean requiresApproval = request.getRequiresApproval() != null ? request.getRequiresApproval() : false;
        
        // Stock OUT and ADJUSTMENT always require approval
        if ("OUT".equals(request.getMovementType()) || "ADJUSTMENT".equals(request.getMovementType())) {
            requiresApproval = true;
        }
        
        // Stock IN requires approval if no reference number provided
        if ("IN".equals(request.getMovementType()) && (request.getReferenceNumber() == null || request.getReferenceNumber().isEmpty())) {
            requiresApproval = true;
        }

        // Create movement record using builder
        StockMovement movement = StockMovement.builder()
                .itemId(request.getItemId())
                .quantity(request.getQuantity())
                .movementType(request.getMovementType())
                .reason(request.getReason())
                .notes(request.getNotes())
                .referenceNumber(request.getReferenceNumber())
                .referenceType(request.getReferenceType())
                .performedBy(request.getPerformedBy())
                .tripId(request.getTripId())
                .fuelSlipId(request.getFuelSlipId())
                .requiresApproval(requiresApproval)
                .approvalStatus(requiresApproval ? "PENDING" : "APPROVED")
                .build();

        StockMovement saved = stockMovementRepository.save(movement);
        log.info("Recorded stock movement for item {}: {} {} units, Approval: {}", 
                request.getItemId(), request.getMovementType(), request.getQuantity(), 
                requiresApproval ? "PENDING" : "AUTO-APPROVED");

        // If no approval required, update inventory immediately
        if (!requiresApproval) {
            updateInventoryQuantity(movement);
        }

        return mapToResponseDTO(saved);
    }

    /**
     * Get all movements with pagination
     */
    @Transactional(readOnly = true)
    public Page<StockMovementResponseDTO> getAllMovements(Pageable pageable) {
        return stockMovementRepository.findAll(pageable)
                .map(this::mapToResponseDTO);
    }

    /**
     * Get movement by ID
     */
    @Transactional(readOnly = true)
    public StockMovementResponseDTO getMovementById(Long id) {
        StockMovement movement = stockMovementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock movement not found with ID: " + id));
        return mapToResponseDTO(movement);
    }

    /**
     * Get movements by item with pagination
     */
    @Transactional(readOnly = true)
    public Page<StockMovementResponseDTO> getMovementsByItem(Long itemId, Pageable pageable) {
        return stockMovementRepository.findByItemId(itemId, pageable)
                .map(this::mapToResponseDTO);
    }

    /**
     * Get movements by trip
     */
    @Transactional(readOnly = true)
    public List<StockMovementResponseDTO> getMovementsByTrip(Long tripId) {
        return stockMovementRepository.findByTripId(tripId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get movements by fuel slip
     */
    @Transactional(readOnly = true)
    public List<StockMovementResponseDTO> getMovementsByFuelSlip(Long fuelSlipId) {
        return stockMovementRepository.findByFuelSlipId(fuelSlipId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get movements by date range
     */
    @Transactional(readOnly = true)
    public List<StockMovementResponseDTO> getMovementsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return stockMovementRepository.findByDateRange(startDate, endDate)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get pending approvals
     */
    @Transactional(readOnly = true)
    public List<StockMovementResponseDTO> getPendingApprovals() {
        return stockMovementRepository.findByApprovalStatus("PENDING")
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Approve a movement
     */
    @Transactional
    public StockMovementResponseDTO approveMovement(Long id, String approvedBy, String notes) {
        StockMovement movement = stockMovementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock movement not found with ID: " + id));
        
        if (!"PENDING".equals(movement.getApprovalStatus())) {
            throw new RuntimeException("Movement is not in pending status. Current status: " + movement.getApprovalStatus());
        }
        
        movement.setApprovalStatus("APPROVED");
        movement.setApprovedBy(approvedBy);
        movement.setApprovedAt(LocalDateTime.now());
        movement.setApprovalNotes(notes);
        
        // Update inventory quantity when approved
        updateInventoryQuantity(movement);
        
        StockMovement updated = stockMovementRepository.save(movement);
        log.info("Movement {} approved by: {}", id, approvedBy);
        return mapToResponseDTO(updated);
    }

    /**
     * Reject a movement
     */
    @Transactional
    public StockMovementResponseDTO rejectMovement(Long id, String rejectedBy, String reason) {
        StockMovement movement = stockMovementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock movement not found with ID: " + id));
        
        if (!"PENDING".equals(movement.getApprovalStatus())) {
            throw new RuntimeException("Movement is not in pending status. Current status: " + movement.getApprovalStatus());
        }
        
        movement.setApprovalStatus("REJECTED");
        movement.setRejectedBy(rejectedBy);
        movement.setRejectedAt(LocalDateTime.now());
        movement.setRejectionReason(reason);
        
        StockMovement updated = stockMovementRepository.save(movement);
        log.info("Movement {} rejected by: {}, reason: {}", id, rejectedBy, reason);
        return mapToResponseDTO(updated);
    }

    /**
     * Get movement statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMovementStats(String startDate, String endDate) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMovements", stockMovementRepository.count());
        stats.put("pendingApprovals", stockMovementRepository.countByApprovalStatus("PENDING"));
        stats.put("approvedMovements", stockMovementRepository.countByApprovalStatus("APPROVED"));
        stats.put("rejectedMovements", stockMovementRepository.countByApprovalStatus("REJECTED"));
        
        // Get counts by movement type
        stats.put("stockIn", stockMovementRepository.countByMovementType("IN"));
        stats.put("stockOut", stockMovementRepository.countByMovementType("OUT"));
        stats.put("adjustments", stockMovementRepository.countByMovementType("ADJUSTMENT"));
        
        return stats;
    }

    /**
     * Update inventory quantity based on movement
     */
    private void updateInventoryQuantity(StockMovement movement) {
        InventoryItem item = inventoryItemRepository.findById(movement.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found with ID: " + movement.getItemId()));
        
        int currentQuantity = item.getQuantity() != null ? item.getQuantity() : 0;
        int newQuantity;
        
        switch (movement.getMovementType()) {
            case "IN":
                newQuantity = currentQuantity + movement.getQuantity();
                break;
            case "OUT":
                if (currentQuantity < movement.getQuantity()) {
                    throw new RuntimeException("Insufficient stock. Available: " + currentQuantity + ", Requested: " + movement.getQuantity());
                }
                newQuantity = currentQuantity - movement.getQuantity();
                break;
            case "ADJUSTMENT":
                newQuantity = movement.getQuantity();
                break;
            default:
                throw new RuntimeException("Invalid movement type: " + movement.getMovementType());
        }
        
        item.setQuantity(newQuantity);
        inventoryItemRepository.save(item);
        log.info("Updated quantity for item {} to: {}", item.getId(), newQuantity);
    }

    /**
     * Map entity to response DTO
     */
    private StockMovementResponseDTO mapToResponseDTO(StockMovement movement) {
        // Get item name
        String itemName = null;
        java.util.Optional<InventoryItem> optionalItem = inventoryItemRepository.findById(movement.getItemId());
        if (optionalItem.isPresent()) {
            itemName = optionalItem.get().getName();
        }

        return StockMovementResponseDTO.builder()
                .id(movement.getId())
                .itemId(movement.getItemId())
                .itemName(itemName)
                .quantity(movement.getQuantity())
                .movementType(movement.getMovementType())
                .reason(movement.getReason())
                .notes(movement.getNotes())
                .referenceNumber(movement.getReferenceNumber())
                .referenceType(movement.getReferenceType())
                .performedBy(movement.getPerformedBy())
                .createdAt(movement.getCreatedAt())
                .tripId(movement.getTripId())
                .fuelSlipId(movement.getFuelSlipId())
                .requiresApproval(movement.getRequiresApproval())
                .approvalStatus(movement.getApprovalStatus())
                .approvedBy(movement.getApprovedBy())
                .approvedAt(movement.getApprovedAt())
                .approvalNotes(movement.getApprovalNotes())
                .rejectedBy(movement.getRejectedBy())
                .rejectedAt(movement.getRejectedAt())
                .rejectionReason(movement.getRejectionReason())
                .build();
    }
}
