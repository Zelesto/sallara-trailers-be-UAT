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
public class DriverIssueService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DriverIssueService.class);

    private final DriverIssueRepository driverIssueRepository;
    private final DriverIssueItemRepository driverIssueItemRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockMovementRepository stockMovementRepository;

    private static final String ISSUE_NUMBER_PREFIX = "DI-";

    /**
     * Issue items to a driver
     */
    public DriverIssueResponseDTO issueItemsToDriver(DriverIssueRequestDTO request, Long userId) {
        log.info("👤 Issuing items to driver: {}", request.getDriverId());

        // Validate items
        for (DriverIssueItemRequestDTO itemReq : request.getItems()) {
            InventoryItem item = inventoryItemRepository.findById(itemReq.getItemId())
                    .orElseThrow(() -> new RuntimeException("Item not found: " + itemReq.getItemId()));

            if (item.getQuantity() < itemReq.getQuantity().intValue()) {
                throw new RuntimeException("Insufficient stock for item: " + item.getName() +
                        ". Available: " + item.getQuantity() + ", Requested: " + itemReq.getQuantity());
            }
        }

        // Create Driver Issue
        DriverIssue issue = DriverIssue.builder()
                .issueNumber(generateIssueNumber())
                .driverId(request.getDriverId())
                .issueDate(request.getIssueDate() != null ? request.getIssueDate() : LocalDateTime.now())
                .status("ISSUED")
                .notes(request.getNotes())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        driverIssueRepository.save(issue);

        // Process items
        for (DriverIssueItemRequestDTO itemReq : request.getItems()) {
            // Create issue item
            DriverIssueItem issueItem = DriverIssueItem.builder()
                    .issue(issue)
                    .itemId(itemReq.getItemId())
                    .quantityIssued(itemReq.getQuantity())
                    .quantityReturned(BigDecimal.ZERO)
                    .conditionIssued(itemReq.getCondition())
                    .notes(itemReq.getNotes())
                    .build();

            driverIssueItemRepository.save(issueItem);

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
        .reason("Driver Issue")
        .notes("Issued to driver: " + request.getDriverId())
        .referenceNumber(issue.getIssueNumber())
        .performedBy(String.valueOf(userId))
        .referenceType("DRIVER_ISSUE")
        .requiresApproval(false)
        .approvalStatus("APPROVED")
        .build();

        // ✅ Set driverId separately
        movement.setDriverId(request.getDriverId());
        
        stockMovementRepository.save(movement);
        }

        log.info("✅ Items issued to driver successfully. Issue Number: {}", issue.getIssueNumber());
        return mapToResponseDTO(issue);
    }


    
    /**
     * Return items from driver
     */
    public DriverIssueResponseDTO returnItemsFromDriver(Long issueId, List<ReturnItemRequestDTO> returns, Long userId) {
        log.info("🔄 Returning items from driver issue: {}", issueId);

        DriverIssue issue = driverIssueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Driver issue not found: " + issueId));

        for (ReturnItemRequestDTO returnReq : returns) {
            DriverIssueItem issueItem = driverIssueItemRepository
                    .findByIssueIdAndItemId(issueId, returnReq.getItemId())
                    .orElseThrow(() -> new RuntimeException("Item not found in issue: " + returnReq.getItemId()));

            // Update return quantity
            BigDecimal newReturned = issueItem.getQuantityReturned().add(returnReq.getQuantity());
            issueItem.setQuantityReturned(newReturned);
            issueItem.setConditionReturned(returnReq.getCondition());
            issueItem.setUpdatedAt(LocalDateTime.now());
            driverIssueItemRepository.save(issueItem);

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
        .reason("Driver Return")
        .notes("Returned from driver: " + issue.getDriverId())
        .referenceNumber(issue.getIssueNumber())
        .performedBy(String.valueOf(userId))
        .referenceType("DRIVER_RETURN")
        .requiresApproval(false)
        .approvalStatus("APPROVED")
        .build();

        // ✅ Set driverId separately
        movement.setDriverId(issue.getDriverId());
        
        stockMovementRepository.save(movement);
        }

        // Update issue status
        updateIssueStatus(issue);

        log.info("✅ Items returned from driver successfully. Issue Number: {}", issue.getIssueNumber());
        return mapToResponseDTO(issue);
    }

    /**
     * Swap an item - return damaged and issue replacement for driver
     */
    @Transactional
    public DriverIssueResponseDTO swapItem(Long oldIssueId, SwapItemRequestDTO swapRequest, Long userId) {
        log.info("🔄 Swapping item from driver issue: {}", oldIssueId);
        
        // 1. Find the existing issue
        DriverIssue oldIssue = driverIssueRepository.findById(oldIssueId)
                .orElseThrow(() -> new RuntimeException("Driver issue not found: " + oldIssueId));
        
        // 2. Find the specific item in the issue
        DriverIssueItem oldItem = driverIssueItemRepository
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
        driverIssueItemRepository.save(oldItem);
        
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
                .reason("Driver Swap Return - " + swapRequest.getDamagedCondition())
                .notes("Damaged item returned from driver. Hold code: " + swapRequest.getDamagedCondition())
                .referenceNumber(oldIssue.getIssueNumber())
                .performedBy(String.valueOf(userId))
                .referenceType("DRIVER_SWAP_RETURN")
                .requiresApproval(false)
                .approvalStatus("APPROVED")
                .build();
        stockMovementRepository.save(returnMovement);
        
        // 9. Create new issue for the replacement item
        DriverIssueRequestDTO newIssueRequest = new DriverIssueRequestDTO();
        newIssueRequest.setDriverId(oldIssue.getDriverId());
        newIssueRequest.setIssueDate(LocalDateTime.now());
        newIssueRequest.setNotes("SWAP: Replacing damaged item. Original Issue: " + oldIssue.getIssueNumber());
        
        DriverIssueItemRequestDTO newItemRequest = new DriverIssueItemRequestDTO();
        newItemRequest.setItemId(swapRequest.getNewItemId());
        newItemRequest.setQuantity(BigDecimal.valueOf(swapRequest.getNewQuantity()));
        newItemRequest.setCondition("NEW");
        newItemRequest.setNotes("Swap replacement for " + swapRequest.getDamagedCondition());
        
        newIssueRequest.setItems(List.of(newItemRequest));
        
        // 10. Create the new issue
        DriverIssueResponseDTO newIssue = issueItemsToDriver(newIssueRequest, userId);
        
        // 11. Link the new issue to the old one
        oldItem.setSwapIssueId(newIssue.getId());
        driverIssueItemRepository.save(oldItem);
        
        log.info("✅ Driver swap completed: Old issue {} returned, New issue {} created", oldIssueId, newIssue.getId());
        
        return newIssue;
    }

    @Transactional(readOnly = true)
    public List<DriverIssueResponseDTO> getAllDriverIssues() {
        log.info("📋 Fetching all driver issues");
        try {
            List<DriverIssue> issues = driverIssueRepository.findAllByOrderByIssueDateDesc();
            log.info("📋 Found {} driver issues in database", issues.size());
            return issues.stream()
                    .map(this::mapToResponseDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("❌ Error fetching driver issues: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public List<DriverIssueResponseDTO> getIssuesByDriver(Long driverId) {
        log.info("👤 Fetching issues for driver: {}", driverId);
        return driverIssueRepository.findByDriverIdOrderByIssueDateDesc(driverId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DriverIssueResponseDTO getIssueById(Long issueId) {
        log.info("📋 Fetching driver issue: {}", issueId);
        DriverIssue issue = driverIssueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Driver issue not found: " + issueId));
        return mapToResponseDTO(issue);
    }

    // ==================== Helper Methods ====================

    private String generateIssueNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(5);
        return ISSUE_NUMBER_PREFIX + timestamp;
    }

    private void updateIssueStatus(DriverIssue issue) {
        List<DriverIssueItem> items = driverIssueItemRepository.findByIssueId(issue.getId());
        
        boolean allReturned = true;
        boolean anyReturned = false;
        
        for (DriverIssueItem item : items) {
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
        driverIssueRepository.save(issue);
    }

    private DriverIssueResponseDTO mapToResponseDTO(DriverIssue issue) {
        List<DriverIssueItem> items = driverIssueItemRepository.findByIssueId(issue.getId());
        
        List<DriverIssueItemResponseDTO> itemDTOs = items.stream()
                .map(this::mapItemToResponseDTO)
                .collect(Collectors.toList());
        
        return DriverIssueResponseDTO.builder()
                .id(issue.getId())
                .issueNumber(issue.getIssueNumber())
                .driverId(issue.getDriverId())
                .issueDate(issue.getIssueDate())
                .status(issue.getStatus())
                .notes(issue.getNotes())
                .items(itemDTOs)
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .build();
    }

    private DriverIssueItemResponseDTO mapItemToResponseDTO(DriverIssueItem item) {
        InventoryItem inventoryItem = inventoryItemRepository.findById(item.getItemId()).orElse(null);
        
        return DriverIssueItemResponseDTO.builder()
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
