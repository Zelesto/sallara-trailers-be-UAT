// src/main/java/com/pgsa/trailers/service/inventory/VehicleIssueService.java
package com.pgsa.trailers.service.inventory;

import com.pgsa.trailers.dto.*;
import com.pgsa.trailers.entity.inventory.InventoryItem;
import com.pgsa.trailers.entity.inventory.InventoryLocation;
import com.pgsa.trailers.entity.inventory.StockMovement;
import com.pgsa.trailers.entity.inventory.VehicleIssue;
import com.pgsa.trailers.entity.inventory.VehicleIssueItem;
import com.pgsa.trailers.repository.InventoryItemRepository;
import com.pgsa.trailers.repository.InventoryLocationRepository;
import com.pgsa.trailers.repository.VehicleIssueItemRepository;
import com.pgsa.trailers.repository.VehicleIssueRepository;
import com.pgsa.trailers.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VehicleIssueService {

    private final VehicleIssueRepository vehicleIssueRepository;
    private final VehicleIssueItemRepository vehicleIssueItemRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final StockMovementRepository stockMovementRepository;

    private static final String ISSUE_NUMBER_PREFIX = "ISS-";

    /**
     * Issue items to a vehicle
     */
    public VehicleIssueResponseDTO issueItemsToVehicle(VehicleIssueRequestDTO request, Long userId) {
        log.info("Issuing items to vehicle: {}", request.getVehicleId());

        // Validate items
        for (VehicleIssueItemRequestDTO itemReq : request.getItems()) {
            InventoryItem item = inventoryItemRepository.findById(itemReq.getItemId())
                    .orElseThrow(() -> new RuntimeException("Item not found: " + itemReq.getItemId()));

            if (item.getQuantity() < itemReq.getQuantity().intValue()) {
                throw new RuntimeException("Insufficient stock for item: " + item.getName() +
                        ". Available: " + item.getQuantity() + ", Requested: " + itemReq.getQuantity());
            }
        }

        // Create Vehicle Issue
        VehicleIssue issue = VehicleIssue.builder()
                .issueNumber(generateIssueNumber())
                .vehicleId(request.getVehicleId())
                .driverId(request.getDriverId())
                .tripId(request.getTripId())
                .issueDate(request.getIssueDate() != null ? request.getIssueDate() : LocalDateTime.now())
                .status("ISSUED")
                .notes(request.getNotes())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        vehicleIssueRepository.save(issue);

        // Process items
        for (VehicleIssueItemRequestDTO itemReq : request.getItems()) {
            // Create issue item
            VehicleIssueItem issueItem = VehicleIssueItem.builder()
                    .issue(issue)
                    .itemId(itemReq.getItemId())
                    .quantityIssued(itemReq.getQuantity())
                    .quantityReturned(BigDecimal.ZERO)
                    .conditionIssued(itemReq.getCondition())
                    .notes(itemReq.getNotes())
                    .build();

            vehicleIssueItemRepository.save(issueItem);

            // Update inventory (deduct from main stock)
            InventoryItem item = inventoryItemRepository.findById(itemReq.getItemId())
                    .orElseThrow(() -> new RuntimeException("Item not found: " + itemReq.getItemId()));
            
            int newQuantity = item.getQuantity() - itemReq.getQuantity().intValue();
            item.setQuantity(newQuantity);
            inventoryItemRepository.save(item);

            // Find or create vehicle location
            InventoryLocation vehicleLocation = findOrCreateVehicleLocation(
                    request.getVehicleId(), 
                    getVehicleRegistration(request.getVehicleId()));

            // Create stock movement
            StockMovement movement = StockMovement.builder()
                    .itemId(itemReq.getItemId())
                    .quantity(itemReq.getQuantity().intValue())
                    .movementType("OUT")
                    .reason("Vehicle Issue")
                    .notes("Issued to vehicle: " + request.getVehicleId() + 
                           ", Driver: " + request.getDriverId() +
                           ", Trip: " + request.getTripId())
                    .referenceNumber(issue.getIssueNumber())
                    .performedBy(String.valueOf(userId))
                    .tripId(request.getTripId())
                    .build();

            stockMovementRepository.save(movement);
        }

        log.info("Items issued successfully. Issue Number: {}", issue.getIssueNumber());
        return mapToResponseDTO(issue);
    }

    /**
     * Return items from vehicle
     */
    public VehicleIssueResponseDTO returnItemsFromVehicle(Long issueId, List<ReturnItemRequestDTO> returns, Long userId) {
        log.info("Returning items from vehicle issue: {}", issueId);

        VehicleIssue issue = vehicleIssueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Vehicle issue not found: " + issueId));

        for (ReturnItemRequestDTO returnReq : returns) {
            VehicleIssueItem issueItem = vehicleIssueItemRepository
                    .findByIssueIdAndItemId(issueId, returnReq.getItemId())
                    .orElseThrow(() -> new RuntimeException("Item not found in issue: " + returnReq.getItemId()));

            // Update return quantity
            BigDecimal newReturned = issueItem.getQuantityReturned().add(returnReq.getQuantity());
            issueItem.setQuantityReturned(newReturned);
            issueItem.setConditionReturned(returnReq.getCondition());
            vehicleIssueItemRepository.save(issueItem);

            // Return to inventory
            InventoryItem item = inventoryItemRepository.findById(returnReq.getItemId())
                    .orElseThrow(() -> new RuntimeException("Item not found: " + returnReq.getItemId()));
            
            int newQuantity = item.getQuantity() + returnReq.getQuantity().intValue();
            item.setQuantity(newQuantity);
            inventoryItemRepository.save(item);

            // Create stock movement
            StockMovement movement = StockMovement.builder()
                    .itemId(returnReq.getItemId())
                    .quantity(returnReq.getQuantity().intValue())
                    .movementType("IN")
                    .reason("Vehicle Return")
                    .notes("Returned from vehicle: " + issue.getVehicleId())
                    .referenceNumber(issue.getIssueNumber())
                    .performedBy(String.valueOf(userId))
                    .build();

            stockMovementRepository.save(movement);
        }

        // Update issue status
        updateIssueStatus(issue);

        log.info("Items returned successfully. Issue Number: {}", issue.getIssueNumber());
        return mapToResponseDTO(issue);
    }

    @Transactional(readOnly = true)
    public List<VehicleIssueResponseDTO> getIssuesByVehicle(Long vehicleId) {
        return vehicleIssueRepository.findByVehicleIdOrderByIssueDateDesc(vehicleId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VehicleIssueResponseDTO getIssueById(Long issueId) {
        VehicleIssue issue = vehicleIssueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Vehicle issue not found: " + issueId));
        return mapToResponseDTO(issue);
    }

    // Helper methods
    private String generateIssueNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(5);
        return ISSUE_NUMBER_PREFIX + timestamp;
    }

    private InventoryLocation findOrCreateVehicleLocation(Long vehicleId, String vehicleRegistration) {
        // Check if location exists
        List<InventoryLocation> locations = inventoryLocationRepository.findAll();
        
        for (InventoryLocation loc : locations) {
            if (loc.getVehicleId() != null && loc.getVehicleId().equals(vehicleId)) {
                return loc;
            }
        }

        // Create new vehicle location
        InventoryLocation location = new InventoryLocation();
        location.setName("Vehicle - " + vehicleRegistration);
        location.setType("VEHICLE");
        location.setVehicleId(vehicleId);
        location.setVehicleRegistration(vehicleRegistration);
        location.setLocationType("VEHICLE");
        location.setIsActive(true);
        
        return inventoryLocationRepository.save(location);
    }

    private String getVehicleRegistration(Long vehicleId) {
        // This should call vehicle service to get registration
        // For now, return a placeholder
        return "VEH-" + vehicleId;
    }

    private void updateIssueStatus(VehicleIssue issue) {
        List<VehicleIssueItem> items = vehicleIssueItemRepository.findByIssueId(issue.getId());
        
        boolean allReturned = true;
        boolean anyReturned = false;
        
        for (VehicleIssueItem item : items) {
            if (item.getQuantityReturned().compareTo(item.getQuantityIssued()) < 0) {
                allReturned = false;
            }
            if (item.getQuantityReturned().compareTo(BigDecimal.ZERO) > 0) {
                anyReturned = true;
            }
        }
        
        if (allReturned) {
            issue.setStatus("RETURNED");
        } else if (anyReturned) {
            issue.setStatus("PARTIALLY_RETURNED");
        } else {
            issue.setStatus("ISSUED");
        }
        
        vehicleIssueRepository.save(issue);
    }

    private VehicleIssueResponseDTO mapToResponseDTO(VehicleIssue issue) {
        List<VehicleIssueItem> items = vehicleIssueItemRepository.findByIssueId(issue.getId());
        
        return VehicleIssueResponseDTO.builder()
                .id(issue.getId())
                .issueNumber(issue.getIssueNumber())
                .vehicleId(issue.getVehicleId())
                .driverId(issue.getDriverId())
                .tripId(issue.getTripId())
                .issueDate(issue.getIssueDate())
                .status(issue.getStatus())
                .notes(issue.getNotes())
                .items(items.stream().map(this::mapItemToResponseDTO).collect(Collectors.toList()))
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .build();
    }

    private VehicleIssueItemResponseDTO mapItemToResponseDTO(VehicleIssueItem item) {
        InventoryItem inventoryItem = inventoryItemRepository.findById(item.getItemId()).orElse(null);
        
        return VehicleIssueItemResponseDTO.builder()
                .id(item.getId())
                .itemId(item.getItemId())
                .itemName(inventoryItem != null ? inventoryItem.getName() : "Unknown")
                .itemCategory(inventoryItem != null ? inventoryItem.getCategory() : null)
                .quantityIssued(item.getQuantityIssued())
                .quantityReturned(item.getQuantityReturned())
                .quantityOutstanding(item.getQuantityIssued().subtract(item.getQuantityReturned()))
                .conditionIssued(item.getConditionIssued())
                .conditionReturned(item.getConditionReturned())
                .notes(item.getNotes())
                .build();
    }
}
