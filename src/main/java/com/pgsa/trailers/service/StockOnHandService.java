package com.pgsa.trailers.service.inventory;

import com.pgsa.trailers.dto.StockOnHandDTO;
import com.pgsa.trailers.dto.StockOnHandFilterDTO;
import com.pgsa.trailers.entity.inventory.DriverIssue;
import com.pgsa.trailers.entity.inventory.DriverIssueItem;
import com.pgsa.trailers.entity.inventory.InventoryItem;
import com.pgsa.trailers.entity.inventory.VehicleIssue;
import com.pgsa.trailers.entity.inventory.VehicleIssueItem;
import com.pgsa.trailers.repository.DriverIssueItemRepository;
import com.pgsa.trailers.repository.DriverIssueRepository;
import com.pgsa.trailers.repository.InventoryItemRepository;
import com.pgsa.trailers.repository.InventoryLocationRepository;
import com.pgsa.trailers.repository.VehicleIssueItemRepository;
import com.pgsa.trailers.repository.VehicleIssueRepository;
import com.pgsa.trailers.service.DriverService;
import com.pgsa.trailers.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Stock on hand = items currently held by vehicles and drivers (issued but not yet returned).
 *
 * Sources:
 *  - vehicle issues  (VehicleIssue / VehicleIssueItem) - holder is the vehicle, or the driver if
 *    the vehicle issue has no vehicle but has a driver
 *  - driver issues   (DriverIssue / DriverIssueItem)   - written by DriverIssueService
 *
 * Previously only vehicle issues were read, so anything issued through the driver-issue screen
 * never appeared in stock on hand.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StockOnHandService {

    private static final String STATUS_ISSUED = "ISSUED";
    private static final String STATUS_RETURNED = "RETURNED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final Set<String> CLOSED_STATUSES = Set.of(STATUS_RETURNED, STATUS_CANCELLED);

    private static final String HOLDER_VEHICLE = "VEHICLE";
    private static final String HOLDER_DRIVER = "DRIVER";

    private final VehicleIssueRepository vehicleIssueRepository;
    private final VehicleIssueItemRepository vehicleIssueItemRepository;
    private final DriverIssueRepository driverIssueRepository;
    private final DriverIssueItemRepository driverIssueItemRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final VehicleService vehicleService;
    private final DriverService driverService;

    /** Who holds the stock. */
    private record Holder(String type, Long id, String name, String identifier, String details) {}

    /** One outstanding issue line, from either source, before filtering. */
    private record Line(Holder holder,
                        Long lineId,
                        Long itemId,
                        BigDecimal issued,
                        BigDecimal returned,
                        String condition,
                        String notes,
                        StockOnHandDTO.StockOnHandDTOBuilder seed) {}

    public List<StockOnHandDTO> getStockOnHand(StockOnHandFilterDTO filter) {
        log.info("📊 Fetching stock on hand with filter: {}", filter);

        // Null-safe filter values (null / blank / "ALL" = no filter)
        String holderTypeFilter = filter != null ? filter.getHolderType() : null;
        Long holderIdFilter = filter != null ? filter.getHolderId() : null;
        Long itemIdFilter = filter != null ? filter.getItemId() : null;
        String categoryFilter = filter != null ? filter.getCategory() : null;
        String statusFilter = filter != null ? filter.getStatus() : null;
        String search = filter != null && filter.getSearch() != null
                ? filter.getSearch().trim().toLowerCase() : "";

        Map<Long, Holder> vehicleCache = new HashMap<>();
        Map<Long, Holder> driverCache = new HashMap<>();
        List<Line> lines = new ArrayList<>();

        // ---------- 1. Vehicle issues ----------
        List<VehicleIssue> vehicleIssues = vehicleIssueRepository.findByStatusNotIn(List.copyOf(CLOSED_STATUSES));
        log.info("📋 Found {} active vehicle issues", vehicleIssues.size());

        for (VehicleIssue issue : vehicleIssues) {
            Holder holder;
            if (issue.getVehicleId() != null) {
                holder = vehicleCache.computeIfAbsent(issue.getVehicleId(), this::resolveVehicle);
            } else if (issue.getDriverId() != null) {
                holder = driverCache.computeIfAbsent(issue.getDriverId(), this::resolveDriver);
            } else {
                log.warn("Vehicle issue {} has neither vehicle nor driver - skipped", issue.getId());
                continue;
            }
            for (VehicleIssueItem it : vehicleIssueItemRepository.findByIssueId(issue.getId())) {
                lines.add(new Line(holder, it.getId(), it.getItemId(),
                        it.getQuantityIssued(), it.getQuantityReturned(),
                        it.getConditionIssued(), it.getNotes(),
                        StockOnHandDTO.builder()
                                .issueDate(issue.getIssueDate())
                                .lastUpdated(issue.getUpdatedAt())));
            }
        }

        // ---------- 2. Driver issues ----------
        List<DriverIssue> driverIssues = driverIssueRepository.findAllByOrderByIssueDateDesc().stream()
                .filter(i -> i.getStatus() == null || !CLOSED_STATUSES.contains(i.getStatus()))
                .collect(Collectors.toList());
        log.info("📋 Found {} active driver issues", driverIssues.size());

        for (DriverIssue issue : driverIssues) {
            if (issue.getDriverId() == null) {
                log.warn("Driver issue {} has no driver - skipped", issue.getId());
                continue;
            }
            Holder holder = driverCache.computeIfAbsent(issue.getDriverId(), this::resolveDriver);
            for (DriverIssueItem it : driverIssueItemRepository.findByIssueId(issue.getId())) {
                lines.add(new Line(holder, it.getId(), it.getItemId(),
                        it.getQuantityIssued(), it.getQuantityReturned(),
                        it.getConditionIssued(), it.getNotes(),
                        StockOnHandDTO.builder()
                                .issueDate(issue.getIssueDate())
                                .lastUpdated(issue.getUpdatedAt())));
            }
        }

        // ---------- 3. Load all inventory items in ONE query ----------
        Set<Long> itemIds = lines.stream().map(Line::itemId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, InventoryItem> items = itemIds.isEmpty()
                ? Map.of()
                : inventoryItemRepository.findAllById(itemIds).stream()
                        .collect(Collectors.toMap(InventoryItem::getId, Function.identity()));

        // ---------- 4. Filter and map ----------
        List<StockOnHandDTO> results = new ArrayList<>();
        for (Line line : lines) {
            BigDecimal issued = nz(line.issued());
            BigDecimal outstanding = issued.subtract(nz(line.returned()));
            if (outstanding.compareTo(BigDecimal.ZERO) <= 0) continue;

            Holder h = line.holder();
            if (!matches(holderTypeFilter, h.type())) continue;
            if (holderIdFilter != null && !holderIdFilter.equals(h.id())) continue;
            if (itemIdFilter != null && !itemIdFilter.equals(line.itemId())) continue;

            InventoryItem item = items.get(line.itemId());
            if (item == null) {
                log.warn("Issue line {} refers to missing inventory item {} - skipped", line.lineId(), line.itemId());
                continue;
            }
            if (!matches(categoryFilter, item.getCategory())) continue;

            if (!search.isEmpty()) {
                boolean hit = contains(item.getName(), search)
                        || contains(item.getSku(), search)
                        || contains(h.identifier(), search)
                        || contains(h.name(), search);
                if (!hit) continue;
            }

            String status = determineStockStatus(item);
            if (!matches(statusFilter, status)) continue;

            BigDecimal unitCost = item.getUnitCost();
            results.add(line.seed()
                    .id(line.lineId())
                    .itemId(line.itemId())
                    .itemName(item.getName())
                    .itemSku(item.getSku())
                    .category(item.getCategory())
                    .unitOfMeasure(item.getUnitOfMeasure())
                    .holderType(h.type())
                    .holderId(h.id())
                    .holderName(h.name())
                    .holderIdentifier(h.identifier())
                    .holderDetails(h.details())
                    .holderDisplayName(h.identifier()
                            + (h.details() != null && !h.details().isBlank() ? " (" + h.details() + ")" : ""))
                    .quantityOnHand(item.getQuantity() != null ? item.getQuantity() : 0)
                    .quantityIssued(issued.intValue())
                    .quantityReturned(nz(line.returned()).intValue())
                    .quantityOutstanding(outstanding.intValue())
                    .unitCost(unitCost)
                    // exact outstanding (no int truncation) for the value
                    .totalValue(unitCost != null ? unitCost.multiply(outstanding) : BigDecimal.ZERO)
                    .status(status)
                    .isHeld(Boolean.TRUE.equals(item.getIsHeld()))
                    .holdReason(item.getHoldReason())
                    .condition(line.condition())
                    .notes(line.notes())
                    .build());
        }

        log.info("📊 Found {} stock on hand records", results.size());
        return results;
    }

    // ============================================================
    // Holder lookups (cached per request; one missing vehicle/driver
    // must not break the whole report)
    // ============================================================

    private Holder resolveVehicle(Long id) {
        String fallback = "Vehicle #" + id;
        try {
            var vehicle = vehicleService.getVehicleById(id);
            if (vehicle == null) return new Holder(HOLDER_VEHICLE, id, fallback, fallback, "");
            String reg = vehicle.getRegistrationNumber() != null ? vehicle.getRegistrationNumber() : fallback;
            String details = (nzs(vehicle.getMake()) + " " + nzs(vehicle.getModel())).trim();
            return new Holder(HOLDER_VEHICLE, id, reg, reg, details);
        } catch (RuntimeException e) {
            log.warn("Vehicle {} could not be loaded for stock on hand: {}", id, e.getMessage());
            return new Holder(HOLDER_VEHICLE, id, fallback, fallback, "");
        }
    }

    private Holder resolveDriver(Long id) {
        String fallback = "Driver #" + id;
        try {
            var driver = driverService.getDriverById(id);
            if (driver == null) return new Holder(HOLDER_DRIVER, id, fallback, fallback, "");
            String name = driver.getFullName() != null ? driver.getFullName() : fallback;
            String details = driver.getEmployeeId() != null ? "Employee #" + driver.getEmployeeId() : "";
            return new Holder(HOLDER_DRIVER, id, name, name, details);
        } catch (RuntimeException e) {
            log.warn("Driver {} could not be loaded for stock on hand: {}", id, e.getMessage());
            return new Holder(HOLDER_DRIVER, id, fallback, fallback, "");
        }
    }

    // ============================================================
    // Helpers
    // ============================================================

    private String determineStockStatus(InventoryItem item) {
        if (Boolean.TRUE.equals(item.getIsHeld())) {
            return "ON_HOLD";
        }
        int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
        if (quantity <= 0) {
            return "OUT_OF_STOCK";
        }
        int minLevel = item.getMinLevel() != null ? item.getMinLevel() : 0;
        if (quantity <= minLevel) {
            return "LOW_STOCK";
        }
        return "IN_STOCK";
    }

    /** null / blank / "ALL" means "no filter". Case-insensitive. */
    private static boolean matches(String filterValue, String actual) {
        if (filterValue == null || filterValue.isBlank() || "ALL".equalsIgnoreCase(filterValue)) return true;
        return filterValue.equalsIgnoreCase(actual);
    }

    private static boolean contains(String value, String searchLower) {
        return value != null && value.toLowerCase().contains(searchLower);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String nzs(String v) {
        return v != null ? v : "";
    }
}
