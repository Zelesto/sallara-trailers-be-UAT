package com.pgsa.trailers.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openrouteservice.api.key:}")
    private String orsApiKey;

    @Value("${openrouteservice.api.url:https://api.openrouteservice.org}")
    private String orsApiUrl;

    // =========================================================
    // MAIN ENTRY (ONLY METHOD FRONTEND SHOULD CALL INDIRECTLY)
    // =========================================================
    public RoutingResult calculateRoute(String origin,
                                        String destination,
                                        String vehicleType) {

        validate(origin, destination);

        Coordinates originCoords = resolveCoordinates(origin, "origin");
        Coordinates destCoords = resolveCoordinates(destination, "destination");

        return callOpenRouteService(originCoords, destCoords, vehicleType);
    }

    // =========================================================
    // VALIDATION
    // =========================================================
    private void validate(String origin, String destination) {
        if (origin == null || origin.isBlank()) {
            throw new IllegalArgumentException("Origin cannot be empty");
        }
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("Destination cannot be empty");
        }
        if (orsApiKey == null || orsApiKey.isBlank()) {
            throw new IllegalStateException("ORS API key not configured");
        }
    }

    // =========================================================
    // COORDINATE RESOLUTION (GEOCODING LAYER)
    // =========================================================
    private Coordinates resolveCoordinates(String location, String type) {
        try {
            return geocode(location);
        } catch (Exception ex) {
            log.warn("Primary geocode failed for {}: {}", location, ex.getMessage());

            Coordinates fallback = geocodeBroad(location);
            if (fallback != null) return fallback;

            throw new RuntimeException("Unable to resolve location: " + location);
        }
    }

    private Coordinates geocode(String location) throws Exception {

        String url = UriComponentsBuilder
                .fromHttpUrl("https://nominatim.openstreetmap.org/search")
                .queryParam("format", "json")
                .queryParam("q", location)
                .queryParam("limit", 1)
                .queryParam("countrycodes", "za")
                .queryParam("addressdetails", 1)
                .build()
                .toUriString();

        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
        );

        JsonNode node = objectMapper.readTree(response.getBody());

        if (!node.isArray() || node.isEmpty()) {
            throw new RuntimeException("Location not found: " + location);
        }

        JsonNode first = node.get(0);

        return new Coordinates(
                first.get("lat").asDouble(),
                first.get("lon").asDouble()
        );
    }

    private Coordinates geocodeBroad(String location) {
        try {
            String simplified = location
                    .replaceAll("\\d+", "")
                    .replaceAll("(?i)street|road|avenue|drive", "")
                    .trim();

            return geocode(simplified);
        } catch (Exception e) {
            return null;
        }
    }

    // =========================================================
    // ROUTING ENGINE (OpenRouteService ONLY HERE)
    // =========================================================
    private RoutingResult callOpenRouteService(Coordinates origin,
                                               Coordinates destination,
                                               String vehicleType) {

        try {
            String profile = resolveProfile(vehicleType);

            String url = UriComponentsBuilder
                    .fromHttpUrl(orsApiUrl + "/v2/directions/" + profile)
                    .queryParam("api_key", orsApiKey)
                    .queryParam("start", origin.lng + "," + origin.lat)
                    .queryParam("end", destination.lng + "," + destination.lat)
                    .build()
                    .toUriString();

            HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders());

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());

            JsonNode route = root.path("features").get(0);
            JsonNode summary = route.path("properties")
                    .path("segments")
                    .get(0)
                    .path("summary");

            double distanceMeters = summary.path("distance").asDouble();
            double durationSeconds = summary.path("duration").asDouble();

            BigDecimal distanceKm = BigDecimal.valueOf(distanceMeters / 1000)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal durationHours = BigDecimal.valueOf(durationSeconds / 3600)
                    .setScale(2, RoundingMode.HALF_UP);

            return new RoutingResult(
                    origin,
                    destination,
                    distanceKm,
                    durationHours
            );

        } catch (Exception e) {
            log.error("Routing failed", e);
            throw new RuntimeException("Route calculation failed", e);
        }
    }

    // =========================================================
    // VEHICLE PROFILE MAPPING
    // =========================================================
    private String resolveProfile(String vehicleType) {

        if (vehicleType == null) return "driving-car";

        return switch (vehicleType.toUpperCase()) {
            case "TRUCK", "TRAILER", "HGV" -> "driving-hgv";
            case "VAN" -> "driving-car";
            case "BIKE" -> "cycling-regular";
            case "WALK" -> "foot-walking";
            default -> "driving-car";
        };
    }

    // =========================================================
    // HEADERS
    // =========================================================
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "TrailersApp/1.0");
        headers.set("Accept-Language", "en");
        return headers;
    }

    // =========================================================
    // RESULT MODEL
    // =========================================================
    public record RoutingResult(
            Coordinates origin,
            Coordinates destination,
            BigDecimal distanceKm,
            BigDecimal durationHours
    ) {}

    public record Coordinates(
            double lat,
            double lng
    ) {}
}
