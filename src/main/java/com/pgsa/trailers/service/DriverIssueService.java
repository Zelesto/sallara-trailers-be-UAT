package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.*;
import com.pgsa.trailers.entity.inventory.*;
import com.pgsa.trailers.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Issue / return / swap of inventory items to drivers.
 *
 * Invariants enforced here:
 *  - quantities are positive whole numbers (inventory stock is an Integer)
 *  - stock can never go negative, also under concurrent requests (rows are locked)
 *  - a driver can never return more than is outstanding
 *  - closed issues (RETURNED / CANCELLED) cannot be returned against
 *  - one line per item per issue (duplicate lines in a request are merged)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DriverIssueService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DriverIssueService.class);

    private static final String ISSUE_NUMBER_PREFIX = "DI-";
    private static final String STATUS_ISSUED = "ISSUED";
    private static final String STATUS_PARTIALLY_RETURNED = "PARTIALLY_RETURNED";
    private static final String STATUS_RETURNED = "RETURNED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final DriverIssueRepository driverIssueRepository;
    private final DriverIssueItemRepository driverIssueItemRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockMovementRepository stockMovementRepository;
    // Used to lock inventory rows (SELECT ... FOR UPDATE) without needing a custom repository method
    private final EntityManager entityManager;

    // ============================================================
    // ISSUE
    // ============================================================

    public DriverIssueResponseDTO issueItemsToDriver(DriverIssueRequestDTO request, Long userId) {
        if (request == null || request.getDriverId() == null) {
            throw new IllegalArgumentException("driverId is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one item is required");
        }
        log.info("👤 Issuing items to driver: {}", request.getDriverId());

        // Merge duplicate lines for the same item (keeps first line's condition/notes)
        Map<Long, DriverIssueItemRequestDTO> firstLine = new LinkedHashMap<>();
        Map<Long, Integer> qtyByItem = new LinkedHashMap<>();
        for (DriverIssueItemRequestDTO line : request.getItems()) {
            if (line.getItemId() == null) {
                throw new IllegalArgumentException("itemId is required on every line");
            }
            int qty = wholePositive(line.getQuantity(), "Issue quantity for item " + line.getItemId());
            firstLine.putIfAbsent(line.getItemId(), line);
            qtyByItem.merge(line.getItemId(), qty, Integer::sum);
        }

        // Lock items (sorted to avoid deadlocks) and validate stock BEFORE writing anything
        Map<Long, InventoryItem> locked = lockItems(qtyByItem.keySet());
        for (Map.Entry<Long, Integer> e : qtyByItem.entrySet()) {
            InventoryItem item = locked.get(e.getKey());
            int available = item.getQuantity() != null ? item.getQuantity() : 0;
            if (available < e.getValue()) {
                throw new RuntimeException("Insufficient stock for item: " + item.getName() +
                        ". Available: " + available + ", Requested: " + e.getValue());
            }
        }

        DriverIssue issue = DriverIssue.builder()
                .issueNumber(generateIssueNumber())
                .driverId(request.getDriverId())
                .issueDate(request.getIssueDate() != null ? request.getIssueDate() : LocalDateTime.now())
                .status(STATUS_ISSUED)
                .notes(request.getNotes())
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        driverIssueRepository.save(issue);

        for (Map.Entry<Long, Integer> e : qtyByItem.entrySet()) {
            Long itemId = e.getKey();
            int qty = e.getValue();
            DriverIssueItemRequestDTO src = firstLine.get(itemId);

            driverIssueItemRepository.save(DriverIssueItem.builder()
                    .issue(issue)
                    .itemId(itemId)
                    .quantityIssued(BigDecimal.valueOf(qty))
                    .quantityReturned(BigDecimal.ZERO)
                    .conditionIssued(src.getCondition())
                    .notes(src.getNotes())
                    .build());

            InventoryItem item = locked.get(itemId);
            item.setQuantity((item.getQuantity() != null ? item.getQuantity() : 0) - qty);
            inventoryItemRepository.save(item);

            saveMovement(itemId, qty, "OUT", "Driver Issue",
                    "Issued to driver: " + request.getDriverId(),
                    issue.getIssueNumber(), "DRIVER_ISSUE", userId, request.getDriverId());
        }

        log.info("✅ Items issued to driver successfully. Issue Number: {}", issue.getIssueNumber());
        return mapToResponseDTO(issue);
    }

    // ============================================================
    // RETURN
    // ============================================================

    public DriverIssueResponseDTO returnItemsFromDriver(Long issueId, List<ReturnItemRequestDTO> returns, Long userId) {
        if (returns == null || returns.isEmpty()) {
            throw new IllegalArgumentException("At least one return line is required");
        }
        log.info("🔄 Returning items from driver issue: {}", issueId);

        DriverIssue issue = driverIssueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Driver issue not found: " + issueId));
        assertOpen(issue);

        // Validate totals per item against what is outstanding, before writing anything
        Map<Long, Integer> qtyByItem = new LinkedHashMap<>();
        for (ReturnItemRequestDTO r : returns) {
            if (r.getItemId() == null) {
                throw new IllegalArgumentException("itemId is required on every return line");
            }
            int qty = wholePositive(r.getQuantity(), "Return quantity for item " + r.getItemId());
            qtyByItem.merge(r.getItemId(), qty, Integer::sum);
        }
        Map<Long, DriverIssueItem> issueLines = new HashMap<>();
        for (Map.Entry<Long, Integer> e : qtyByItem.entrySet()) {
            DriverIssueItem line = driverIssueItemRepository.findByIssueIdAndItemId(issueId, e.getKey())
                    .orElseThrow(() -> new RuntimeException("Item not found in issue: " + e.getKey()));
            BigDecimal outstanding = outstanding(line);
            if (BigDecimal.valueOf(e.getValue()).compareTo(outstanding) > 0) {
                throw new IllegalArgumentException("Cannot return " + e.getValue() + " of item " + e.getKey()
                        + " on issue " + issue.getIssueNumber() + ": only " + outstanding.toPlainString()
                        + " outstanding");
            }
            issueLines.put(e.getKey(), line);
        }

        Map<Long, InventoryItem> locked = lockItems(qtyByItem.keySet());

        for (ReturnItemRequestDTO r : returns) {
            int qty = r.getQuantity().intValueExact();
            DriverIssueItem line = issueLines.get(r.getItemId());
            line.setQuantityReturned(nz(line.getQuantityReturned()).add(BigDecimal.valueOf(qty)));
            line.setConditionReturned(r.getCondition());
            line.setUpdatedAt(LocalDateTime.now());
            driverIssueItemRepository.save(line);

            InventoryItem item = locked.get(r.getItemId());
            item.setQuantity((item.getQuantity() != null ? item.getQuantity() : 0) + qty);
            inventoryItemRepository.save(item);

            saveMovement(r.getItemId(), qty, "IN", "Driver Return",
                    "Returned from driver: " + issue.getDriverId(),
                    issue.getIssueNumber(), "DRIVER_RETURN", userId, issue.getDriverId());
        }

        updateIssueStatus(issue);
        log.info("✅ Items returned from driver successfully. Issue Number: {}", issue.getIssueNumber());
        return mapToResponseDTO(issue);
    }

    // ============================================================
    // SWAP (return damaged + issue replacement)
    // ============================================================

    public DriverIssueResponseDTO swapItem(Long oldIssueId, SwapItemRequestDTO swapRequest, Long userId) {
        log.info("🔄 Swapping item from driver issue: {}", oldIssueId);

        DriverIssue oldIssue = driverIssueRepository.findById(oldIssueId)
                .orElseThrow(() -> new RuntimeException("Driver issue not found: " + oldIssueId));
        assertOpen(oldIssue);

        DriverIssueItem oldItem = driverIssueItemRepository
                .findByIssueIdAndItemId(oldIssueId, swapRequest.getOldItemId())
                .orElseThrow(() -> new RuntimeException("Item not found in issue: " + swapRequest.getOldItemId()));

        BigDecimal outstanding = outstanding(oldItem);
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Item already returned, cannot swap");
        }

        // Default = everything still outstanding (was: full issued qty, which over-returned
        // when part had already been returned)
        int returnQty = swapRequest.getReturnQuantity() != null
                ? wholePositive(swapRequest.getReturnQuantity(), "Swap return quantity")
                : wholePositive(outstanding, "Swap return quantity");
        if (BigDecimal.valueOf(returnQty).compareTo(outstanding) > 0) {
            throw new IllegalArgumentException("Cannot return " + returnQty + ": only "
                    + outstanding.toPlainString() + " outstanding");
        }
        Object newQty = swapRequest.getNewQuantity(); // works whether the DTO uses int, Integer, long or Long
        if (swapRequest.getNewItemId() == null || newQty == null || ((Number) newQty).longValue() <= 0) {
            throw new IllegalArgumentException("newItemId and a positive newQuantity are required");
        }

        // 1. Return the old item
        oldItem.setQuantityReturned(nz(oldItem.getQuantityReturned()).add(BigDecimal.valueOf(returnQty)));
        oldItem.setConditionReturned(swapRequest.getDamagedCondition());
        oldItem.setIsSwap(true);
        oldItem.setSwapReason(swapRequest.getDamagedCondition());
        oldItem.setUpdatedAt(LocalDateTime.now());
        driverIssueItemRepository.save(oldItem);

        // 2. Hold record + put back into stock.
        // OPEN QUESTION (unchanged behaviour): the damaged units are added back to the item's
        // available quantity, and the hold is set on the whole item (all units). Confirm the rule.
        InventoryItem inventoryItem = lockItems(Set.of(swapRequest.getOldItemId())).get(swapRequest.getOldItemId());
        inventoryItem.setHoldCode(swapRequest.getDamagedCondition());
        inventoryItem.setHoldReason(swapRequest.getDamageNotes());
        inventoryItem.setHoldDate(LocalDateTime.now());
        inventoryItem.setHeldBy(String.valueOf(userId));
        int current = inventoryItem.getQuantity() != null ? inventoryItem.getQuantity() : 0;
        inventoryItem.setQuantity(current + returnQty);
        inventoryItemRepository.save(inventoryItem);

        saveMovement(swapRequest.getOldItemId(), returnQty, "IN",
                "Driver Swap Return - " + swapRequest.getDamagedCondition(),
                "Damaged item returned from driver. Hold code: " + swapRequest.getDamagedCondition(),
                oldIssue.getIssueNumber(), "DRIVER_SWAP_RETURN", userId, oldIssue.getDriverId());

        updateIssueStatus(oldIssue);

        // 3. Issue the replacement (stock is locked & validated inside)
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

        DriverIssueResponseDTO newIssue = issueItemsToDriver(newIssueRequest, userId);

        oldItem.setSwapIssueId(newIssue.getId());
        driverIssueItemRepository.save(oldItem);

        log.info("✅ Driver swap completed: Old issue {} returned, New issue {} created", oldIssueId, newIssue.getId());
        return newIssue;
    }

    // ============================================================
    // QUERIES
    // ============================================================

    @Transactional(readOnly = true)
    public List<DriverIssueResponseDTO> getAllDriverIssues() {
        log.info("📋 Fetching all driver issues");
        // Errors are no longer swallowed into an empty list.
        List<DriverIssue> issues = driverIssueRepository.findAllByOrderByIssueDateDesc();
        log.info("📋 Found {} driver issues in database", issues.size());
        return issues.stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DriverIssueResponseDTO> getIssuesByDriver(Long driverId) {
        log.info("👤 Fetching issues for driver: {}", driverId);
        return driverIssueRepository.findByDriverIdOrderByIssueDateDesc(driverId)
                .stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DriverIssueResponseDTO getIssueById(Long issueId) {
        log.info("📋 Fetching driver issue: {}", issueId);
        DriverIssue issue = driverIssueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Driver issue not found: " + issueId));
        return mapToResponseDTO(issue);
    }

    // ============================================================
    // HELPERS
    // ============================================================

    /** Locks inventory rows (SELECT ... FOR UPDATE) in id order to prevent overselling and deadlocks. */
    private Map<Long, InventoryItem> lockItems(Collection<Long> itemIds) {
        Map<Long, InventoryItem> locked = new HashMap<>();
        for (Long id : new TreeSet<>(itemIds)) {
            InventoryItem item = entityManager.find(InventoryItem.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (item == null) {
                throw new RuntimeException("Item not found: " + id);
            }
            locked.put(id, item);
        }
        return locked;
    }

    private void assertOpen(DriverIssue issue) {
        if (STATUS_RETURNED.equals(issue.getStatus()) || STATUS_CANCELLED.equals(issue.getStatus())) {
            throw new IllegalStateException("Issue " + issue.getIssueNumber() + " is " + issue.getStatus()
                    + " and cannot be changed");
        }
    }

    /** Inventory quantity is an Integer, so issue/return quantities must be whole and positive. */
    private static int wholePositive(BigDecimal qty, String label) {
        if (qty == null || qty.signum() <= 0) {
            throw new IllegalArgumentException(label + " must be greater than 0");
        }
        try {
            return qty.stripTrailingZeros().intValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(label + " must be a whole number (got " + qty.toPlainString() + ")");
        }
    }

    private static BigDecimal outstanding(DriverIssueItem line) {
        return nz(line.getQuantityIssued()).subtract(nz(line.getQuantityReturned()));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private void saveMovement(Long itemId, int qty, String type, String reason, String notes,
                              String referenceNumber, String referenceType, Long userId, Long driverId) {
        StockMovement movement = StockMovement.builder()
                .itemId(itemId)
                .quantity(qty)
                .movementType(type)
                .reason(reason)
                .notes(notes)
                .referenceNumber(referenceNumber)
                .performedBy(String.valueOf(userId))
                .referenceType(referenceType)
                .requiresApproval(false)
                .approvalStatus("APPROVED")
                .build();
        movement.setDriverId(driverId);
        stockMovementRepository.save(movement);
    }

    // KNOWN ISSUE (open): last 8 digits of epoch millis -> repeats every ~27.7 hours.
    // Left unchanged pending column length / SequenceService confirmation.
    private String generateIssueNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(5);
        return ISSUE_NUMBER_PREFIX + timestamp;
    }

    private void updateIssueStatus(DriverIssue issue) {
        List<DriverIssueItem> items = driverIssueItemRepository.findByIssueId(issue.getId());

        boolean allReturned = !items.isEmpty();
        boolean anyReturned = false;
        for (DriverIssueItem item : items) {
            if (nz(item.getQuantityReturned()).compareTo(nz(item.getQuantityIssued())) < 0) {
                allReturned = false;
            }
            if (nz(item.getQuantityReturned()).compareTo(BigDecimal.ZERO) > 0) {
                anyReturned = true;
            }
        }

        if (allReturned) {
            issue.setStatus(STATUS_RETURNED);
        } else if (anyReturned) {
            issue.setStatus(STATUS_PARTIALLY_RETURNED);
        } else {
            issue.setStatus(STATUS_ISSUED);
        }
        issue.setUpdatedAt(LocalDateTime.now());
        driverIssueRepository.save(issue);
    }

    private DriverIssueResponseDTO mapToResponseDTO(DriverIssue issue) {
        List<DriverIssueItem> items = driverIssueItemRepository.findByIssueId(issue.getId());

        Set<Long> itemIds = items.stream().map(DriverIssueItem::getItemId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, InventoryItem> inv = itemIds.isEmpty() ? Map.of()
                : inventoryItemRepository.findAllById(itemIds).stream()
                        .collect(Collectors.toMap(InventoryItem::getId, i -> i));

        List<DriverIssueItemResponseDTO> itemDTOs = items.stream()
                .map(i -> mapItemToResponseDTO(i, inv.get(i.getItemId())))
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

    private DriverIssueItemResponseDTO mapItemToResponseDTO(DriverIssueItem item, InventoryItem inventoryItem) {
        return DriverIssueItemResponseDTO.builder()
                .id(item.getId())
                .itemId(item.getItemId())
                .itemName(inventoryItem != null ? inventoryItem.getName() : "Unknown")
                .itemCategory(inventoryItem != null ? inventoryItem.getCategory() : null)
                .quantityIssued(item.getQuantityIssued())
                .quantityReturned(item.getQuantityReturned())
                .quantityOutstanding(outstanding(item))
                .conditionIssued(item.getConditionIssued())
                .conditionReturned(item.getConditionReturned())
                .notes(item.getNotes())
                .build();
    }
}
