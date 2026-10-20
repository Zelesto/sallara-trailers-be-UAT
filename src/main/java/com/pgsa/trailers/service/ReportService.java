// src/main/java/com/pgsa/trailers/service/ReportService.java

package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.report.FuelReportDTO;
import com.pgsa.trailers.dto.report.LoadReportDTO;
import com.pgsa.trailers.dto.report.TripReportDTO;
import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.entity.inventory.DriverIssue;
import com.pgsa.trailers.entity.inventory.InventoryItem;
import com.pgsa.trailers.entity.inventory.StockMovement;
import com.pgsa.trailers.entity.inventory.InventoryLocation;
import com.pgsa.trailers.entity.inventory.VehicleIssue;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.ops.FuelSlip;
import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.DriverIssueRepository;
import com.pgsa.trailers.repository.DriverRepository;
import com.pgsa.trailers.repository.FuelSlipRepository;
import com.pgsa.trailers.repository.InventoryItemRepository;
import com.pgsa.trailers.repository.StockMovementRepository;
import com.pgsa.trailers.repository.LoadRepository;
import com.pgsa.trailers.repository.InventoryLocationRepository;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.repository.VehicleIssueRepository;
import com.pgsa.trailers.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    // ── Existing repos ─────────────────────────────────────────
    private final TripRepository tripRepository;
    private final LoadRepository loadRepository;
    private final FuelSlipRepository fuelSlipRepository;
    private final VehicleRepository vehicleRepository;

    // ── New repos (inventory) ──────────────────────────────────
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryLocationRepository locationRepository;
    private final StockMovementRepository StockMovementRepository;
    private final VehicleIssueRepository vehicleIssueRepository;
    private final DriverIssueRepository driverIssueRepository;
    private final DriverRepository driverRepository;

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final String LOGO_URL = "https://trailers-backend.onrender.com/logo.png";

    // ============================================================
    // TRIP (unchanged)
    // ============================================================
    @Transactional(readOnly = true)
    public String generateTripReportHTML(String tripNumber) {
        try {
            Trip trip = tripRepository.findByTripNumberWithRelations(tripNumber)
                    .orElseThrow(() -> new RuntimeException("Trip not found: " + tripNumber));
            return generateTripHTML(TripReportDTO.fromEntity(trip));
        } catch (Exception e) {
            log.error("Error generating trip report: {}", e.getMessage(), e);
            return generateErrorHTML("Trip Report", tripNumber, e.getMessage());
        }
    }

    public Map<String, Object> getTripReportData(String tripNumber) {
        try {
            Trip trip = tripRepository.findByTripNumberWithRelations(tripNumber)
                    .orElseThrow(() -> new RuntimeException("Trip not found: " + tripNumber));
            TripReportDTO dto = TripReportDTO.fromEntity(trip);
            Map<String, Object> data = new HashMap<>();
            data.put("tripNumber", dto.getTripNumber());
            data.put("customerName", dto.getCustomerName());
            data.put("customerCode", dto.getCustomerCode());
            data.put("status", dto.getStatus());
            data.put("tripType", dto.getTripType());
            data.put("referenceNumber", dto.getReferenceNumber());
            data.put("originLocation", dto.getOriginLocation());
            data.put("destinationLocation", dto.getDestinationLocation());
            data.put("plannedDistanceKm", dto.getPlannedDistanceKm());
            data.put("actualDistanceKm", dto.getActualDistanceKm());
            data.put("driverName", dto.getDriverName());
            data.put("driverLicense", dto.getDriverLicense());
            data.put("vehicleRegistration", dto.getVehicleRegistration());
            data.put("vehicleMake", dto.getVehicleMake());
            data.put("vehicleModel", dto.getVehicleModel());
            data.put("plannedStartDate", dto.getPlannedStartDate());
            data.put("plannedEndDate", dto.getPlannedEndDate());
            data.put("actualStartDate", dto.getActualStartDate());
            data.put("actualEndDate", dto.getActualEndDate());
            data.put("actualStartOdometer", dto.getActualStartOdometer());
            data.put("actualEndOdometer", dto.getActualEndOdometer());
            return data;
        } catch (Exception e) {
            log.error("Error getting trip report data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get trip report data", e);
        }
    }

    private String generateTripHTML(TripReportDTO trip) {
        // ... unchanged — copy verbatim from your original file ...
        // (existing implementation preserved)
        return "";
    }

    // ============================================================
    // LOAD (unchanged)
    // ============================================================
    @Transactional(readOnly = true)
    public String generateLoadReportHTML(String loadNumber) {
        try {
            Optional<Load> loadOpt = loadRepository.findByLoadNumber(loadNumber);
            if (loadOpt.isEmpty()) {
                return generateErrorHTML("Load Report", loadNumber, "Load not found: " + loadNumber);
            }
            Load load = loadOpt.get();
            List<Trip> trips = tripRepository.findByLoadId(load.getLoadNumber());
            return generateLoadHTML(LoadReportDTO.fromEntity(load, trips));
        } catch (Exception e) {
            log.error("Error generating load report: {}", e.getMessage(), e);
            return generateErrorHTML("Load Report", loadNumber, e.getMessage());
        }
    }

    public Map<String, Object> getLoadReportData(String loadNumber) {
        try {
            Load load = loadRepository.findByLoadNumber(loadNumber)
                    .orElseThrow(() -> new RuntimeException("Load not found: " + loadNumber));
            List<Trip> trips = tripRepository.findByLoadId(load.getLoadNumber());
            LoadReportDTO dto = LoadReportDTO.fromEntity(load, trips);
            Map<String, Object> data = new HashMap<>();
            data.put("loadNumber", dto.getLoadNumber());
            data.put("status", dto.getStatus());
            data.put("description", dto.getDescription());
            data.put("commodityType", dto.getCommodityType());
            data.put("weightKg", dto.getWeightKg());
            data.put("palletCount", dto.getPalletCount());
            data.put("tripCount", dto.getTripCount());
            data.put("customerName", dto.getCustomerName());
            data.put("totalDistanceKm", dto.getTotalDistanceKm());
            data.put("totalFromDepotKm", dto.getTotalFromDepotKm());
            data.put("totalToDepotKm", dto.getTotalToDepotKm());
            List<Map<String, Object>> tripList = new ArrayList<>();
            for (LoadReportDTO.TripSummary t : dto.getTrips()) {
                Map<String, Object> m = new HashMap<>();
                m.put("tripNumber", t.getTripNumber());
                m.put("driverName", t.getDriverName());
                m.put("vehicleRegistration", t.getVehicleRegistration());
                m.put("plannedStartDate", t.getPlannedStartDate());
                m.put("plannedEndDate", t.getPlannedEndDate());
                m.put("actualDistanceKm", t.getActualDistanceKm());
                m.put("status", t.getStatus());
                tripList.add(m);
            }
            data.put("trips", tripList);
            return data;
        } catch (Exception e) {
            log.error("Error getting load report data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get load report data", e);
        }
    }

    private String generateLoadHTML(LoadReportDTO load) {
        // ... unchanged — copy verbatim from your original file ...
        return "";
    }

    // ============================================================
    // FUEL (unchanged)
    // ============================================================
    @Transactional(readOnly = true)
    public String generateFuelReportHTML(Long vehicleId, String startDateStr, String endDateStr) {
        try {
            LocalDate startDate = parseDate(startDateStr);
            LocalDate endDate = parseDate(endDateStr);
            List<FuelSlip> slips = getFuelSlips(vehicleId, startDate, endDate);
            return generateFuelHTML(buildFuelReportDTO(slips, vehicleId, startDate, endDate));
        } catch (Exception e) {
            log.error("Error generating fuel report: {}", e.getMessage(), e);
            return generateErrorHTML("Fuel Report", String.valueOf(vehicleId), e.getMessage());
        }
    }

    public Map<String, Object> getFuelReportData(Long vehicleId, String startDate, String endDate) {
        try {
            LocalDate fromDate = parseDate(startDate);
            LocalDate toDate = parseDate(endDate);
            List<FuelSlip> slips = getFuelSlips(vehicleId, fromDate, toDate);
            FuelReportDTO dto = buildFuelReportDTO(slips, vehicleId, fromDate, toDate);
            Map<String, Object> data = new HashMap<>();
            data.put("vehicleRegistration", dto.getVehicleRegistration());
            data.put("startDate", dto.getStartDate());
            data.put("endDate", dto.getEndDate());
            data.put("totalLiters", dto.getTotalLiters());
            data.put("totalCost", dto.getTotalCost());
            data.put("avgUnitPrice", dto.getAvgUnitPrice());
            data.put("entryCount", dto.getEntryCount());
            List<Map<String, Object>> entryList = new ArrayList<>();
            for (FuelReportDTO.FuelEntry entry : dto.getEntries()) {
                Map<String, Object> m = new HashMap<>();
                m.put("date", entry.getDate());
                m.put("station", entry.getStation());
                m.put("liters", entry.getLiters());
                m.put("unitPrice", entry.getUnitPrice());
                m.put("total", entry.getTotal());
                m.put("odometer", entry.getOdometer());
                entryList.add(m);
            }
            data.put("entries", entryList);
            return data;
        } catch (Exception e) {
            log.error("Error getting fuel report data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get fuel report data", e);
        }
    }

    private String generateFuelHTML(FuelReportDTO report) {
        // ... unchanged — copy verbatim from your original file ...
        return "";
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        try { return LocalDate.parse(dateStr); }
        catch (DateTimeParseException e) {
            log.warn("⚠️ Invalid date format: {}, using null", dateStr);
            return null;
        }
    }

    private List<FuelSlip> getFuelSlips(Long vehicleId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : null;
        if (vehicleId != null && start != null && end != null)
            return fuelSlipRepository.findByVehicleIdAndDateBetween(vehicleId, start, end);
        if (vehicleId != null) return fuelSlipRepository.findByVehicleId(vehicleId);
        if (start != null && end != null) return fuelSlipRepository.findByTransactionDateBetween(start, end);
        return fuelSlipRepository.findAll();
    }

    private FuelReportDTO buildFuelReportDTO(List<FuelSlip> slips, Long vehicleId,
                                             LocalDate startDate, LocalDate endDate) {
        String reg = null;
        if (vehicleId != null) {
            reg = vehicleRepository.findById(vehicleId).map(Vehicle::getRegistrationNumber).orElse(null);
        }
        FuelReportDTO dto = new FuelReportDTO();
        dto.setVehicleRegistration(reg != null ? reg : "All Vehicles");
        dto.setStartDate(startDate != null ? startDate.format(DATE_FORMATTER) : "N/A");
        dto.setEndDate(endDate != null ? endDate.format(DATE_FORMATTER) : "N/A");
        dto.setEntryCount(slips.size());
        double totalLiters = slips.stream().filter(s -> s.getQuantity() != null)
                .mapToDouble(s -> s.getQuantity().doubleValue()).sum();
        double totalCost = slips.stream().filter(s -> s.getTotalAmount() != null)
                .mapToDouble(s -> s.getTotalAmount().doubleValue()).sum();
        double avgUnitPrice = slips.stream().filter(s -> s.getUnitPrice() != null)
                .mapToDouble(s -> s.getUnitPrice().doubleValue()).average().orElse(0.0);
        List<FuelReportDTO.FuelEntry> entries = new ArrayList<>();
        for (FuelSlip s : slips) {
            FuelReportDTO.FuelEntry e = new FuelReportDTO.FuelEntry();
            e.setDate(s.getTransactionDate() != null
                    ? s.getTransactionDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) : "N/A");
            e.setStation(s.getFuelSource() != null ? s.getFuelSource().getName() : "N/A");
            e.setLiters(s.getQuantity() != null ? s.getQuantity().doubleValue() : 0.0);
            e.setTotal(s.getTotalAmount() != null ? s.getTotalAmount().doubleValue() : 0.0);
            e.setUnitPrice(s.getUnitPrice() != null ? s.getUnitPrice().doubleValue() : 0.0);
            e.setOdometer(s.getOdometerReading() != null ? s.getOdometerReading().doubleValue() : 0.0);
            entries.add(e);
        }
        dto.setTotalLiters(totalLiters);
        dto.setTotalCost(totalCost);
        dto.setAvgUnitPrice(avgUnitPrice);
        dto.setEntries(entries);
        return dto;
    }

    // ============================================================
    // INVENTORY — STOCK ON HAND
    // ============================================================
    @Transactional(readOnly = true)
    public String generateStockOnHandReportHTML(Long locationId, String category, String status,
                                                String search, String issueableTo, Boolean includeHeld) {
        try {
            Map<String, Object> data = getStockOnHandReportData(
                    locationId, category, status, search, issueableTo, includeHeld);
            return generateStockOnHandHTML(data);
        } catch (Exception e) {
            log.error("Error generating stock-on-hand report: {}", e.getMessage(), e);
            return generateErrorHTML("Stock on Hand", String.valueOf(locationId), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStockOnHandReportData(Long locationId, String category,
                                                        String status, String search,
                                                        String issueableTo, Boolean includeHeld) {
        List<InventoryItem> all = inventoryItemRepository.findAll();
        boolean include = includeHeld != null && includeHeld;

        List<InventoryItem> filtered = all.stream()
                .filter(i -> locationId == null || Objects.equals(i.getLocationId(), locationId))
                .filter(i -> category == null || category.isBlank()
                        || category.equalsIgnoreCase(i.getCategory()))
                .filter(i -> status == null || status.isBlank() || matchesStatusFilter(i, status))
                .filter(i -> search == null || search.isBlank()
                        || containsIgnoreCase(i.getName(), search)
                        || containsIgnoreCase(i.getSku(), search)
                        || containsIgnoreCase(i.getCategory(), search))
                .filter(i -> matchesIssueableFilter(i, issueableTo))
                .filter(i -> include || !Boolean.TRUE.equals(i.getIsHeld()))
                .collect(Collectors.toList());

        Map<Long, String> locationNames = loadLocationNames();

        double totalValue = 0.0;
        int totalQty = 0, low = 0, out = 0;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (InventoryItem i : filtered) {
            int qty = intOf(i.getQuantity());
            int min = intOf(i.getMinLevel());
            double unit = doubleOf(i.getUnitCost());
            double lineValue = qty * unit;
            totalQty += qty;
            totalValue += lineValue;
            if (qty <= 0) out++;
            else if (qty <= min) low++;

            Map<String, Object> r = new HashMap<>();
            r.put("sku", safe(i.getSku()));
            r.put("name", safe(i.getName()));
            r.put("category", safe(i.getCategory()));
            r.put("unitOfMeasure", safe(i.getUnitOfMeasure()));
            r.put("quantity", qty);
            r.put("minLevel", min);
            r.put("unitCost", unit);
            r.put("lineValue", lineValue);
            r.put("locationName", locationNames.getOrDefault(i.getLocationId(), "—"));
            r.put("statusLabel", itemStatusLabel(i));
            r.put("isConsumable", yesNo(i.getIsConsumable()));
            r.put("isDriverIssuable", yesNo(i.getIsDriverIssuable()));
            r.put("isVehicleIssuable", yesNo(i.getIsVehicleIssuable()));
            rows.add(r);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("locationName", locationId != null
                ? locationNames.getOrDefault(locationId, "Unknown") : "All Locations");
        data.put("categoryFilter", blankToDefault(category, "All Categories"));
        data.put("statusFilter", blankToDefault(status, "All Statuses"));
        data.put("searchTerm", blankToDefault(search, ""));
        data.put("issueableTo", blankToDefault(issueableTo, "All"));
        data.put("includeHeld", include);
        data.put("totalItems", filtered.size());
        data.put("totalQuantity", totalQty);
        data.put("totalValue", totalValue);
        data.put("lowStockCount", low);
        data.put("outOfStockCount", out);
        data.put("items", rows);
        return data;
    }

    private String generateStockOnHandHTML(Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");

        StringBuilder rows = new StringBuilder();
        if (items.isEmpty()) {
            rows.append(emptyRow(12, "📭 No items match the selected filters"));
        } else {
            for (Map<String, Object> r : items) {
                rows.append("""
                    <tr>
                      <td>%s</td>
                      <td><strong>%s</strong></td>
                      <td>%s</td>
                      <td style="text-align:right">%d</td>
                      <td>%s</td>
                      <td style="text-align:right">R %.2f</td>
                      <td style="text-align:right">R %.2f</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                    </tr>
                    """.formatted(
                        esc(r.get("sku")), esc(r.get("name")), esc(r.get("category")),
                        r.get("quantity"), esc(r.get("unitOfMeasure")),
                        r.get("unitCost"), r.get("lineValue"),
                        esc(r.get("locationName")), esc(r.get("statusLabel")),
                        esc(r.get("isConsumable")), esc(r.get("isDriverIssuable")),
                        esc(r.get("isVehicleIssuable"))));
            }
        }

        String stats = buildStatCard(String.valueOf(data.get("totalItems")), "Total Items", "#DBEAFE", "#1E40AF")
                + buildStatCard(String.valueOf(data.get("totalQuantity")), "Total Quantity", "#E0E7FF", "#3730A3")
                + buildStatCard(String.format("R %.2f", data.get("totalValue")), "Total Value", "#D1FAE5", "#065F46")
                + buildStatCard(String.valueOf(data.get("lowStockCount")), "Low Stock", "#FEF3C7", "#92400E")
                + buildStatCard(String.valueOf(data.get("outOfStockCount")), "Out of Stock", "#FEE2E2", "#991B1B");

        String filterLine = "Location: " + esc(data.get("locationName"))
                + " • Category: " + esc(data.get("categoryFilter"))
                + " • Status: " + esc(data.get("statusFilter"))
                + " • Issueable: " + esc(data.get("issueableTo"));

        String table = "<table><thead><tr>"
                + "<th>SKU</th><th>Name</th><th>Category</th><th style='text-align:right'>Qty</th><th>Unit</th>"
                + "<th style='text-align:right'>Unit Cost</th><th style='text-align:right'>Value</th>"
                + "<th>Location</th><th>Status</th><th>Consumable</th><th>Driver</th><th>Vehicle</th>"
                + "</tr></thead><tbody>" + rows + "</tbody></table>";

        return buildInventoryShell("Stock on Hand", "#3B82F6", "#DBEAFE", "#1E40AF",
                "📦", filterLine, stats, table, "");
    }

    // ============================================================
    // INVENTORY — LOW STOCK
    // ============================================================
    @Transactional(readOnly = true)
    public String generateLowStockReportHTML(Long locationId, String category) {
        try {
            return generateLowStockHTML(getLowStockReportData(locationId, category));
        } catch (Exception e) {
            log.error("Error generating low-stock report: {}", e.getMessage(), e);
            return generateErrorHTML("Low Stock", String.valueOf(locationId), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLowStockReportData(Long locationId, String category) {
        List<InventoryItem> all = inventoryItemRepository.findAll();
        Map<Long, String> locationNames = loadLocationNames();

        List<InventoryItem> lowStock = all.stream()
                .filter(i -> locationId == null || Objects.equals(i.getLocationId(), locationId))
                .filter(i -> category == null || category.isBlank()
                        || category.equalsIgnoreCase(i.getCategory()))
                .filter(i -> intOf(i.getQuantity()) <= intOf(i.getMinLevel()))
                .collect(Collectors.toList());

        double totalReorderValue = 0.0;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (InventoryItem i : lowStock) {
            int qty = intOf(i.getQuantity());
            int min = intOf(i.getMinLevel());
            int reorder = intOf(i.getReorderLevel());
            int suggested = Math.max(min - qty, Math.max(reorder, 1));
            double unit = doubleOf(i.getUnitCost());
            double reorderValue = suggested * unit;
            totalReorderValue += reorderValue;

            Map<String, Object> r = new HashMap<>();
            r.put("sku", safe(i.getSku()));
            r.put("name", safe(i.getName()));
            r.put("category", safe(i.getCategory()));
            r.put("unitOfMeasure", safe(i.getUnitOfMeasure()));
            r.put("quantity", qty);
            r.put("minLevel", min);
            r.put("reorderLevel", reorder);
            r.put("reorderQty", suggested);
            r.put("unitCost", unit);
            r.put("reorderValue", reorderValue);
            r.put("locationName", locationNames.getOrDefault(i.getLocationId(), "—"));
            r.put("statusLabel", qty <= 0 ? "Out of Stock" : "Low Stock");
            rows.add(r);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("locationName", locationId != null
                ? locationNames.getOrDefault(locationId, "Unknown") : "All Locations");
        data.put("categoryFilter", blankToDefault(category, "All Categories"));
        data.put("totalItems", lowStock.size());
        data.put("totalReorderValue", totalReorderValue);
        data.put("items", rows);
        return data;
    }

    private String generateLowStockHTML(Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");

        StringBuilder rows = new StringBuilder();
        if (items.isEmpty()) {
            rows.append(emptyRow(10, "✅ No items at or below minimum level"));
        } else {
            for (Map<String, Object> r : items) {
                String status = esc(r.get("statusLabel"));
                String statusBg = "Out of Stock".equals(r.get("statusLabel")) ? "#FEE2E2" : "#FEF3C7";
                String statusColor = "Out of Stock".equals(r.get("statusLabel")) ? "#991B1B" : "#92400E";
                rows.append("""
                    <tr>
                      <td>%s</td>
                      <td><strong>%s</strong></td>
                      <td>%s</td>
                      <td style="text-align:right">%d</td>
                      <td style="text-align:right">%d</td>
                      <td style="text-align:right;font-weight:600;color:#B45309">%d</td>
                      <td style="text-align:right">R %.2f</td>
                      <td style="text-align:right;font-weight:600">R %.2f</td>
                      <td>%s</td>
                      <td><span class="status-badge" style="background-color:%s;color:%s">%s</span></td>
                    </tr>
                    """.formatted(
                        esc(r.get("sku")), esc(r.get("name")), esc(r.get("category")),
                        r.get("quantity"), r.get("minLevel"), r.get("reorderQty"),
                        r.get("unitCost"), r.get("reorderValue"),
                        esc(r.get("locationName")),
                        statusBg, statusColor, status));
            }
        }

        String stats = buildStatCard(String.valueOf(data.get("totalItems")), "Items to Reorder", "#FEF3C7", "#92400E")
                + buildStatCard(String.format("R %.2f", data.get("totalReorderValue")), "Reorder Value", "#FEE2E2", "#991B1B")
                + buildStatCard(esc(data.get("locationName")), "Location", "#DBEAFE", "#1E40AF")
                + buildStatCard(esc(data.get("categoryFilter")), "Category", "#E0E7FF", "#3730A3");

        String filterLine = "Location: " + esc(data.get("locationName"))
                + " • Category: " + esc(data.get("categoryFilter"));

        String table = "<table><thead><tr>"
                + "<th>SKU</th><th>Name</th><th>Category</th>"
                + "<th style='text-align:right'>On Hand</th>"
                + "<th style='text-align:right'>Min Level</th>"
                + "<th style='text-align:right'>Suggested Qty</th>"
                + "<th style='text-align:right'>Unit Cost</th>"
                + "<th style='text-align:right'>Reorder Value</th>"
                + "<th>Location</th><th>Status</th>"
                + "</tr></thead><tbody>" + rows + "</tbody></table>";

        return buildInventoryShell("Low Stock / Reorder Report", "#F59E0B", "#FEF3C7", "#92400E",
                "⚠️", filterLine, stats, table, "");
    }

    // ============================================================
    // INVENTORY — MOVEMENTS
    // ============================================================
    @Transactional(readOnly = true)
    public String generateStockMovementsReportHTML(String startDate, String endDate,
                                                       String movementType, String approvalStatus,
                                                       Long itemId) {
        try {
            return generateStockMovementsHTML(getStockMovementsReportData(
                    startDate, endDate, movementType, approvalStatus, itemId));
        } catch (Exception e) {
            log.error("Error generating inventory movements report: {}", e.getMessage(), e);
            return generateErrorHTML("Inventory Movements", String.valueOf(itemId), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStockMovementsReportData(String startDate, String endDate,
                                                               String movementType, String approvalStatus,
                                                               Long itemId) {
        LocalDate from = parseDate(startDate);
        LocalDate to = parseDate(endDate);

        List<StockMovement> all = StockMovementRepository.findAll();
        List<StockMovement> filtered = all.stream()
                .filter(m -> from == null || (m.getCreatedAt() != null
                        && !m.getCreatedAt().toLocalDate().isBefore(from)))
                .filter(m -> to == null || (m.getCreatedAt() != null
                        && !m.getCreatedAt().toLocalDate().isAfter(to)))
                .filter(m -> movementType == null || movementType.isBlank()
                        || movementType.equalsIgnoreCase(m.getMovementType()))
                .filter(m -> approvalStatus == null || approvalStatus.isBlank()
                        || approvalStatus.equalsIgnoreCase(m.getApprovalStatus()))
                .filter(m -> itemId == null || Objects.equals(m.getItemId(), itemId))
                .sorted(Comparator.comparing(StockMovement::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        int totalIn = 0, totalOut = 0, totalAdj = 0;
        int pending = 0, approved = 0, rejected = 0;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (StockMovement m : filtered) {
            String type = m.getMovementType() != null ? m.getMovementType() : "";
            int qty = intOf(m.getQuantity());
            if ("IN".equalsIgnoreCase(type)) totalIn += qty;
            else if ("OUT".equalsIgnoreCase(type)) totalOut += qty;
            else if ("ADJUSTMENT".equalsIgnoreCase(type)) totalAdj += qty;

            String status = m.getApprovalStatus() != null ? m.getApprovalStatus() : "";
            if ("PENDING".equalsIgnoreCase(status)) pending++;
            else if ("APPROVED".equalsIgnoreCase(status)) approved++;
            else if ("REJECTED".equalsIgnoreCase(status)) rejected++;

            Map<String, Object> r = new HashMap<>();
            r.put("createdAt", m.getCreatedAt() != null
                    ? m.getCreatedAt().format(TIME_FORMATTER) : "—");
            r.put("itemName", safe(m.getItemName()));
            r.put("movementType", type);
            r.put("quantity", qty);
            r.put("reason", safe(m.getReason()));
            r.put("referenceNumber", safe(m.getReferenceNumber()));
            r.put("referenceType", safe(m.getReferenceType()));
            r.put("approvalStatus", status);
            r.put("performedBy", safe(m.getPerformedBy()));
            r.put("approvedBy", safe(m.getApprovedBy()));
            rows.add(r);
        }

        String itemName = itemId != null
                ? inventoryItemRepository.findById(itemId).map(InventoryItem::getName).orElse("Item #" + itemId)
                : "All Items";

        Map<String, Object> data = new HashMap<>();
        data.put("startDate", from != null ? from.format(DATE_FORMATTER) : "Beginning");
        data.put("endDate", to != null ? to.format(DATE_FORMATTER) : "Today");
        data.put("movementType", blankToDefault(movementType, "All Types"));
        data.put("approvalStatus", blankToDefault(approvalStatus, "All Statuses"));
        data.put("itemName", itemName);
        data.put("totalMovements", filtered.size());
        data.put("totalIn", totalIn);
        data.put("totalOut", totalOut);
        data.put("totalAdjustments", totalAdj);
        data.put("totalPending", pending);
        data.put("totalApproved", approved);
        data.put("totalRejected", rejected);
        data.put("movements", rows);
        return data;
    }

    private String generateStockMovementsHTML(Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> movements = (List<Map<String, Object>>) data.get("movements");

        StringBuilder rows = new StringBuilder();
        if (movements.isEmpty()) {
            rows.append(emptyRow(10, "📭 No movements match the selected filters"));
        } else {
            for (Map<String, Object> r : movements) {
                String type = (String) r.get("movementType");
                String typeBg = typeColorBg(type);
                String typeColor = typeColorText(type);
                String status = (String) r.get("approvalStatus");
                String statusBg = statusColorBg(status);
                String statusColor = statusColorText(status);
                rows.append("""
                    <tr>
                      <td>%s</td>
                      <td><strong>%s</strong></td>
                      <td><span class="status-badge" style="background-color:%s;color:%s">%s</span></td>
                      <td style="text-align:right;font-weight:600">%d</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td><span class="status-badge" style="background-color:%s;color:%s">%s</span></td>
                      <td>%s</td>
                      <td>%s</td>
                    </tr>
                    """.formatted(
                        esc(r.get("createdAt")), esc(r.get("itemName")),
                        typeBg, typeColor, esc(type),
                        r.get("quantity"),
                        esc(r.get("reason")), esc(r.get("referenceNumber")), esc(r.get("referenceType")),
                        statusBg, statusColor, esc(status),
                        esc(r.get("performedBy")), esc(r.get("approvedBy"))));
            }
        }

        String stats = buildStatCard(String.valueOf(data.get("totalMovements")), "Total Movements", "#EDE9FE", "#5B21B6")
                + buildStatCard(String.valueOf(data.get("totalIn")), "Stock In", "#D1FAE5", "#065F46")
                + buildStatCard(String.valueOf(data.get("totalOut")), "Stock Out", "#FEE2E2", "#991B1B")
                + buildStatCard(String.valueOf(data.get("totalPending")), "Pending", "#FEF3C7", "#92400E");

        String filterLine = "Period: " + esc(data.get("startDate")) + " → " + esc(data.get("endDate"))
                + " • Type: " + esc(data.get("movementType"))
                + " • Status: " + esc(data.get("approvalStatus"))
                + " • Item: " + esc(data.get("itemName"));

        String table = "<table><thead><tr>"
                + "<th>Date</th><th>Item</th><th>Type</th><th style='text-align:right'>Qty</th>"
                + "<th>Reason</th><th>Reference</th><th>Ref Type</th>"
                + "<th>Status</th><th>Performed By</th><th>Approved By</th>"
                + "</tr></thead><tbody>" + rows + "</tbody></table>";

        return buildInventoryShell("Inventory Movements", "#8B5CF6", "#EDE9FE", "#5B21B6",
                "🔄", filterLine, stats, table, "");
    }

    // ============================================================
    // INVENTORY — VALUATION
    // ============================================================
    @Transactional(readOnly = true)
    public String generateStockValuationReportHTML(String groupBy, Long locationId, String category) {
        try {
            return generateStockValuationHTML(getStockValuationReportData(groupBy, locationId, category));
        } catch (Exception e) {
            log.error("Error generating stock valuation report: {}", e.getMessage(), e);
            return generateErrorHTML("Stock Valuation", groupBy, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStockValuationReportData(String groupBy, Long locationId, String category) {
        List<InventoryItem> all = inventoryItemRepository.findAll();
        Map<Long, String> locationNames = loadLocationNames();

        List<InventoryItem> filtered = all.stream()
                .filter(i -> locationId == null || Objects.equals(i.getLocationId(), locationId))
                .filter(i -> category == null || category.isBlank()
                        || category.equalsIgnoreCase(i.getCategory()))
                .collect(Collectors.toList());

        String mode = (groupBy == null || groupBy.isBlank()) ? "category" : groupBy.toLowerCase();
        if (!mode.equals("location") && !mode.equals("category") && !mode.equals("none")) {
            mode = "category";
        }

        Map<String, List<InventoryItem>> grouped;
        switch (mode) {
            case "location" -> grouped = filtered.stream().collect(Collectors.groupingBy(
                    i -> locationNames.getOrDefault(i.getLocationId(), "Unassigned")));
            case "none" -> grouped = Map.of("All Items", filtered);
            default -> grouped = filtered.stream().collect(Collectors.groupingBy(
                    i -> safe(i.getCategory())));
        }

        double grandTotal = 0.0;
        int totalQty = 0;
        List<Map<String, Object>> groups = new ArrayList<>();
        for (Map.Entry<String, List<InventoryItem>> e : grouped.entrySet()) {
            double groupValue = 0.0;
            int groupQty = 0;
            for (InventoryItem i : e.getValue()) {
                int q = intOf(i.getQuantity());
                double v = q * doubleOf(i.getUnitCost());
                groupQty += q;
                groupValue += v;
            }
            grandTotal += groupValue;
            totalQty += groupQty;

            Map<String, Object> g = new HashMap<>();
            g.put("groupKey", e.getKey());
            g.put("groupLabel", e.getKey());
            g.put("itemCount", e.getValue().size());
            g.put("totalQuantity", groupQty);
            g.put("totalValue", groupValue);
            groups.add(g);
        }
        groups.sort((a, b) -> Double.compare(
                ((Number) b.get("totalValue")).doubleValue(),
                ((Number) a.get("totalValue")).doubleValue()));

        for (Map<String, Object> g : groups) {
            double v = ((Number) g.get("totalValue")).doubleValue();
            g.put("sharePercent", grandTotal > 0 ? (v / grandTotal) * 100.0 : 0.0);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("groupBy", "none".equals(mode) ? "None"
                : "location".equals(mode) ? "Location" : "Category");
        data.put("locationName", locationId != null
                ? locationNames.getOrDefault(locationId, "Unknown") : "All Locations");
        data.put("categoryFilter", blankToDefault(category, "All Categories"));
        data.put("grandTotal", grandTotal);
        data.put("totalItems", filtered.size());
        data.put("totalQuantity", totalQty);
        data.put("groups", groups);
        return data;
    }

    private String generateStockValuationHTML(Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> groups = (List<Map<String, Object>>) data.get("groups");

        StringBuilder rows = new StringBuilder();
        if (groups.isEmpty()) {
            rows.append(emptyRow(5, "📭 No items found for valuation"));
        } else {
            for (Map<String, Object> g : groups) {
                rows.append("""
                    <tr>
                      <td><strong>%s</strong></td>
                      <td style="text-align:right">%d</td>
                      <td style="text-align:right">%d</td>
                      <td style="text-align:right;font-weight:600">R %.2f</td>
                      <td style="text-align:right">%.1f%%</td>
                    </tr>
                    """.formatted(
                        esc(g.get("groupLabel")),
                        g.get("itemCount"),
                        g.get("totalQuantity"),
                        g.get("totalValue"),
                        g.get("sharePercent")));
            }
        }

        String stats = buildStatCard(String.format("R %.2f", data.get("grandTotal")), "Grand Total", "#CCFBF1", "#115E59")
                + buildStatCard(String.valueOf(data.get("totalItems")), "Total Items", "#DBEAFE", "#1E40AF")
                + buildStatCard(String.valueOf(data.get("totalQuantity")), "Total Quantity", "#E0E7FF", "#3730A3")
                + buildStatCard(esc(data.get("groupBy")), "Grouped By", "#F1F5F9", "#334155");

        String filterLine = "Location: " + esc(data.get("locationName"))
                + " • Category: " + esc(data.get("categoryFilter"))
                + " • Group By: " + esc(data.get("groupBy"));

        String groupCol = "Category".equals(data.get("groupBy")) ? "Category"
                : "Location".equals(data.get("groupBy")) ? "Location" : "Group";

        String table = "<table><thead><tr>"
                + "<th>" + groupCol + "</th>"
                + "<th style='text-align:right'>Item Count</th>"
                + "<th style='text-align:right'>Total Quantity</th>"
                + "<th style='text-align:right'>Total Value</th>"
                + "<th style='text-align:right'>Share</th>"
                + "</tr></thead><tbody>" + rows + "</tbody></table>";

        return buildInventoryShell("Stock Valuation", "#14B8A6", "#CCFBF1", "#115E59",
                "💰", filterLine, stats, table, "");
    }

    // ============================================================
    // INVENTORY — ISSUES (vehicle + driver)
    // ============================================================
    @Transactional(readOnly = true)
    public String generateInventoryIssuesReportHTML(String issueType, String status, Long vehicleId,
                                                    Long driverId, String startDate, String endDate) {
        try {
            return generateInventoryIssuesHTML(getInventoryIssuesReportData(
                    issueType, status, vehicleId, driverId, startDate, endDate));
        } catch (Exception e) {
            log.error("Error generating inventory issues report: {}", e.getMessage(), e);
            return generateErrorHTML("Inventory Issues", issueType, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getInventoryIssuesReportData(String issueType, String status,
                                                            Long vehicleId, Long driverId,
                                                            String startDate, String endDate) {
        LocalDate from = parseDate(startDate);
        LocalDate to = parseDate(endDate);
        String mode = (issueType == null || issueType.isBlank()) ? "both" : issueType.toLowerCase();

        List<Map<String, Object>> issueRows = new ArrayList<>();

        if (!"driver".equals(mode)) {
            for (VehicleIssue v : vehicleIssueRepository.findAll()) {
                if (vehicleId != null && !Objects.equals(v.getVehicleId(), vehicleId)) continue;
                if (!matchesDate(v.getIssueDate(), from, to)) continue;
                if (!matchesStatus(v.getStatus(), status)) continue;
                Map<String, Object> row = baseIssueRow(v.getId(), v.getIssueNumber(), "Vehicle",
                        v.getVehicleId(), null, v.getDriverId(), v.getStatus(),
                        v.getIssueDate(), v.getNotes(), v.getItems());
                issueRows.add(row);
            }
        }
        if (!"vehicle".equals(mode)) {
            for (DriverIssue d : driverIssueRepository.findAll()) {
                if (driverId != null && !Objects.equals(d.getDriverId(), driverId)) continue;
                if (!matchesDate(d.getIssueDate(), from, to)) continue;
                if (!matchesStatus(d.getStatus(), status)) continue;
                Map<String, Object> row = baseIssueRow(d.getId(), d.getIssueNumber(), "Driver",
                        null, null, d.getDriverId(), d.getStatus(),
                        d.getIssueDate(), d.getNotes(), d.getItems());
                issueRows.add(row);
            }
        }

        issueRows.sort((a, b) -> {
            Object da = a.get("issueDate");
            Object db = b.get("issueDate");
            if (da instanceof LocalDateTime && db instanceof LocalDateTime) {
                return ((LocalDateTime) db).compareTo((LocalDateTime) da);
            }
            return 0;
        });

        int open = 0, partial = 0, returned = 0;
        for (Map<String, Object> r : issueRows) {
            String s = (String) r.get("status");
            if ("RETURNED".equalsIgnoreCase(s)) returned++;
            else if ("PARTIALLY_RETURNED".equalsIgnoreCase(s)) partial++;
            else open++;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("issueType", blankToDefault(issueType, "All Types"));
        data.put("statusFilter", blankToDefault(status, "All Statuses"));
        data.put("vehicleRegistration", vehicleId != null
                ? vehicleRepository.findById(vehicleId)
                    .map(Vehicle::getRegistrationNumber).orElse("Vehicle #" + vehicleId)
                : "All Vehicles");
        data.put("driverName", driverId != null
                ? driverRepository.findById(driverId)
                    .map(this::driverLabel).orElse("Driver #" + driverId)
                : "All Drivers");
        data.put("startDate", from != null ? from.format(DATE_FORMATTER) : "Beginning");
        data.put("endDate", to != null ? to.format(DATE_FORMATTER) : "Today");
        data.put("totalIssues", issueRows.size());
        data.put("openIssues", open);
        data.put("partiallyReturnedIssues", partial);
        data.put("returnedIssues", returned);
        data.put("issues", issueRows);
        return data;
    }

    private Map<String, Object> baseIssueRow(Long id, String issueNumber, String type,
                                              Long vehicleId, Long driverIdPlaceholder,
                                              Long driverId, String status,
                                              LocalDateTime issueDate, String notes,
                                              List<?> items) {
        int totalIssued = 0, totalReturned = 0;
        String firstItemName = "—";
        if (items != null && !items.isEmpty()) {
            Object first = items.get(0);
            firstItemName = safe(invokeGetter(first, "getItemName"));
            for (Object it : items) {
                totalIssued += intOf(invokeGetter(it, "getQuantityIssued"));
                totalReturned += intOf(invokeGetter(it, "getQuantityReturned"));
            }
        }
        int outstanding = Math.max(0, totalIssued - totalReturned);

        Map<String, Object> row = new HashMap<>();
        row.put("issueNumber", safe(issueNumber));
        row.put("issueType", type);
        row.put("issueDate", issueDate);
        row.put("issueDateStr", issueDate != null
                ? issueDate.format(DATE_FORMATTER) : "—");
        row.put("vehicleRegistration", "Vehicle".equals(type) && vehicleId != null
                ? vehicleRepository.findById(vehicleId)
                    .map(Vehicle::getRegistrationNumber).orElse("Vehicle #" + vehicleId)
                : "—");
        row.put("driverName", driverId != null
                ? driverRepository.findById(driverId)
                    .map(this::driverLabel).orElse("Driver #" + driverId)
                : "—");
        row.put("itemName", firstItemName);
        row.put("quantityIssued", totalIssued);
        row.put("quantityReturned", totalReturned);
        row.put("quantityOutstanding", outstanding);
        row.put("status", status != null ? status : "ISSUED");
        row.put("notes", safe(notes));
        return row;
    }

    private String generateInventoryIssuesHTML(Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> issues = (List<Map<String, Object>>) data.get("issues");

        StringBuilder rows = new StringBuilder();
        if (issues.isEmpty()) {
            rows.append(emptyRow(9, "📭 No issues match the selected filters"));
        } else {
            for (Map<String, Object> r : issues) {
                String status = (String) r.get("status");
                String statusBg = statusColorBg(status);
                String statusColor = statusColorText(status);
                rows.append("""
                    <tr>
                      <td><strong>%s</strong></td>
                      <td><span class="status-badge" style="background-color:#E0E7FF;color:#3730A3">%s</span></td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td style="text-align:right">%d</td>
                      <td style="text-align:right">%d</td>
                      <td style="text-align:right;font-weight:600">%d</td>
                    </tr>
                    """.formatted(
                        esc(r.get("issueNumber")),
                        esc(r.get("issueType")),
                        esc(r.get("issueDateStr")),
                        esc(r.get("vehicleRegistration")),
                        esc(r.get("driverName")),
                        esc(r.get("itemName")),
                        r.get("quantityIssued"),
                        r.get("quantityReturned"),
                        r.get("quantityOutstanding")));
            }
        }

        String stats = buildStatCard(String.valueOf(data.get("totalIssues")), "Total Issues", "#FCE7F3", "#9D174D")
                + buildStatCard(String.valueOf(data.get("openIssues")), "Open", "#DBEAFE", "#1E40AF")
                + buildStatCard(String.valueOf(data.get("partiallyReturnedIssues")), "Partially Returned", "#FEF3C7", "#92400E")
                + buildStatCard(String.valueOf(data.get("returnedIssues")), "Returned", "#D1FAE5", "#065F46");

        String filterLine = "Type: " + esc(data.get("issueType"))
                + " • Status: " + esc(data.get("statusFilter"))
                + " • Period: " + esc(data.get("startDate")) + " → " + esc(data.get("endDate"));

        String table = "<table><thead><tr>"
                + "<th>Issue #</th><th>Type</th><th>Date</th>"
                + "<th>Vehicle</th><th>Driver</th><th>Item</th>"
                + "<th style='text-align:right'>Issued</th>"
                + "<th style='text-align:right'>Returned</th>"
                + "<th style='text-align:right'>Outstanding</th>"
                + "</tr></thead><tbody>" + rows + "</tbody></table>";

        return buildInventoryShell("Inventory Issues Register", "#EC4899", "#FCE7F3", "#9D174D",
                "🎫", filterLine, stats, table, "");
    }

    // ============================================================
    // INVENTORY — ITEM DETAIL
    // ============================================================
    @Transactional(readOnly = true)
    public String generateInventoryItemReportHTML(Long itemId) {
        try {
            return generateInventoryItemHTML(getInventoryItemReportData(itemId));
        } catch (Exception e) {
            log.error("Error generating inventory item report: {}", e.getMessage(), e);
            return generateErrorHTML("Inventory Item", String.valueOf(itemId), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getInventoryItemReportData(Long itemId) {
        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Inventory item not found: " + itemId));

        Map<Long, String> locationNames = loadLocationNames();

        // Movements for this item (last 50)
        List<StockMovement> movements = StockMovementRepository.findAll().stream()
                .filter(m -> Objects.equals(m.getItemId(), itemId))
                .sorted(Comparator.comparing(StockMovement::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(50)
                .toList();

        List<Map<String, Object>> movementRows = new ArrayList<>();
        for (StockMovement m : movements) {
            Map<String, Object> r = new HashMap<>();
            r.put("createdAt", m.getCreatedAt() != null ? m.getCreatedAt().format(TIME_FORMATTER) : "—");
            r.put("movementType", safe(m.getMovementType()));
            r.put("quantity", intOf(m.getQuantity()));
            r.put("reason", safe(m.getReason()));
            r.put("referenceNumber", safe(m.getReferenceNumber()));
            r.put("approvalStatus", safe(m.getApprovalStatus()));
            r.put("performedBy", safe(m.getPerformedBy()));
            movementRows.add(r);
        }

        // Issues referencing this item
        List<Map<String, Object>> issueRows = new ArrayList<>();
        int totalIssued = 0, totalReturned = 0;
        for (VehicleIssue v : vehicleIssueRepository.findAll()) {
            if (v.getItems() == null) continue;
            Map<String, Object> line = findItemLine(v.getItems(), itemId);
            if (line == null) continue;
            int issued = intOf(line.get("quantityIssued"));
            int returned = intOf(line.get("quantityReturned"));
            totalIssued += issued;
            totalReturned += returned;
            Map<String, Object> r = new HashMap<>();
            r.put("issueNumber", safe(v.getIssueNumber()));
            r.put("issueType", "Vehicle");
            r.put("issueDate", v.getIssueDate() != null ? v.getIssueDate().format(DATE_FORMATTER) : "—");
            r.put("vehicleRegistration", v.getVehicleId() != null
                    ? vehicleRepository.findById(v.getVehicleId())
                        .map(Vehicle::getRegistrationNumber).orElse("—") : "—");
            r.put("driverName", v.getDriverId() != null
                    ? driverRepository.findById(v.getDriverId()).map(this::driverLabel).orElse("—") : "—");
            r.put("quantityIssued", issued);
            r.put("quantityReturned", returned);
            r.put("quantityOutstanding", Math.max(0, issued - returned));
            r.put("status", safe(v.getStatus()));
            issueRows.add(r);
        }
        for (DriverIssue d : driverIssueRepository.findAll()) {
            if (d.getItems() == null) continue;
            Map<String, Object> line = findItemLine(d.getItems(), itemId);
            if (line == null) continue;
            int issued = intOf(line.get("quantityIssued"));
            int returned = intOf(line.get("quantityReturned"));
            totalIssued += issued;
            totalReturned += returned;
            Map<String, Object> r = new HashMap<>();
            r.put("issueNumber", safe(d.getIssueNumber()));
            r.put("issueType", "Driver");
            r.put("issueDate", d.getIssueDate() != null ? d.getIssueDate().format(DATE_FORMATTER) : "—");
            r.put("vehicleRegistration", "—");
            r.put("driverName", d.getDriverId() != null
                    ? driverRepository.findById(d.getDriverId()).map(this::driverLabel).orElse("—") : "—");
            r.put("quantityIssued", issued);
            r.put("quantityReturned", returned);
            r.put("quantityOutstanding", Math.max(0, issued - returned));
            r.put("status", safe(d.getStatus()));
            issueRows.add(r);
        }

        int qty = intOf(item.getQuantity());
        double unit = doubleOf(item.getUnitCost());

        Map<String, Object> data = new HashMap<>();
        data.put("sku", safe(item.getSku()));
        data.put("name", safe(item.getName()));
        data.put("category", safe(item.getCategory()));
        data.put("unitOfMeasure", safe(item.getUnitOfMeasure()));
        data.put("locationName", locationNames.getOrDefault(item.getLocationId(), "—"));
        data.put("quantity", qty);
        data.put("minLevel", intOf(item.getMinLevel()));
        data.put("reorderLevel", intOf(item.getReorderLevel()));
        data.put("unitCost", unit);
        data.put("currentValue", qty * unit);
        data.put("statusLabel", itemStatusLabel(item));
        data.put("isConsumable", Boolean.TRUE.equals(item.getIsConsumable()));
        data.put("isDriverIssuable", Boolean.TRUE.equals(item.getIsDriverIssuable()));
        data.put("isVehicleIssuable", Boolean.TRUE.equals(item.getIsVehicleIssuable()));
        data.put("isHeld", Boolean.TRUE.equals(item.getIsHeld()));
        data.put("isActive", Boolean.TRUE.equals(item.getIsActive()));
        data.put("notes", safe(item.getNotes()));
        data.put("totalMovements", movementRows.size());
        data.put("totalIssued", totalIssued);
        data.put("totalReturned", totalReturned);
        data.put("totalOutstanding", Math.max(0, totalIssued - totalReturned));
        data.put("movements", movementRows);
        data.put("issues", issueRows);
        return data;
    }

    private String generateInventoryItemHTML(Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> movements = (List<Map<String, Object>>) data.get("movements");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> issues = (List<Map<String, Object>>) data.get("issues");

        StringBuilder movementRows = new StringBuilder();
        if (movements.isEmpty()) {
            movementRows.append(emptyRow(7, "No movements recorded"));
        } else {
            for (Map<String, Object> m : movements) {
                movementRows.append("""
                    <tr>
                      <td>%s</td>
                      <td>%s</td>
                      <td style="text-align:right;font-weight:600">%d</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                    </tr>
                    """.formatted(
                        esc(m.get("createdAt")), esc(m.get("movementType")), m.get("quantity"),
                        esc(m.get("reason")), esc(m.get("referenceNumber")),
                        esc(m.get("approvalStatus")), esc(m.get("performedBy"))));
            }
        }

        StringBuilder issueRows = new StringBuilder();
        if (issues.isEmpty()) {
            issueRows.append(emptyRow(8, "No issues recorded"));
        } else {
            for (Map<String, Object> i : issues) {
                issueRows.append("""
                    <tr>
                      <td><strong>%s</strong></td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td style="text-align:right">%d</td>
                      <td style="text-align:right">%d</td>
                      <td style="text-align:right;font-weight:600">%d</td>
                    </tr>
                    """.formatted(
                        esc(i.get("issueNumber")), esc(i.get("issueType")), esc(i.get("issueDate")),
                        esc(i.get("vehicleRegistration")), esc(i.get("driverName")),
                        i.get("quantityIssued"), i.get("quantityReturned"), i.get("quantityOutstanding")));
            }
        }

        String detailsSection = """
            <div class="grid" style="margin-bottom:24px">
              <div class="section">
                <div class="section-title" style="color:#64748B;border-bottom-color:#E5E7EB">Identification</div>
                <div class="row"><span class="label">SKU</span><span class="value"><strong>%s</strong></span></div>
                <div class="row"><span class="label">Name</span><span class="value">%s</span></div>
                <div class="row"><span class="label">Category</span><span class="value">%s</span></div>
                <div class="row"><span class="label">Unit</span><span class="value">%s</span></div>
                <div class="row"><span class="label">Location</span><span class="value">%s</span></div>
              </div>
              <div class="section">
                <div class="section-title" style="color:#64748B;border-bottom-color:#E5E7EB">Stock Levels</div>
                <div class="row"><span class="label">Quantity</span><span class="value"><strong>%d</strong></span></div>
                <div class="row"><span class="label">Min Level</span><span class="value">%d</span></div>
                <div class="row"><span class="label">Reorder Level</span><span class="value">%d</span></div>
                <div class="row"><span class="label">Unit Cost</span><span class="value">R %.2f</span></div>
                <div class="row"><span class="label">Current Value</span><span class="value"><strong>R %.2f</strong></span></div>
              </div>
              <div class="section">
                <div class="section-title" style="color:#64748B;border-bottom-color:#E5E7EB">Flags</div>
                <div class="row"><span class="label">Status</span><span class="value">%s</span></div>
                <div class="row"><span class="label">Consumable</span><span class="value">%s</span></div>
                <div class="row"><span class="label">Driver Issuable</span><span class="value">%s</span></div>
                <div class="row"><span class="label">Vehicle Issuable</span><span class="value">%s</span></div>
                <div class="row"><span class="label">On Hold</span><span class="value">%s</span></div>
              </div>
              <div class="section">
                <div class="section-title" style="color:#64748B;border-bottom-color:#E5E7EB">Summary</div>
                <div class="row"><span class="label">Movements</span><span class="value">%d</span></div>
                <div class="row"><span class="label">Total Issued</span><span class="value">%d</span></div>
                <div class="row"><span class="label">Total Returned</span><span class="value">%d</span></div>
                <div class="row"><span class="label">Outstanding</span><span class="value"><strong>%d</strong></span></div>
                <div class="row"><span class="label">Notes</span><span class="value">%s</span></div>
              </div>
            </div>
            """.formatted(
                esc(data.get("sku")), esc(data.get("name")), esc(data.get("category")),
                esc(data.get("unitOfMeasure")), esc(data.get("locationName")),
                data.get("quantity"), data.get("minLevel"), data.get("reorderLevel"),
                data.get("unitCost"), data.get("currentValue"),
                esc(data.get("statusLabel")),
                yesNo(data.get("isConsumable")), yesNo(data.get("isDriverIssuable")),
                yesNo(data.get("isVehicleIssuable")), yesNo(data.get("isHeld")),
                data.get("totalMovements"), data.get("totalIssued"),
                data.get("totalReturned"), data.get("totalOutstanding"),
                esc(data.get("notes")));

        String body = detailsSection
                + "<div class='section-title' style='margin-top:24px;color:#64748B'>Recent Movements</div>"
                + "<table><thead><tr><th>Date</th><th>Type</th><th style='text-align:right'>Qty</th>"
                + "<th>Reason</th><th>Reference</th><th>Status</th><th>Performed By</th>"
                + "</tr></thead><tbody>" + movementRows + "</tbody></table>"
                + "<div class='section-title' style='margin-top:24px;color:#64748B'>Issue History</div>"
                + "<table><thead><tr><th>Issue #</th><th>Type</th><th>Date</th>"
                + "<th>Vehicle</th><th>Driver</th><th style='text-align:right'>Issued</th>"
                + "<th style='text-align:right'>Returned</th>"
                + "<th style='text-align:right'>Outstanding</th>"
                + "</tr></thead><tbody>" + issueRows + "</tbody></table>";

        return buildInventoryShell(
                "Inventory Item — " + esc(data.get("name")),
                "#64748B", "#F1F5F9", "#334155",
                "🏷️",
                "SKU: " + esc(data.get("sku")) + " • Location: " + esc(data.get("locationName")),
                "", "", body);
    }

    // ============================================================
    // INVENTORY HELPERS
    // ============================================================

    private Map<Long, String> loadLocationNames() {
        Map<Long, String> map = new HashMap<>();
        for (Location l : locationRepository.findAll()) {
            map.put(l.getId(), l.getName());
        }
        return map;
    }

    private boolean matchesStatusFilter(InventoryItem i, String status) {
        int qty = intOf(i.getQuantity());
        int min = intOf(i.getMinLevel());
        return switch (status) {
            case "In Stock" -> qty > min;
            case "Low Stock" -> qty > 0 && qty <= min;
            case "Out of Stock" -> qty <= 0;
            default -> true;
        };
    }

    private boolean matchesIssueableFilter(InventoryItem i, String filter) {
        if (filter == null || filter.isBlank() || "all".equalsIgnoreCase(filter)) return true;
        boolean isDriver = Boolean.TRUE.equals(i.getIsDriverIssuable());
        boolean isVehicle = Boolean.TRUE.equals(i.getIsVehicleIssuable());
        return switch (filter.toLowerCase()) {
            case "driver" -> isDriver && !isVehicle;
            case "vehicle" -> isVehicle && !isDriver;
            case "both" -> isDriver && isVehicle;
            case "neither" -> !isDriver && !isVehicle;
            default -> true;
        };
    }

    private boolean matchesStatus(String actual, String filter) {
        if (filter == null || filter.isBlank()) return true;
        return actual != null && actual.equalsIgnoreCase(filter);
    }

    private boolean matchesDate(LocalDateTime date, LocalDate from, LocalDate to) {
        if (from == null && to == null) return true;
        if (date == null) return false;
        LocalDate d = date.toLocalDate();
        if (from != null && d.isBefore(from)) return false;
        if (to != null && d.isAfter(to)) return false;
        return true;
    }

    private String itemStatusLabel(InventoryItem i) {
        int qty = intOf(i.getQuantity());
        int min = intOf(i.getMinLevel());
        if (qty <= 0) return "Out of Stock";
        if (qty <= min) return "Low Stock";
        return "In Stock";
    }

    private int intOf(Object v) {
        if (v instanceof Number n) return n.intValue();
        return 0;
    }

    private double doubleOf(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        return 0.0;
    }

    private String safe(Object v) {
        return v == null ? "" : v.toString();
    }

    private String blankToDefault(String v, String def) {
        return v == null || v.isBlank() ? def : v;
    }

    private String yesNo(Object v) {
        if (v instanceof Boolean b) return b ? "Yes" : "No";
        if (v instanceof String s) return Boolean.parseBoolean(s) ? "Yes" : "No";
        return "No";
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && needle != null
                && haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private String driverLabel(Driver d) {
        if (d == null) return "—";
        try {
            String full = d.getFullName();
            if (full != null && !full.isBlank()) return full;
        } catch (Exception ignored) { }
        String first = d.getFirstName() != null ? d.getFirstName() : "";
        String last = d.getLastName() != null ? d.getLastName() : "";
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? "Driver #" + d.getId() : combined;
    }

    private Object invokeGetter(Object target, String method) {
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> findItemLine(List<?> items, Long itemId) {
        for (Object it : items) {
            Object id = invokeGetter(it, "getItemId");
            if (id instanceof Number n && n.longValue() == itemId) {
                Map<String, Object> map = new HashMap<>();
                map.put("quantityIssued", invokeGetter(it, "getQuantityIssued"));
                map.put("quantityReturned", invokeGetter(it, "getQuantityReturned"));
                return map;
            }
        }
        return null;
    }

    private String typeColorBg(String type) {
        if (type == null) return "#F3F4F6";
        return switch (type.toUpperCase()) {
            case "IN" -> "#D1FAE5";
            case "OUT" -> "#FEE2E2";
            case "ADJUSTMENT" -> "#FEF3C7";
            default -> "#F3F4F6";
        };
    }

    private String typeColorText(String type) {
        if (type == null) return "#374151";
        return switch (type.toUpperCase()) {
            case "IN" -> "#065F46";
            case "OUT" -> "#991B1B";
            case "ADJUSTMENT" -> "#92400E";
            default -> "#374151";
        };
    }

    private String statusColorBg(String status) {
        if (status == null) return "#F3F4F6";
        return switch (status.toUpperCase()) {
            case "APPROVED", "RETURNED", "COMPLETED" -> "#D1FAE5";
            case "PENDING", "PARTIALLY_RETURNED" -> "#FEF3C7";
            case "REJECTED", "CANCELLED" -> "#FEE2E2";
            case "ISSUED" -> "#DBEAFE";
            default -> "#F3F4F6";
        };
    }

    private String statusColorText(String status) {
        if (status == null) return "#374151";
        return switch (status.toUpperCase()) {
            case "APPROVED", "RETURNED", "COMPLETED" -> "#065F46";
            case "PENDING", "PARTIALLY_RETURNED" -> "#92400E";
            case "REJECTED", "CANCELLED" -> "#991B1B";
            case "ISSUED" -> "#1E40AF";
            default -> "#374151";
        };
    }

    private String esc(Object v) {
        if (v == null) return "";
        return v.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String emptyRow(int colSpan, String message) {
        return "<tr><td colspan='" + colSpan
                + "' style='text-align:center;padding:30px;color:#6B7280;font-size:13px'>"
                + message + "</td></tr>";
    }

    private String buildStatCard(String value, String label, String bgColor, String textColor) {
        return "<div class='stat-card' style='background:" + bgColor + "'>"
                + "<div class='stat-value' style='color:" + textColor + "'>" + value + "</div>"
                + "<div class='stat-label' style='color:" + textColor + "'>" + label + "</div>"
                + "</div>";
    }

    private String buildInventoryShell(String title, String accentColor, String accentLightColor,
                                       String accentDarkColor, String emoji, String filterLine,
                                       String statCardsHtml, String tableHtml, String bodyHtml) {
        String statsBlock = statCardsHtml == null || statCardsHtml.isEmpty()
                ? ""
                : "<div class='stat-grid'>" + statCardsHtml + "</div>";
        String tablesBlock = tableHtml == null || tableHtml.isEmpty() ? "" : tableHtml;
        String extraBlock = bodyHtml == null || bodyHtml.isEmpty() ? "" : bodyHtml;

        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8">
          <title>%s</title>
          <style>
            * { margin:0; padding:0; box-sizing:border-box; }
            body { font-family:'Segoe UI', Arial, sans-serif; background:#F7F7FC; padding:20px; }
            .container { max-width:1300px; margin:0 auto; background:#FFF; border-radius:16px;
                         box-shadow:0 4px 24px rgba(0,0,0,0.08); padding:40px; }
            .header { text-align:center; border-bottom:3px double %s; padding-bottom:20px; margin-bottom:30px; }
            .logo-container { display:flex; align-items:center; justify-content:center; gap:14px; margin-bottom:4px; }
            .logo-img { height:50px; width:auto; }
            .logo-text { font-size:28px; font-weight:700; color:%s; }
            .logo-text span { color:%s; }
            .subtitle { font-size:14px; color:#6B7280; }
            .report-title { font-size:20px; font-weight:600; color:#111827; margin-top:8px; }
            .filter-line { font-size:12px; color:#6B7280; margin-top:4px; }
            .stat-grid { display:grid; grid-template-columns:repeat(5, 1fr); gap:14px; margin-bottom:24px; }
            .stat-card { text-align:center; padding:16px; border-radius:12px; border:1px solid rgba(0,0,0,0.04); }
            .stat-value { font-size:22px; font-weight:700; }
            .stat-label { font-size:11px; text-transform:uppercase; letter-spacing:0.3px; margin-top:4px; }
            .grid { display:grid; grid-template-columns:1fr 1fr; gap:20px; }
            .section { background:#F9FAFB; border-radius:12px; padding:16px 20px; }
            .section-title { font-size:13px; font-weight:600; color:%s; text-transform:uppercase;
                             letter-spacing:0.5px; margin-bottom:12px; padding-bottom:6px; border-bottom:2px solid #E5E7EB; }
            .row { display:flex; padding:6px 0; border-bottom:1px solid #F3F4F6; }
            .row:last-child { border-bottom:none; }
            .label { width:140px; font-size:12px; color:#6B7280; flex-shrink:0; }
            .value { font-size:13px; font-weight:500; color:#111827; }
            .status-badge { display:inline-block; padding:2px 10px; border-radius:20px;
                            font-size:11px; font-weight:600; }
            table { width:100%%; border-collapse:collapse; font-size:11px; margin-top:12px; }
            th { background:#F3F4F6; padding:10px 10px; text-align:left; font-weight:600;
                 color:#374151; border-bottom:2px solid #E5E7EB; }
            td { padding:8px 10px; border-bottom:1px solid #F3F4F6; }
            .footer { text-align:center; margin-top:30px; padding-top:20px;
                      border-top:1px solid #E5E7EB; font-size:11px; color:#9CA3AF; }
            .print-btn { display:inline-block; padding:10px 24px;
                         background:linear-gradient(135deg, %s 0%%, %s 100%%);
                         color:white; border:none; border-radius:10px;
                         font-size:14px; font-weight:600; cursor:pointer; margin-top:20px; }
            @media print {
              body { background:white; padding:0; }
              .container { box-shadow:none; padding:20px; }
              .print-btn { display:none !important; }
            }
            @media (max-width:900px) {
              .stat-grid { grid-template-columns:1fr 1fr; }
              .grid { grid-template-columns:1fr; }
              .container { padding:20px; }
            }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <div class="logo-container">
                <img src="%s" alt="SNL Trailers" class="logo-img" onerror="this.style.display='none'">
                <div class="logo-text">%s <span>SNL</span> Trailers</div>
              </div>
              <div class="subtitle">Logistics &amp; Trucking Operations</div>
              <div class="report-title">%s</div>
              <div class="filter-line">%s • Generated: %s</div>
            </div>
            %s
            %s
            %s
            <div style="text-align:center" class="no-print">
              <button class="print-btn" onclick="window.print()">🖨️ Print / Save as PDF</button>
            </div>
            <div class="footer">
              <strong>SNL Trailers</strong> • Generated: %s • Confidential
            </div>
          </div>
        </body>
        </html>
        """.formatted(
                esc(title),
                accentColor, accentColor, accentLightColor,
                accentColor,
                accentColor, accentLightColor,
                LOGO_URL, emoji,
                esc(title),
                esc(filterLine), LocalDateTime.now().format(TIME_FORMATTER),
                statsBlock,
                tablesBlock,
                extraBlock,
                LocalDateTime.now().format(TIME_FORMATTER));
    }

    // ============================================================
    // ERROR HTML (unchanged)
    // ============================================================
    private String generateErrorHTML(String entity, String identifier, String error) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8">
          <title>Error - %s</title>
          <style>
            body { font-family:'Segoe UI', Arial, sans-serif; background:#FEF2F2; padding:40px; }
            .container { max-width:600px; margin:40px auto; background:white; border-radius:12px;
                         box-shadow:0 4px 12px rgba(0,0,0,0.1); padding:30px; border-left:4px solid #DC2626; }
            h2 { color:#DC2626; font-size:20px; margin-bottom:8px; }
            .entity { font-size:14px; color:#6B7280; margin-bottom:4px; }
            .identifier { font-size:13px; color:#374151; font-weight:500; margin-bottom:16px; }
            .error-detail { background:#F9FAFB; padding:12px 16px; border-radius:6px;
                            font-family:monospace; font-size:13px; color:#374151; margin:12px 0;
                            border:1px solid #E5E7EB; word-break:break-all; }
            .suggestion { color:#6B7280; font-size:13px; }
            .button { display:inline-block; margin-top:16px; padding:8px 20px; background:#4F46E5;
                      color:white; border:none; border-radius:6px; font-size:14px; cursor:pointer; }
          </style>
        </head>
        <body>
          <div class="container">
            <h2>⚠️ Report Generation Error</h2>
            <div class="entity">Entity: %s</div>
            <div class="identifier">Identifier: %s</div>
            <div class="error-detail">%s</div>
            <p class="suggestion">Please try again or contact support.</p>
            <button class="button" onclick="window.history.back()">← Go Back</button>
          </div>
        </body>
        </html>
        """.formatted(entity, entity, identifier, error);
    }

    // ============================================================
    // EXISTING STATUS HELPERS (unchanged)
    // ============================================================
    private String getStatusColor(String status) {
        if (status == null) return "#F3F4F6";
        return switch (status.toUpperCase()) {
            case "COMPLETED", "FINALIZED" -> "#D1FAE5";
            case "IN_PROGRESS", "ACTIVE" -> "#DBEAFE";
            case "PLANNED", "ASSIGNED" -> "#FEF3C7";
            case "CANCELLED" -> "#FEE2E2";
            default -> "#F3F4F6";
        };
    }

    private String getStatusTextColor(String status) {
        if (status == null) return "#6B7280";
        return switch (status.toUpperCase()) {
            case "COMPLETED", "FINALIZED" -> "#065F46";
            case "IN_PROGRESS", "ACTIVE" -> "#1E40AF";
            case "PLANNED", "ASSIGNED" -> "#92400E";
            case "CANCELLED" -> "#991B1B";
            default -> "#6B7280";
        };
    }
}
