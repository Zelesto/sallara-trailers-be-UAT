// src/main/java/com/pgsa/trailers/service/JasperReportService.java

package com.pgsa.trailers.service;

import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class JasperReportService {

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private static final String LOGO_URL =
        "https://trailers-backend.onrender.com/logo.png";

    // ============================================================
    // EXISTING REPORTS — trips / loads / fuel
    // ============================================================

    public byte[] generateTripReportPDF(Map<String, Object> data) {
        try {
            log.info("📄 Generating PDF trip report");
            return generatePDF("trip_report.jrxml", buildTripParams(data));
        } catch (JRException | IOException e) {
            log.error("❌ Error generating PDF trip report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF trip report", e);
        }
    }

    public byte[] generateLoadReportPDF(Map<String, Object> data) {
        try {
            log.info("📄 Generating PDF load report");
            return generatePDF("load_report.jrxml", buildLoadParams(data));
        } catch (JRException | IOException e) {
            log.error("❌ Error generating PDF load report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF load report", e);
        }
    }

    public byte[] generateFuelReportPDF(Map<String, Object> data) {
        try {
            log.info("📄 Generating PDF fuel report");
            return generatePDF("fuel_report.jrxml", buildFuelParams(data));
        } catch (JRException | IOException e) {
            log.error("❌ Error generating PDF fuel report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF fuel report", e);
        }
    }

    // ============================================================
    // INVENTORY REPORTS (NEW)
    // ============================================================

    public byte[] generateStockOnHandReportPDF(Map<String, Object> data) {
        try {
            log.info("📄 Generating PDF stock-on-hand report");
            return generatePDF("stock_on_hand.jrxml", buildStockOnHandParams(data));
        } catch (JRException | IOException e) {
            log.error("❌ Error generating PDF stock-on-hand report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF stock-on-hand report", e);
        }
    }

    public byte[] generateLowStockReportPDF(Map<String, Object> data) {
        try {
            log.info("📄 Generating PDF low-stock report");
            return generatePDF("low_stock.jrxml", buildLowStockParams(data));
        } catch (JRException | IOException e) {
            log.error("❌ Error generating PDF low-stock report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF low-stock report", e);
        }
    }

    public byte[] generateStockMovementsReportPDF(Map<String, Object> data) {
        try {
            log.info("📄 Generating PDF inventory movements report");
            return generatePDF("inventory_movements.jrxml", buildStockMovementsParams(data));
        } catch (JRException | IOException e) {
            log.error("❌ Error generating PDF inventory movements report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF inventory movements report", e);
        }
    }

    public byte[] generateStockValuationReportPDF(Map<String, Object> data) {
        try {
            log.info("📄 Generating PDF stock valuation report");
            return generatePDF("stock_valuation.jrxml", buildStockValuationParams(data));
        } catch (JRException | IOException e) {
            log.error("❌ Error generating PDF stock valuation report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF stock valuation report", e);
        }
    }

    public byte[] generateInventoryIssuesReportPDF(Map<String, Object> data) {
        try {
            log.info("📄 Generating PDF inventory issues report");
            return generatePDF("inventory_issues.jrxml", buildInventoryIssuesParams(data));
        } catch (JRException | IOException e) {
            log.error("❌ Error generating PDF inventory issues report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF inventory issues report", e);
        }
    }

    public byte[] generateInventoryItemReportPDF(Map<String, Object> data) {
        try {
            log.info("📄 Generating PDF inventory item report");
            return generatePDF("inventory_item.jrxml", buildInventoryItemParams(data));
        } catch (JRException | IOException e) {
            log.error("❌ Error generating PDF inventory item report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF inventory item report", e);
        }
    }

    // ============================================================
    // GENERIC PDF GENERATION (unchanged)
    // ============================================================

    private byte[] generatePDF(String templateName, Map<String, Object> params)
            throws JRException, IOException {

        InputStream templateStream = new ClassPathResource("reports/" + templateName).getInputStream();

        JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);

        JasperPrint jasperPrint = JasperFillManager.fillReport(
            jasperReport,
            params,
            new JREmptyDataSource()
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));

        SimplePdfExporterConfiguration config = new SimplePdfExporterConfiguration();
        config.setPdfJavaScript("this.print();");
        exporter.setConfiguration(config);
        exporter.exportReport();

        log.info("✅ PDF generated: {} bytes", outputStream.size());
        return outputStream.toByteArray();
    }

    // ============================================================
    // PARAMETER BUILDERS — trips / loads / fuel (unchanged)
    // ============================================================

    private Map<String, Object> buildTripParams(Map<String, Object> data) {
        Map<String, Object> params = new HashMap<>();
        params.put("companyName", "Trailers");
        params.put("reportTitle", "Trip Report");
        params.put("logoUrl", LOGO_URL);
        params.put("tripNumber", getString(data, "tripNumber"));
        params.put("customerName", getString(data, "customerName"));
        params.put("customerCode", getString(data, "customerCode"));
        params.put("status", getString(data, "status"));
        params.put("tripType", getString(data, "tripType"));
        params.put("referenceNumber", getString(data, "referenceNumber"));
        params.put("originLocation", getString(data, "originLocation"));
        params.put("destinationLocation", getString(data, "destinationLocation"));
        params.put("plannedDistanceKm", getDouble(data, "plannedDistanceKm"));
        params.put("actualDistanceKm", getDouble(data, "actualDistanceKm"));
        params.put("driverName", getString(data, "driverName"));
        params.put("driverLicense", getString(data, "driverLicense"));
        params.put("vehicleRegistration", getString(data, "vehicleRegistration"));
        params.put("vehicleMake", getString(data, "vehicleMake"));
        params.put("vehicleModel", getString(data, "vehicleModel"));
        params.put("plannedStartDate", getString(data, "plannedStartDate"));
        params.put("plannedEndDate", getString(data, "plannedEndDate"));
        params.put("actualStartDate", getString(data, "actualStartDate"));
        params.put("actualEndDate", getString(data, "actualEndDate"));
        params.put("actualStartOdometer", getDouble(data, "actualStartOdometer"));
        params.put("actualEndOdometer", getDouble(data, "actualEndOdometer"));
        params.put("reportGenerated", LocalDateTime.now().format(DATE_FORMATTER));
        return params;
    }

    private Map<String, Object> buildLoadParams(Map<String, Object> data) {
        Map<String, Object> params = new HashMap<>();
        params.put("companyName", "Trailers");
        params.put("reportTitle", "Load Report");
        params.put("logoUrl", LOGO_URL);
        params.put("loadNumber", getString(data, "loadNumber"));
        params.put("status", getString(data, "status"));
        params.put("description", getString(data, "description"));
        params.put("commodityType", getString(data, "commodityType"));
        params.put("weightKg", getDouble(data, "weightKg"));
        params.put("palletCount", getInt(data, "palletCount"));
        params.put("tripCount", getInt(data, "tripCount"));
        params.put("customerName", getString(data, "customerName"));
        params.put("totalDistanceKm", getDouble(data, "totalDistanceKm"));
        params.put("totalFromDepotKm", getDouble(data, "totalFromDepotKm"));
        params.put("totalToDepotKm", getDouble(data, "totalToDepotKm"));
        params.put("trips", data.get("trips"));
        params.put("reportGenerated", LocalDateTime.now().format(DATE_FORMATTER));
        return params;
    }

    private Map<String, Object> buildFuelParams(Map<String, Object> data) {
        Map<String, Object> params = new HashMap<>();
        params.put("companyName", "Trailers");
        params.put("reportTitle", "Fuel Consumption Report");
        params.put("logoUrl", LOGO_URL);
        params.put("vehicleRegistration", getString(data, "vehicleRegistration"));
        params.put("startDate", getString(data, "startDate"));
        params.put("endDate", getString(data, "endDate"));
        params.put("totalLiters", getDouble(data, "totalLiters"));
        params.put("totalCost", getDouble(data, "totalCost"));
        params.put("avgUnitPrice", getDouble(data, "avgUnitPrice"));
        params.put("entryCount", getInt(data, "entryCount"));
        params.put("entries", data.get("entries"));
        params.put("reportGenerated", LocalDateTime.now().format(DATE_FORMATTER));
        return params;
    }

    // ============================================================
    // PARAMETER BUILDERS — INVENTORY (NEW)
    // ============================================================

    private Map<String, Object> buildStockOnHandParams(Map<String, Object> data) {
        Map<String, Object> params = new HashMap<>();
        params.put("companyName", "Trailers");
        params.put("reportTitle", "Stock on Hand");
        params.put("logoUrl", LOGO_URL);

        // Filters shown in the header
        params.put("locationName", getStringOr(data, "locationName", "All Locations"));
        params.put("categoryFilter", getStringOr(data, "categoryFilter", "All Categories"));
        params.put("statusFilter", getStringOr(data, "statusFilter", "All Statuses"));
        params.put("searchTerm", getStringOr(data, "searchTerm", ""));
        params.put("issueableTo", getStringOr(data, "issueableTo", "All"));
        params.put("includeHeld", getYesNo(data, "includeHeld"));

        // Summary band
        params.put("totalItems", getInt(data, "totalItems"));
        params.put("totalQuantity", getInt(data, "totalQuantity"));
        params.put("totalValue", getDouble(data, "totalValue"));
        params.put("lowStockCount", getInt(data, "lowStockCount"));
        params.put("outOfStockCount", getInt(data, "outOfStockCount"));

        // Detail band
        params.put("items", getList(data, "items"));
        params.put("reportGenerated", LocalDateTime.now().format(DATE_FORMATTER));
        return params;
    }

    private Map<String, Object> buildLowStockParams(Map<String, Object> data) {
        Map<String, Object> params = new HashMap<>();
        params.put("companyName", "Trailers");
        params.put("reportTitle", "Low Stock / Reorder Report");
        params.put("logoUrl", LOGO_URL);

        params.put("locationName", getStringOr(data, "locationName", "All Locations"));
        params.put("categoryFilter", getStringOr(data, "categoryFilter", "All Categories"));

        params.put("totalItems", getInt(data, "totalItems"));
        params.put("totalReorderValue", getDouble(data, "totalReorderValue"));

        params.put("items", getList(data, "items"));
        params.put("reportGenerated", LocalDateTime.now().format(DATE_FORMATTER));
        return params;
    }

    private Map<String, Object> buildStockMovementsParams(Map<String, Object> data) {
        Map<String, Object> params = new HashMap<>();
        params.put("companyName", "Trailers");
        params.put("reportTitle", "Inventory Movements");
        params.put("logoUrl", LOGO_URL);

        params.put("startDate", getStringOr(data, "startDate", "Beginning"));
        params.put("endDate", getStringOr(data, "endDate", "Today"));
        params.put("movementType", getStringOr(data, "movementType", "All Types"));
        params.put("approvalStatus", getStringOr(data, "approvalStatus", "All Statuses"));
        params.put("itemName", getStringOr(data, "itemName", "All Items"));

        params.put("totalMovements", getInt(data, "totalMovements"));
        params.put("totalIn", getInt(data, "totalIn"));
        params.put("totalOut", getInt(data, "totalOut"));
        params.put("totalAdjustments", getInt(data, "totalAdjustments"));
        params.put("totalPending", getInt(data, "totalPending"));
        params.put("totalApproved", getInt(data, "totalApproved"));
        params.put("totalRejected", getInt(data, "totalRejected"));

        params.put("movements", getList(data, "movements"));
        params.put("reportGenerated", LocalDateTime.now().format(DATE_FORMATTER));
        return params;
    }

    private Map<String, Object> buildStockValuationParams(Map<String, Object> data) {
        Map<String, Object> params = new HashMap<>();
        params.put("companyName", "Trailers");
        params.put("reportTitle", "Stock Valuation");
        params.put("logoUrl", LOGO_URL);

        params.put("groupBy", getStringOr(data, "groupBy", "Category"));
        params.put("locationName", getStringOr(data, "locationName", "All Locations"));
        params.put("categoryFilter", getStringOr(data, "categoryFilter", "All Categories"));

        params.put("grandTotal", getDouble(data, "grandTotal"));
        params.put("totalItems", getInt(data, "totalItems"));
        params.put("totalQuantity", getInt(data, "totalQuantity"));

        params.put("groups", getList(data, "groups"));
        params.put("reportGenerated", LocalDateTime.now().format(DATE_FORMATTER));
        return params;
    }

    private Map<String, Object> buildInventoryIssuesParams(Map<String, Object> data) {
        Map<String, Object> params = new HashMap<>();
        params.put("companyName", "Trailers");
        params.put("reportTitle", "Inventory Issues Register");
        params.put("logoUrl", LOGO_URL);

        params.put("issueType", getStringOr(data, "issueType", "All Types"));
        params.put("statusFilter", getStringOr(data, "statusFilter", "All Statuses"));
        params.put("vehicleRegistration", getStringOr(data, "vehicleRegistration", "All Vehicles"));
        params.put("driverName", getStringOr(data, "driverName", "All Drivers"));
        params.put("startDate", getStringOr(data, "startDate", "Beginning"));
        params.put("endDate", getStringOr(data, "endDate", "Today"));

        params.put("totalIssues", getInt(data, "totalIssues"));
        params.put("openIssues", getInt(data, "openIssues"));
        params.put("partiallyReturnedIssues", getInt(data, "partiallyReturnedIssues"));
        params.put("returnedIssues", getInt(data, "returnedIssues"));

        params.put("issues", getList(data, "issues"));
        params.put("reportGenerated", LocalDateTime.now().format(DATE_FORMATTER));
        return params;
    }

    private Map<String, Object> buildInventoryItemParams(Map<String, Object> data) {
        Map<String, Object> params = new HashMap<>();
        params.put("companyName", "Trailers");
        params.put("reportTitle", "Inventory Item Detail");
        params.put("logoUrl", LOGO_URL);

        // Identity
        params.put("sku", getString(data, "sku"));
        params.put("name", getString(data, "name"));
        params.put("category", getString(data, "category"));
        params.put("unitOfMeasure", getString(data, "unitOfMeasure"));
        params.put("locationName", getString(data, "locationName"));

        // Stock levels
        params.put("quantity", getInt(data, "quantity"));
        params.put("minLevel", getInt(data, "minLevel"));
        params.put("reorderLevel", getInt(data, "reorderLevel"));
        params.put("unitCost", getDouble(data, "unitCost"));
        params.put("currentValue", getDouble(data, "currentValue"));
        params.put("statusLabel", getString(data, "statusLabel"));

        // Flags (rendered as Yes / No)
        params.put("isConsumable", getYesNo(data, "isConsumable"));
        params.put("isDriverIssuable", getYesNo(data, "isDriverIssuable"));
        params.put("isVehicleIssuable", getYesNo(data, "isVehicleIssuable"));
        params.put("isHeld", getYesNo(data, "isHeld"));
        params.put("isActive", getYesNo(data, "isActive"));

        // Counters
        params.put("totalMovements", getInt(data, "totalMovements"));
        params.put("totalIssued", getInt(data, "totalIssued"));
        params.put("totalReturned", getInt(data, "totalReturned"));
        params.put("totalOutstanding", getInt(data, "totalOutstanding"));

        // Notes + collections
        params.put("notes", getStringOr(data, "notes", ""));
        params.put("movements", getList(data, "movements"));
        params.put("issues", getList(data, "issues"));

        params.put("reportGenerated", LocalDateTime.now().format(DATE_FORMATTER));
        return params;
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private String getString(Map<String, Object> data, String key) {
        return getStringOr(data, key, "N/A");
    }

    private String getStringOr(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private Double getDouble(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private Integer getInt(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    /** Formats a Boolean / "true" / "false" as "Yes" / "No" for PDF display. */
    private String getYesNo(Map<String, Object> data, String key) {
        Object value = data.get(key);
        boolean flag;
        if (value instanceof Boolean) {
            flag = (Boolean) value;
        } else if (value instanceof String) {
            flag = Boolean.parseBoolean((String) value);
        } else {
            flag = false;
        }
        return flag ? "Yes" : "No";
    }

    /** Returns a List or an empty list — safe for Jasper detail bands that iterate. */
    private List<?> getList(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value instanceof List ? (List<?>) value : Collections.emptyList();
    }
}
