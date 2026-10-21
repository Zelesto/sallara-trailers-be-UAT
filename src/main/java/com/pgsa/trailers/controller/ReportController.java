// src/main/java/com/pgsa/trailers/controller/ReportController.java

package com.pgsa.trailers.controller;

import com.pgsa.trailers.service.JasperReportService;
import com.pgsa.trailers.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pgsa.trailers.service.PivotReportService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;
    private final JasperReportService jasperReportService;
    private final PivotReportService pivotReportService;

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    // ============================================================
    // HTML REPORTS — TRIPS / LOADS / FUEL
    // ============================================================

    @PostMapping(
        value = "/trip/{tripNumber}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> generateTripReportHTML(
            @PathVariable String tripNumber,
            @RequestParam(defaultValue = "html") String format) {

        log.info("📊 Generating HTML trip report for: {}", tripNumber);

        try {
            String rawHtml = reportService.generateTripReportHTML(tripNumber);
            return buildSuccessResponse(rawHtml);

        } catch (Exception e) {
            log.error("❌ Error generating trip report: {}", e.getMessage(), e);
            return buildErrorResponse(e.getMessage());
        }
    }

    @PostMapping(
        value = "/load/{loadNumber}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> generateLoadReportHTML(
            @PathVariable String loadNumber,
            @RequestParam(defaultValue = "html") String format) {

        log.info("📊 Generating HTML load report for: {}", loadNumber);

        try {
            String rawHtml = reportService.generateLoadReportHTML(loadNumber);
            return buildSuccessResponse(rawHtml);

        } catch (Exception e) {
            log.error("❌ Error generating load report: {}", e.getMessage(), e);
            return buildErrorResponse(e.getMessage());
        }
    }

    @PostMapping(
        value = "/fuel",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> generateFuelReportHTML(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "html") String format) {

        log.info("📊 Generating HTML fuel report for vehicle: {}", vehicleId);

        try {
            String rawHtml = reportService.generateFuelReportHTML(vehicleId, startDate, endDate);
            return buildSuccessResponse(rawHtml);

        } catch (Exception e) {
            log.error("❌ Error generating fuel report: {}", e.getMessage(), e);
            return buildErrorResponse(e.getMessage());
        }
    }

    // ============================================================
    // INVENTORY — HTML REPORTS
    // ============================================================

    @PostMapping(
        value = "/inventory/stock-on-hand",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> generateStockOnHandReportHTML(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String issueableTo,
            @RequestParam(required = false) Boolean includeHeld,
            @RequestParam(defaultValue = "html") String format) {

        log.info("📊 Generating stock-on-hand report — location={}, category={}, status={}",
                locationId, category, status);

        try {
            String rawHtml = reportService.generateStockOnHandReportHTML(
                    locationId, category, status, search, issueableTo, includeHeld);
            return buildSuccessResponse(rawHtml);

        } catch (Exception e) {
            log.error("❌ Error generating stock-on-hand report: {}", e.getMessage(), e);
            return buildErrorResponse(e.getMessage());
        }
    }

    @PostMapping(
        value = "/inventory/low-stock",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> generateLowStockReportHTML(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "html") String format) {

        log.info("📊 Generating low-stock report — location={}, category={}", locationId, category);

        try {
            String rawHtml = reportService.generateLowStockReportHTML(locationId, category);
            return buildSuccessResponse(rawHtml);

        } catch (Exception e) {
            log.error("❌ Error generating low-stock report: {}", e.getMessage(), e);
            return buildErrorResponse(e.getMessage());
        }
    }

    @PostMapping(
        value = "/inventory/movements",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> generateStockMovementsReportHTML(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String movementType,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) Long itemId,
            @RequestParam(defaultValue = "html") String format) {

        log.info("📊 Generating inventory movements report — {} → {}, item={}",
                startDate, endDate, itemId);

        try {
            String rawHtml = reportService.generateStockMovementsReportHTML(
                    startDate, endDate, movementType, approvalStatus, itemId);
            return buildSuccessResponse(rawHtml);

        } catch (Exception e) {
            log.error("❌ Error generating inventory movements report: {}", e.getMessage(), e);
            return buildErrorResponse(e.getMessage());
        }
    }

    @PostMapping(
        value = "/inventory/valuation",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> generateStockValuationReportHTML(
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "html") String format) {

        log.info("📊 Generating stock valuation report — groupBy={}", groupBy);

        try {
            String rawHtml = reportService.generateStockValuationReportHTML(groupBy, locationId, category);
            return buildSuccessResponse(rawHtml);

        } catch (Exception e) {
            log.error("❌ Error generating stock valuation report: {}", e.getMessage(), e);
            return buildErrorResponse(e.getMessage());
        }
    }

    @PostMapping(
        value = "/inventory/issues",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> generateInventoryIssuesReportHTML(
            @RequestParam(required = false) String issueType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "html") String format) {

        log.info("📊 Generating inventory issues report — type={}, status={}", issueType, status);

        try {
            String rawHtml = reportService.generateInventoryIssuesReportHTML(
                    issueType, status, vehicleId, driverId, startDate, endDate);
            return buildSuccessResponse(rawHtml);

        } catch (Exception e) {
            log.error("❌ Error generating inventory issues report: {}", e.getMessage(), e);
            return buildErrorResponse(e.getMessage());
        }
    }

    @PostMapping(
        value = "/inventory/item/{itemId}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> generateInventoryItemReportHTML(
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "html") String format) {

        log.info("📊 Generating inventory item report for item: {}", itemId);

        try {
            String rawHtml = reportService.generateInventoryItemReportHTML(itemId);
            return buildSuccessResponse(rawHtml);

        } catch (Exception e) {
            log.error("❌ Error generating inventory item report: {}", e.getMessage(), e);
            return buildErrorResponse(e.getMessage());
        }
    }


    @PostMapping(
    value = "/analytics/pivot",
    produces = MediaType.APPLICATION_JSON_VALUE
)
public ResponseEntity<Map<String, Object>> generatePivotReportHTML(
        @RequestParam(required = false) String entity,
        @RequestParam(required = false) String groupBy,
        @RequestParam(required = false) String pivotBy,
        @RequestParam(required = false) String metric,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {

    log.info("📊 Generating pivot report — {} by {} × {} — {}",
            entity, groupBy, pivotBy, metric);

    try {
        Map<String, Object> data = pivotReportService.buildPivotData(
                entity, groupBy, pivotBy, metric, startDate, endDate);
        String html = pivotReportService.buildPivotHtml(data);
        return buildSuccessResponse(html);
    } catch (Exception e) {
        log.error("❌ Error generating pivot report: {}", e.getMessage(), e);
        return buildErrorResponse(e.getMessage());
    }
}
    // ============================================================
    // PDF REPORTS — TRIPS / LOADS / FUEL
    // ============================================================

    @PostMapping(
        value = "/trip/{tripNumber}/pdf",
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> generateTripReportPDF(
            @PathVariable String tripNumber) {

        log.info("📄 Generating PDF trip report for: {}", tripNumber);

        try {
            Map<String, Object> data = reportService.getTripReportData(tripNumber);
            byte[] pdf = jasperReportService.generateTripReportPDF(data);
            return buildPdfResponse(pdf, "trip-report-" + tripNumber + ".pdf");

        } catch (Exception e) {
            log.error("❌ Error generating PDF report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(
        value = "/load/{loadNumber}/pdf",
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> generateLoadReportPDF(
            @PathVariable String loadNumber) {

        log.info("📄 Generating PDF load report for: {}", loadNumber);

        try {
            Map<String, Object> data = reportService.getLoadReportData(loadNumber);
            byte[] pdf = jasperReportService.generateLoadReportPDF(data);
            return buildPdfResponse(pdf, "load-report-" + loadNumber + ".pdf");

        } catch (Exception e) {
            log.error("❌ Error generating PDF report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(
        value = "/fuel/pdf",
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> generateFuelReportPDF(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        log.info("📄 Generating PDF fuel report for vehicle: {}", vehicleId);

        try {
            Map<String, Object> data = reportService.getFuelReportData(vehicleId, startDate, endDate);
            byte[] pdf = jasperReportService.generateFuelReportPDF(data);
            return buildPdfResponse(pdf,
                    "fuel-report-" + (vehicleId != null ? vehicleId : "all") + ".pdf");

        } catch (Exception e) {
            log.error("❌ Error generating PDF report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ============================================================
    // INVENTORY — PDF REPORTS
    // ============================================================

    @PostMapping(
        value = "/inventory/stock-on-hand/pdf",
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> generateStockOnHandReportPDF(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String issueableTo,
            @RequestParam(required = false) Boolean includeHeld) {

        log.info("📄 Generating stock-on-hand PDF — location={}, category={}",
                locationId, category);

        try {
            Map<String, Object> data = reportService.getStockOnHandReportData(
                    locationId, category, status, search, issueableTo, includeHeld);
            byte[] pdf = jasperReportService.generateStockOnHandReportPDF(data);

            String filename = "stock-on-hand"
                    + (locationId != null ? "-loc-" + locationId : "")
                    + ".pdf";
            return buildPdfResponse(pdf, filename);

        } catch (Exception e) {
            log.error("❌ Error generating stock-on-hand PDF: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(
        value = "/inventory/low-stock/pdf",
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> generateLowStockReportPDF(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String category) {

        log.info("📄 Generating low-stock PDF — location={}, category={}", locationId, category);

        try {
            Map<String, Object> data = reportService.getLowStockReportData(locationId, category);
            byte[] pdf = jasperReportService.generateLowStockReportPDF(data);

            String filename = "low-stock"
                    + (locationId != null ? "-loc-" + locationId : "")
                    + ".pdf";
            return buildPdfResponse(pdf, filename);

        } catch (Exception e) {
            log.error("❌ Error generating low-stock PDF: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(
        value = "/inventory/movements/pdf",
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> generateStockMovementsReportPDF(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String movementType,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) Long itemId) {

        log.info("📄 Generating inventory movements PDF — {} → {}, item={}",
                startDate, endDate, itemId);

        try {
            Map<String, Object> data = reportService.getStockMovementsReportData(
                    startDate, endDate, movementType, approvalStatus, itemId);
            byte[] pdf = jasperReportService.generateStockMovementsReportPDF(data);
            return buildPdfResponse(pdf, "inventory-movements.pdf");

        } catch (Exception e) {
            log.error("❌ Error generating inventory movements PDF: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(
        value = "/inventory/valuation/pdf",
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> generateStockValuationReportPDF(
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String category) {

        log.info("📄 Generating stock valuation PDF — groupBy={}", groupBy);

        try {
            Map<String, Object> data = reportService.getStockValuationReportData(
                    groupBy, locationId, category);
            byte[] pdf = jasperReportService.generateStockValuationReportPDF(data);

            String filename = "stock-valuation"
                    + (groupBy != null && !groupBy.isBlank() ? "-by-" + groupBy : "")
                    + ".pdf";
            return buildPdfResponse(pdf, filename);

        } catch (Exception e) {
            log.error("❌ Error generating stock valuation PDF: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(
        value = "/inventory/issues/pdf",
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> generateInventoryIssuesReportPDF(
            @RequestParam(required = false) String issueType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        log.info("📄 Generating inventory issues PDF — type={}, status={}", issueType, status);

        try {
            Map<String, Object> data = reportService.getInventoryIssuesReportData(
                    issueType, status, vehicleId, driverId, startDate, endDate);
            byte[] pdf = jasperReportService.generateInventoryIssuesReportPDF(data);

            String filename = "inventory-issues"
                    + (issueType != null && !issueType.isBlank() && !"both".equals(issueType)
                        ? "-" + issueType
                        : "")
                    + ".pdf";
            return buildPdfResponse(pdf, filename);

        } catch (Exception e) {
            log.error("❌ Error generating inventory issues PDF: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(
        value = "/inventory/item/{itemId}/pdf",
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> generateInventoryItemReportPDF(
            @PathVariable Long itemId) {

        log.info("📄 Generating inventory item PDF for item: {}", itemId);

        try {
            Map<String, Object> data = reportService.getInventoryItemReportData(itemId);
            byte[] pdf = jasperReportService.generateInventoryItemReportPDF(data);
            return buildPdfResponse(pdf, "inventory-item-" + itemId + ".pdf");

        } catch (Exception e) {
            log.error("❌ Error generating inventory item PDF: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @PostMapping(
    value = "/analytics/pivot/pdf",
    produces = MediaType.APPLICATION_PDF_VALUE
)
public ResponseEntity<byte[]> generatePivotReportPDF(
        @RequestParam(required = false) String entity,
        @RequestParam(required = false) String groupBy,
        @RequestParam(required = false) String pivotBy,
        @RequestParam(required = false) String metric,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {

    log.info("📄 Generating PDF pivot report — {} by {} × {}", entity, groupBy, pivotBy);

    try {
        Map<String, Object> data = pivotReportService.buildPivotData(
                entity, groupBy, pivotBy, metric, startDate, endDate);
        byte[] pdf = jasperReportService.generatePivotReportPDF(data);
        return buildPdfResponse(pdf, "pivot-report.pdf");
    } catch (Exception e) {
        log.error("❌ Error generating PDF pivot report: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError().build();
    }
}
    // ============================================================
    // HELPER METHODS
    // ============================================================

    private ResponseEntity<Map<String, Object>> buildSuccessResponse(String rawHtml) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("format", "html");
        response.put("content", rawHtml);
        response.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", message);
        errorResponse.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    private ResponseEntity<byte[]> buildPdfResponse(byte[] pdf, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("birtAvailable", false);
        response.put("reportsAvailable", true);
        response.put("pdfReportsAvailable", true);
        response.put("inventoryReportsAvailable", true);
        response.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));
        return ResponseEntity.ok(response);
    }
}
