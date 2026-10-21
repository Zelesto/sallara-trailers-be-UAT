// src/main/java/com/pgsa/trailers/service/PivotReportService.java

package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.entity.ops.FuelSlip;
import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.FuelSlipRepository;
import com.pgsa.trailers.repository.LoadRepository;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PivotReportService {

    private final TripRepository tripRepository;
    private final LoadRepository loadRepository;
    private final FuelSlipRepository fuelSlipRepository;
    private final VehicleRepository vehicleRepository;

    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FMT  = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMM yyyy");
    private static final String LOGO_URL = "https://trailers-backend.onrender.com/logo.png";

    private static final Set<String> ENTITIES = Set.of("TRIPS", "LOADS", "FUEL");
    private static final Set<String> GROUPERS = Set.of(
        "VEHICLE", "DRIVER", "CUSTOMER", "MONTH", "YEAR", "STATUS",
        "COMMODITY", "LOCATION", "TRIP_TYPE", "TRIP", "LOAD"
    );
    private static final Set<String> METRICS = Set.of(
        "COUNT", "DISTANCE", "COST", "LITERS", "WEIGHT", "DURATION"
    );

    // ============================================================
    // PUBLIC — BUILD PIVOT DATA MAP
    // ============================================================
    @Transactional(readOnly = true)
    public Map<String, Object> buildPivotData(
            String entity, String groupBy, String pivotBy, String metric,
            String startDate, String endDate) {

        String ent = normalize(entity, "TRIPS");
        String g   = normalize(groupBy, defaultGroupFor(ent));
        String p   = (pivotBy == null || pivotBy.isBlank() || pivotBy.equalsIgnoreCase(g))
                     ? null : pivotBy.toUpperCase();
        String m   = normalize(metric, defaultMetricFor(ent));

        if (!ENTITIES.contains(ent)) ent = "TRIPS";
        if (!GROUPERS.contains(g))   g = defaultGroupFor(ent);
        if (p != null && !GROUPERS.contains(p)) p = null;
        if (!METRICS.contains(m))    m = defaultMetricFor(ent);

        LocalDate from = parseDate(startDate);
        LocalDate to   = parseDate(endDate);

        List<Map<String, Object>> records = fetchRecords(ent, from, to);

        // Aggregate
        Map<String, Map<String, Double>> matrix = new LinkedHashMap<>();
        Map<String, Double> rowTotals = new LinkedHashMap<>();
        Map<String, Double> colTotals = new LinkedHashMap<>();
        double grandTotal = 0.0;

        for (Map<String, Object> rec : records) {
            String gKey = groupKey(rec, g);
            String pKey = p != null ? groupKey(rec, p) : "Value";
            if (gKey == null || gKey.isBlank()) gKey = "(Unspecified)";
            if (pKey == null || pKey.isBlank()) pKey = "(Unspecified)";

            double value = metricValue(rec, m);

            matrix.computeIfAbsent(gKey, k -> new LinkedHashMap<>())
                  .merge(pKey, value, Double::sum);
            rowTotals.merge(gKey, value, Double::sum);
            colTotals.merge(pKey, value, Double::sum);
            grandTotal += value;
        }

        // Column keys
        List<String> pivotKeys = new ArrayList<>(colTotals.keySet());
        if (p != null) {
            if ("MONTH".equals(p) || "YEAR".equals(p)) {
                pivotKeys.sort(Comparator.comparing(this::sortableDateKey));
            } else {
                pivotKeys.sort(String.CASE_INSENSITIVE_ORDER);
            }
        } else {
            pivotKeys = List.of("Value");
        }

        // Row ordering
        List<String> rowKeys = new ArrayList<>(matrix.keySet());
        if ("MONTH".equals(g) || "YEAR".equals(g)) {
            rowKeys.sort(Comparator.comparing(this::sortableDateKey));
        } else if ("VEHICLE".equals(g) || "DRIVER".equals(g)) {
            rowKeys.sort(Comparator
                .comparing((String k) -> -rowTotals.getOrDefault(k, 0.0))
                .thenComparing(String.CASE_INSENSITIVE_ORDER));
        } else {
            rowKeys.sort(String.CASE_INSENSITIVE_ORDER);
        }

        // Build rows
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String rk : rowKeys) {
            Map<String, Double> cells = matrix.get(rk);
            List<Map<String, Object>> values = new ArrayList<>();
            for (String ck : pivotKeys) {
                double v = cells.getOrDefault(ck, 0.0);
                values.add(Map.of(
                    "columnKey", ck,
                    "value", v,
                    "formatted", formatValue(v, m)
                ));
            }
            rows.add(Map.of(
                "groupKey", rk,
                "groupLabel", rk,
                "values", values,
                "total", rowTotals.getOrDefault(rk, 0.0),
                "formattedTotal", formatValue(rowTotals.getOrDefault(rk, 0.0), m)
            ));
        }

        List<Map<String, Object>> colTotalRow = new ArrayList<>();
        for (String ck : pivotKeys) {
            double v = colTotals.getOrDefault(ck, 0.0);
            colTotalRow.add(Map.of(
                "columnKey", ck,
                "value", v,
                "formatted", formatValue(v, m)
            ));
        }

        String subtitle = (from != null || to != null)
            ? (from != null ? from.format(DATE_FMT) : "Beginning")
              + " → "
              + (to != null ? to.format(DATE_FMT) : "Today")
            : "All Time";

        Map<String, Object> data = new HashMap<>();
        data.put("entity", ent);
        data.put("groupBy", g);
        data.put("pivotBy", p);
        data.put("metric", m);
        data.put("title", titleFor(ent, g, p, m));
        data.put("subtitle", subtitle);
        data.put("groupLabel", labelFor(g));
        data.put("pivotLabel", p != null ? labelFor(p) : null);
        data.put("metricLabel", metricLabel(m));
        data.put("hasPivot", p != null);
        data.put("columnHeaders", pivotKeys);
        data.put("rows", rows);
        data.put("columnTotals", colTotalRow);
        data.put("grandTotal", grandTotal);
        data.put("formattedGrandTotal", formatValue(grandTotal, m));
        data.put("rowCount", rows.size());
        data.put("recordCount", records.size());
        data.put("generated", LocalDateTime.now().format(TIME_FMT));
        data.put("logoUrl", LOGO_URL);

        log.info("📊 Pivot: {} by {} × {} — {} rows, {} records",
                 ent, g, (p == null ? "—" : p), rows.size(), records.size());
        return data;
    }

    // ============================================================
    // HTML BUILDER (matrix)
    // ============================================================
    public String buildPivotHtml(Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<String> cols = (List<String>) data.get("columnHeaders");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("rows");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> colTotals = (List<Map<String, Object>>) data.get("columnTotals");

        boolean hasPivot = Boolean.TRUE.equals(data.get("hasPivot"));
        String groupLabel = esc(data.get("groupLabel"));
        String pivotLabel = data.get("pivotLabel") == null ? "" : esc(data.get("pivotLabel"));
        String metricLabel = esc(data.get("metricLabel"));

        // Header
        StringBuilder thead = new StringBuilder("<tr>");
        thead.append("<th style='text-align:left;min-width:180px'>").append(groupLabel).append("</th>");
        if (hasPivot) {
            for (String c : cols) {
                thead.append("<th style='text-align:right'>").append(esc(c)).append("</th>");
            }
        } else {
            thead.append("<th style='text-align:right'>").append(metricLabel).append("</th>");
        }
        thead.append("<th style='text-align:right;font-weight:700;background:#EEF2FF;color:#3730A3'>Total</th>");
        thead.append("</tr>");

        // Body
        StringBuilder tbody = new StringBuilder();
        if (rows.isEmpty()) {
            tbody.append("<tr><td colspan='")
                 .append(cols.size() + 2)
                 .append("' style='text-align:center;padding:30px;color:#6B7280'>📭 No data for the selected filters</td></tr>");
        } else {
            for (Map<String, Object> row : rows) {
                tbody.append("<tr>");
                tbody.append("<td><strong>").append(esc(row.get("groupLabel"))).append("</strong></td>");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> values = (List<Map<String, Object>>) row.get("values");
                for (Map<String, Object> v : values) {
                    tbody.append("<td style='text-align:right'>").append(esc(v.get("formatted"))).append("</td>");
                }
                tbody.append("<td style='text-align:right;font-weight:600;background:#F9FAFB'>")
                     .append(esc(row.get("formattedTotal")))
                     .append("</td>");
                tbody.append("</tr>");
            }
        }

        // Column totals row
        StringBuilder tfoot = new StringBuilder("<tr style='background:#EEF2FF'>");
        tfoot.append("<td style='font-weight:700;color:#3730A3'>Grand Total</td>");
        if (hasPivot) {
            for (Map<String, Object> ct : colTotals) {
                tfoot.append("<td style='text-align:right;font-weight:700;color:#3730A3'>")
                     .append(esc(ct.get("formatted"))).append("</td>");
            }
        } else {
            tfoot.append("<td style='text-align:right;font-weight:700;color:#3730A3'>")
                 .append(esc(data.get("formattedGrandTotal"))).append("</td>");
        }
        tfoot.append("<td style='text-align:right;font-weight:700;color:#3730A3'>")
             .append(esc(data.get("formattedGrandTotal"))).append("</td>");
        tfoot.append("</tr>");

        String subtitleLine = esc(data.get("subtitle"))
            + " • " + data.get("recordCount") + " records • Generated: " + esc(data.get("generated"));

        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8">
          <title>%s</title>
          <style>
            * { margin:0; padding:0; box-sizing:border-box; }
            body { font-family:'Segoe UI', Arial, sans-serif; background:#F7F7FC; padding:20px; }
            .container { max-width:1400px; margin:0 auto; background:#FFF; border-radius:16px;
                         box-shadow:0 4px 24px rgba(0,0,0,0.08); padding:40px; }
            .header { text-align:center; border-bottom:3px double #4F46E5; padding-bottom:20px; margin-bottom:24px; }
            .logo-container { display:flex; align-items:center; justify-content:center; gap:14px; margin-bottom:4px; }
            .logo-img { height:50px; width:auto; }
            .logo-text { font-size:26px; font-weight:700; color:#4F46E5; }
            .logo-text span { color:#6366F1; }
            .subtitle { font-size:13px; color:#6B7280; }
            .report-title { font-size:20px; font-weight:600; color:#111827; margin-top:8px; }
            .filter-line { font-size:12px; color:#6B7280; margin-top:6px; }
            table { width:100%%; border-collapse:collapse; font-size:12px; margin-top:16px; }
            th { background:#F3F4F6; padding:10px 12px; text-align:left; font-weight:600;
                 color:#374151; border-bottom:2px solid #E5E7EB; white-space:nowrap; }
            td { padding:8px 12px; border-bottom:1px solid #F3F4F6; }
            tr:hover td { background:#FAFAFA; }
            tfoot td { border-top:2px solid #C7D2FE; }
            .footer { text-align:center; margin-top:30px; padding-top:20px;
                      border-top:1px solid #E5E7EB; font-size:11px; color:#9CA3AF; }
            .print-btn { display:inline-block; padding:10px 24px;
                         background:linear-gradient(135deg, #4F46E5 0%%, #6366F1 100%%);
                         color:white; border:none; border-radius:10px;
                         font-size:14px; font-weight:600; cursor:pointer; margin-top:20px; }
            @media print {
              body { background:white; padding:0; }
              .container { box-shadow:none; padding:20px; }
              .print-btn { display:none !important; }
            }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <div class="logo-container">
                <img src="%s" alt="SNL Trailers" class="logo-img" onerror="this.style.display='none'">
                <div class="logo-text">📊 <span>SNL</span> Trailers</div>
              </div>
              <div class="subtitle">Logistics &amp; Trucking Operations</div>
              <div class="report-title">%s</div>
              <div class="filter-line">%s</div>
            </div>
            <table>
              <thead>%s</thead>
              <tbody>%s</tbody>
              <tfoot>%s</tfoot>
            </table>
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
            esc(data.get("title")),
            esc(data.get("logoUrl")),
            esc(data.get("title")),
            subtitleLine,
            thead.toString(),
            tbody.toString(),
            tfoot.toString(),
            esc(data.get("generated"))
        );
    }

    // ============================================================
    // FETCHERS
    // ============================================================
    private List<Map<String, Object>> fetchRecords(String entity, LocalDate from, LocalDate to) {
        switch (entity) {
            case "LOADS": return fetchLoads(from, to);
            case "FUEL":  return fetchFuel(from, to);
            default:      return fetchTrips(from, to);
        }
    }

    private List<Map<String, Object>> fetchTrips(LocalDate from, LocalDate to) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Trip t : tripRepository.findAll()) {
            LocalDate d = extractTripDate(t);
            if (!inRange(d, from, to)) continue;
            Map<String, Object> r = new HashMap<>();
            r.put("_date", d);
            r.put("vehicle", tripVehicleLabel(t));
            r.put("driver", tripDriverLabel(t));
            r.put("customer", safe(getStr(t, "getCustomerName")));
            r.put("status", safe(getStr(t, "getStatus")));
            r.put("tripType", safe(getStr(t, "getTripType")));
            r.put("commodity", safe(getStr(t, "getCommodityType")));
            r.put("location", safe(getStr(t, "getOriginLocation")));
            r.put("tripRef", safe(getStr(t, "getTripNumber")));
            r.put("loadRef", safe(getStr(t, "getLoadNumber")));
            r.put("distance", getDbl(t, "getActualDistanceKm", "getPlannedDistanceKm"));
            r.put("cost", 0.0);
            r.put("weight", getDbl(t, "getWeightKg"));
            r.put("liters", 0.0);
            r.put("duration", durationHours(t));
            r.put("count", 1.0);
            out.add(r);
        }
        return out;
    }

    private List<Map<String, Object>> fetchLoads(LocalDate from, LocalDate to) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Load l : loadRepository.findAll()) {
            LocalDate d = extractLoadDate(l);
            if (!inRange(d, from, to)) continue;
            Map<String, Object> r = new HashMap<>();
            r.put("_date", d);
            r.put("vehicle", safe(getStr(l, "getVehicleRegistration")));
            r.put("driver", safe(getStr(l, "getDriverName")));
            r.put("customer", safe(getStr(l, "getCustomerName")));
            r.put("status", safe(getStr(l, "getStatus")));
            r.put("tripType", "LOAD");
            r.put("commodity", safe(getStr(l, "getCommodityType")));
            r.put("location", "—");
            r.put("tripRef", "—");
            r.put("loadRef", safe(getStr(l, "getLoadNumber")));
            r.put("distance", getDbl(l, "getTotalDistanceKm"));
            r.put("cost", 0.0);
            r.put("weight", getDbl(l, "getWeightKg"));
            r.put("liters", 0.0);
            r.put("duration", 0.0);
            r.put("count", 1.0);
            out.add(r);
        }
        return out;
    }

    private List<Map<String, Object>> fetchFuel(LocalDate from, LocalDate to) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (FuelSlip f : fuelSlipRepository.findAll()) {
            LocalDate d = f.getTransactionDate() != null
                ? f.getTransactionDate().toLocalDate() : null;
            if (!inRange(d, from, to)) continue;
            Map<String, Object> r = new HashMap<>();
            r.put("_date", d);
            r.put("vehicle", fuelVehicleLabel(f));
            r.put("driver", safe(getStr(f, "getDriverName")));
            r.put("customer", "—");
            r.put("status", safe(getStr(f, "getStatus")));
            r.put("tripType", "FUEL");
            r.put("commodity", "Fuel");
            r.put("location", fuelSourceLabel(f));
            r.put("tripRef", fuelTripRef(f));
            r.put("loadRef", "—");
            r.put("distance", 0.0);
            r.put("cost", getDbl(f, "getTotalAmount"));
            r.put("weight", 0.0);
            r.put("liters", getDbl(f, "getQuantity"));
            r.put("duration", 0.0);
            r.put("count", 1.0);
            out.add(r);
        }
        return out;
    }

    // ============================================================
    // EXTRACTION
    // ============================================================
    private LocalDate extractTripDate(Trip t) {
        for (String name : new String[]{"getActualStartDate", "getPlannedStartDate"}) {
            Object v = getObj(t, name);
            if (v instanceof LocalDateTime) return ((LocalDateTime) v).toLocalDate();
            if (v instanceof LocalDate)     return (LocalDate) v;
        }
        return null;
    }

    private LocalDate extractLoadDate(Load l) {
        for (String name : new String[]{"getLoadDate", "getCreatedAt"}) {
            Object v = getObj(l, name);
            if (v instanceof LocalDateTime) return ((LocalDateTime) v).toLocalDate();
            if (v instanceof LocalDate)     return (LocalDate) v;
        }
        return null;
    }

    private double durationHours(Trip t) {
        Object start = getObj(t, "getActualStartDate");
        Object end   = getObj(t, "getActualEndDate");
        if (start instanceof LocalDateTime && end instanceof LocalDateTime) {
            long minutes = Duration.between((LocalDateTime) start, (LocalDateTime) end).toMinutes();
            return Math.max(0, minutes) / 60.0;
        }
        return 0.0;
    }

    private String tripVehicleLabel(Trip t) {
        String reg = safe(getStr(t, "getVehicleRegistration"));
        if (!"—".equals(reg)) return reg;
        Object vid = getObj(t, "getVehicleId");
        if (vid instanceof Number n) {
            return vehicleRepository.findById(n.longValue())
                .map(Vehicle::getRegistrationNumber)
                .orElse("Vehicle #" + n);
        }
        return "Unassigned";
    }

    private String tripDriverLabel(Trip t) {
        String name = safe(getStr(t, "getDriverName"));
        if (!"—".equals(name)) return name;
        return "Unassigned";
    }

    private String fuelVehicleLabel(FuelSlip f) {
        Object v = getObj(f, "getVehicle");
        if (v instanceof Vehicle veh) return veh.getRegistrationNumber();
        Object vid = getObj(f, "getVehicleId");
        if (vid instanceof Number n) {
            return vehicleRepository.findById(n.longValue())
                .map(Vehicle::getRegistrationNumber)
                .orElse("Vehicle #" + n);
        }
        return "Unassigned";
    }

    private String fuelSourceLabel(FuelSlip f) {
        Object src = getObj(f, "getFuelSource");
        if (src != null) {
            Object n = getObj(src, "getName");
            if (n != null) return n.toString();
        }
        return "—";
    }

    private String fuelTripRef(FuelSlip f) {
        for (String name : new String[]{"getTripNumber", "getTripId"}) {
            Object v = getObj(f, name);
            if (v != null) return v.toString();
        }
        return "—";
    }

    // ============================================================
    // GROUP KEY / METRIC
    // ============================================================
    private String groupKey(Map<String, Object> rec, String grouper) {
        return switch (grouper) {
            case "VEHICLE"   -> safe((String) rec.get("vehicle"));
            case "DRIVER"    -> safe((String) rec.get("driver"));
            case "CUSTOMER"  -> safe((String) rec.get("customer"));
            case "STATUS"    -> safe((String) rec.get("status"));
            case "COMMODITY" -> safe((String) rec.get("commodity"));
            case "LOCATION"  -> safe((String) rec.get("location"));
            case "TRIP_TYPE" -> safe((String) rec.get("tripType"));
            case "TRIP"      -> safe((String) rec.get("tripRef"));
            case "LOAD"      -> safe((String) rec.get("loadRef"));
            case "MONTH" -> {
                Object d = rec.get("_date");
                yield d instanceof LocalDate ld ? ld.format(MONTH_FMT) : "(Unknown Month)";
            }
            case "YEAR" -> {
                Object d = rec.get("_date");
                yield d instanceof LocalDate ld ? String.valueOf(ld.getYear()) : "(Unknown Year)";
            }
            default -> "(Unknown)";
        };
    }

    private double metricValue(Map<String, Object> rec, String metric) {
        Object v = switch (metric) {
            case "COUNT"    -> rec.get("count");
            case "DISTANCE" -> rec.get("distance");
            case "COST"     -> rec.get("cost");
            case "LITERS"   -> rec.get("liters");
            case "WEIGHT"   -> rec.get("weight");
            case "DURATION" -> rec.get("duration");
            default -> null;
        };
        return v instanceof Number n ? n.doubleValue() : 0.0;
    }

    // ============================================================
    // FORMAT / LABEL / UTILS
    // ============================================================
    private String normalize(String v, String def) {
        if (v == null || v.isBlank()) return def;
        return v.trim().toUpperCase();
    }

    private String defaultGroupFor(String e) {
        return switch (e) {
            case "LOADS" -> "CUSTOMER";
            case "FUEL"  -> "VEHICLE";
            default      -> "VEHICLE";
        };
    }

    private String defaultMetricFor(String e) {
        return switch (e) {
            case "FUEL"  -> "LITERS";
            default      -> "COUNT";
        };
    }

    private String titleFor(String entity, String g, String p, String m) {
        String e = switch (entity) {
            case "LOADS" -> "Loads";
            case "FUEL"  -> "Fuel Purchases";
            default      -> "Trips";
        };
        if (p != null) {
            return e + " by " + labelFor(g) + " × " + labelFor(p) + " — " + metricLabel(m);
        }
        return e + " by " + labelFor(g) + " — " + metricLabel(m);
    }

    private String labelFor(String k) {
        if (k == null) return "";
        return switch (k) {
            case "VEHICLE"   -> "Vehicle";
            case "DRIVER"    -> "Driver";
            case "CUSTOMER"  -> "Customer";
            case "MONTH"     -> "Month";
            case "YEAR"      -> "Year";
            case "STATUS"    -> "Status";
            case "COMMODITY" -> "Commodity";
            case "LOCATION"  -> "Location";
            case "TRIP_TYPE" -> "Trip Type";
            case "TRIP"      -> "Trip";
            case "LOAD"      -> "Load";
            default          -> k;
        };
    }

    private String metricLabel(String m) {
        return switch (m) {
            case "COUNT"    -> "Count";
            case "DISTANCE" -> "Distance (km)";
            case "COST"     -> "Cost (R)";
            case "LITERS"   -> "Fuel (L)";
            case "WEIGHT"   -> "Weight (kg)";
            case "DURATION" -> "Duration (h)";
            default         -> m;
        };
    }

    private String formatValue(double v, String metric) {
        return switch (metric) {
            case "COUNT"    -> String.format("%,.0f", v);
            case "DISTANCE", "WEIGHT", "DURATION" -> String.format("%,.1f", v);
            default -> String.format("%,.2f", v);
        };
    }

    private String safe(String v) {
        return v == null || v.isBlank() ? "—" : v.trim();
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); }
        catch (DateTimeParseException e) { return null; }
    }

    private boolean inRange(LocalDate d, LocalDate from, LocalDate to) {
        if (from == null && to == null) return true;
        if (d == null) return false;
        if (from != null && d.isBefore(from)) return false;
        if (to != null && d.isAfter(to)) return false;
        return true;
    }

    private String sortableDateKey(String s) {
        if (s == null) return "";
        try {
            LocalDate d = LocalDate.parse("01 " + s, DateTimeFormatter.ofPattern("dd MMM yyyy"));
            return String.format("%04d-%02d", d.getYear(), d.getMonthValue());
        } catch (Exception e) {
            return s;
        }
    }

    private Object getObj(Object target, String method) {
        if (target == null) return null;
        try { return target.getClass().getMethod(method).invoke(target); }
        catch (Exception e) { return null; }
    }

    private String getStr(Object target, String method) {
        Object v = getObj(target, method);
        return v == null ? null : v.toString();
    }

    private double getDbl(Object target, String... methods) {
        for (String m : methods) {
            Object v = getObj(target, m);
            if (v instanceof Number n) return n.doubleValue();
        }
        return 0.0;
    }

    private String esc(Object v) {
        if (v == null) return "";
        return v.toString()
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
