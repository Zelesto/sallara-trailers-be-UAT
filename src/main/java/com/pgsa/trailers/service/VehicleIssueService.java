package com.pgsa.trailers.service.inventory;

import com.pgsa.trailers.dto.*;
import com.pgsa.trailers.entity.inventory.*;
import com.pgsa.trailers.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VehicleIssueService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VehicleIssueService.class);

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
        log.info("🚗 Issuing items to vehicle: {}", request.getVehicleId());

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
                    .referenceType("VEHICLE_ISSUE")
                    .requiresApproval(false)
                    .approvalStatus("APPROVED")
                    .build();

            stockMovementRepository.save(movement);
        }

        log.info("✅ Items issued successfully. Issue Number: {}", issue.getIssueNumber());
        return mapToResponseDTO(issue);
    }

    /**
 * Swap an item - return damaged and issue replacement for vehicle
 */
@Transactional
public VehicleIssueResponseDTO swapItem(Long oldIssueId, SwapItemRequestDTO swapRequest, Long userId) {
    log.info("🔄 Swapping item from vehicle issue: {}", oldIssueId);
    
    // 1. Find the existing issue
    VehicleIssue oldIssue = vehicleIssueRepository.findById(oldIssueId)
            .orElseThrow(() -> new RuntimeException("Vehicle issue not found: " + oldIssueId));
    
    // 2. Find the specific item in the issue
    VehicleIssueItem oldItem = vehicleIssueItemRepository
            .findByIssueIdAndItemId(oldIssueId, swapRequest.getOldItemId())
            .orElseThrow(() -> new RuntimeException("Item not found in issue: " + swapRequest.getOldItemId()));
    
    // 3. Validate old item is not already returned
    if (oldItem.getQuantityReturned().compareTo(oldItem.getQuantityIssued()) >= 0) {
        throw new RuntimeException("Item already returned, cannot swap");
    }
    
    // 4. Validate new item has sufficient stock
    InventoryItem newItem = inventoryItemRepository.findById(swapRequest.getNewItemId())
            .orElseThrow(() -> new RuntimeException("New item not found: " + swapRequest.getNewItemId()));
    
    if (newItem.getQuantity() < swapRequest.getNewQuantity()) {
        throw new RuntimeException("Insufficient stock for new item: " + newItem.getName() +
                ". Available: " + newItem.getQuantity() + ", Requested: " + swapRequest.getNewQuantity());
    }
    
    // 5. Process the old item return
    BigDecimal returnQuantity = swapRequest.getReturnQuantity() != null ? 
            swapRequest.getReturnQuantity() : oldItem.getQuantityIssued();
    
    // Mark old item as returned
    oldItem.setQuantityReturned(oldItem.getQuantityReturned().add(returnQuantity));
    oldItem.setConditionReturned(swapRequest.getDamagedCondition());
    oldItem.setIsSwap(true);
    oldItem.setSwapReason(swapRequest.getDamagedCondition());
    oldItem.setUpdatedAt(LocalDateTime.now());
    vehicleIssueItemRepository.save(oldItem);
    
    // 6. Create hold/damage record on the old item
    InventoryItem inventoryItem = inventoryItemRepository.findById(swapRequest.getOldItemId())
            .orElseThrow(() -> new RuntimeException("Inventory item not found"));
    
    inventoryItem.setHoldCode(swapRequest.getDamagedCondition());
    inventoryItem.setHoldReason(swapRequest.getDamageNotes());
    inventoryItem.setHoldDate(LocalDateTime.now());
    inventoryItem.setHeldBy(String.valueOf(userId));
    inventoryItemRepository.save(inventoryItem);
    
    // 7. Return old item to inventory with hold status
    int currentQuantity = inventoryItem.getQuantity() != null ? inventoryItem.getQuantity() : 0;
    inventoryItem.setQuantity(currentQuantity + returnQuantity.intValue());
    inventoryItemRepository.save(inventoryItem);
    
    // 8. Create stock movement for return
    StockMovement returnMovement = StockMovement.builder()
            .itemId(swapRequest.getOldItemId())
            .quantity(returnQuantity.intValue())
            .movementType("IN")
            .reason("Vehicle Swap Return - " + swapRequest.getDamagedCondition())
            .notes("Damaged item returned from vehicle. Hold code: " + swapRequest.getDamagedCondition())
            .referenceNumber(oldIssue.getIssueNumber())
            .performedBy(String.valueOf(userId))
            .referenceType("VEHICLE_SWAP_RETURN")
            .requiresApproval(false)
            .approvalStatus("APPROVED")
            .build();
    stockMovementRepository.save(returnMovement);
    
    // 9. Create new issue for the replacement item
    VehicleIssueRequestDTO newIssueRequest = new VehicleIssueRequestDTO();
    newIssueRequest.setVehicleId(oldIssue.getVehicleId());
    newIssueRequest.setDriverId(oldIssue.getDriverId());
    newIssueRequest.setTripId(oldIssue.getTripId());
    newIssueRequest.setIssueDate(LocalDateTime.now());
    newIssueRequest.setNotes("SWAP: Replacing damaged item. Original Issue: " + oldIssue.getIssueNumber());
    
    VehicleIssueItemRequestDTO newItemRequest = new VehicleIssueItemRequestDTO();
    newItemRequest.setItemId(swapRequest.getNewItemId());
    newItemRequest.setQuantity(BigDecimal.valueOf(swapRequest.getNewQuantity()));
    newItemRequest.setCondition("NEW");
    newItemRequest.setNotes("Swap replacement for " + swapRequest.getDamagedCondition());
    
    newIssueRequest.setItems(List.of(newItemRequest));
    
    // 10. Create the new issue
    VehicleIssueResponseDTO newIssue = issueItemsToVehicle(newIssueRequest, userId);
    
    // 11. Link the new issue to the old one
    oldItem.setSwapIssueId(newIssue.getId());
    vehicleIssueItemRepository.save(oldItem);
    
    log.info("✅ Vehicle swap completed: Old issue {} returned, New issue {} created", oldIssueId, newIssue.getId());
    
    return newIssue;
}

    /**
     * Return items from vehicle
     */
    public VehicleIssueResponseDTO returnItemsFromVehicle(Long issueId, List<ReturnItemRequestDTO> returns, Long userId) {
        log.info("🔄 Returning items from vehicle issue: {}", issueId);

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
            issueItem.setUpdatedAt(LocalDateTime.now());
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
                    .notes("Returned from vehicle: " + issue.getVehicleId() +
                           ", Condition: " + returnReq.getCondition())
                    .referenceNumber(issue.getIssueNumber())
                    .performedBy(String.valueOf(userId))
                    .referenceType("VEHICLE_RETURN")
                    .requiresApproval(false)
                    .approvalStatus("APPROVED")
                    .build();

            stockMovementRepository.save(movement);
        }

        // Update issue status
        updateIssueStatus(issue);

        log.info("✅ Items returned successfully. Issue Number: {}", issue.getIssueNumber());
        return mapToResponseDTO(issue);
    }

    @Transactional(readOnly = true)
    public List<VehicleIssueResponseDTO> getAllVehicleIssues() {
        log.info("📋 Fetching all vehicle issues");
        try {
            List<VehicleIssue> issues = vehicleIssueRepository.findAllByOrderByIssueDateDesc();
            log.info("📋 Found {} vehicle issues in database", issues.size());
            
            if (issues.isEmpty()) {
                log.warn("⚠️ No vehicle issues found in database");
                return new ArrayList<>();
            }
            
            // Log each issue for debugging
            for (VehicleIssue issue : issues) {
                log.debug("📋 Issue: ID={}, Number={}, Vehicle={}, Driver={}, Status={}", 
                    issue.getId(), 
                    issue.getIssueNumber(), 
                    issue.getVehicleId(), 
                    issue.getDriverId(),
                    issue.getStatus()
                );
            }
            
            return issues.stream()
                    .map(this::mapToResponseDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("❌ Error fetching vehicle issues: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public List<VehicleIssueResponseDTO> getIssuesByVehicle(Long vehicleId) {
        log.info("🚗 Fetching issues for vehicle: {}", vehicleId);
        return vehicleIssueRepository.findByVehicleIdOrderByIssueDateDesc(vehicleId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VehicleIssueResponseDTO> getIssuesByDriver(Long driverId) {
        log.info("👤 Fetching issues for driver: {}", driverId);
        return vehicleIssueRepository.findByDriverIdOrderByIssueDateDesc(driverId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VehicleIssueResponseDTO getIssueById(Long issueId) {
        log.info("📋 Fetching vehicle issue: {}", issueId);
        VehicleIssue issue = vehicleIssueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Vehicle issue not found: " + issueId));
        return mapToResponseDTO(issue);
    }

    // ==================== Helper Methods ====================

    private String generateIssueNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(5);
        return ISSUE_NUMBER_PREFIX + timestamp;
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
        
        issue.setUpdatedAt(LocalDateTime.now());
        vehicleIssueRepository.save(issue);
    }

    private VehicleIssueResponseDTO mapToResponseDTO(VehicleIssue issue) {
        if (issue == null) {
            return null;
        }
        
        log.debug("🔄 Mapping issue: {}", issue.getId());
        
        List<VehicleIssueItem> items = vehicleIssueItemRepository.findByIssueId(issue.getId());
        log.debug("📦 Issue has {} items", items.size());
        
        List<VehicleIssueItemResponseDTO> itemDTOs = items.stream()
                .map(this::mapItemToResponseDTO)
                .collect(Collectors.toList());
        
        return VehicleIssueResponseDTO.builder()
                .id(issue.getId())
                .issueNumber(issue.getIssueNumber())
                .vehicleId(issue.getVehicleId())
                .driverId(issue.getDriverId())
                .tripId(issue.getTripId())
                .issueDate(issue.getIssueDate())
                .status(issue.getStatus())
                .notes(issue.getNotes())
                .items(itemDTOs)
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
