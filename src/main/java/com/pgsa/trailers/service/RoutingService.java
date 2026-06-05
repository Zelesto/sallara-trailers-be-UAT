package com.pgsa.trailers.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // ===================== ORS =====================
    @Value("${openrouteservice.api.key:}")
    private String orsApiKey;

    @Value("${openrouteservice.api.url:https://api.openrouteservice.org}")
    private String orsApiUrl;

    // ===================== GOOGLE =====================
    @Value("${google.maps.api.key:}")
    private String googleApiKey;

    @Value("${google.maps.api.url:https://maps.googleapis.com/maps/api}")
    private String googleApiUrl;

    // ===================== MAPBOX =====================
    @Value("${mapbox.api.token:}")
    private String mapboxToken;

    // =========================================================
    // MAIN ENTRY
    // =========================================================
    public RoutingResult calculateRoute(String origin, String destination, String vehicleType, String provider) {

        try {
            if (provider == null) provider = "ors";

            log.info("Routing via {} from '{}' to '{}'", provider, origin, destination);

            switch (provider.toLowerCase()) {

                case "google":
                    return calculateWithGoogle(origin, destination, vehicleType);

                case "mapbox":
                    return calculateWithMapbox(origin, destination, vehicleType);

                case "ors":
                default:
                    return calculateWithOpenRouteService(origin, destination, vehicleType);
            }

        } catch (Exception e) {
            log.error("Routing failed: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    // =========================================================
    // GOOGLE ROUTING (NEW)
    // =========================================================
    private RoutingResult calculateWithGoogle(String origin, String destination, String vehicleType) throws Exception {

        if (googleApiKey == null || googleApiKey.isEmpty()) {
            throw new IllegalStateException("Google Maps API key not configured");
        }

        Coordinates originCoords = geocodeLocation(origin);
        Coordinates destCoords = geocodeLocation(destination);

        String mode = getGoogleTravelMode(vehicleType);

        String url = UriComponentsBuilder
                .fromHttpUrl(googleApiUrl + "/directions/json")
                .queryParam("origin", originCoords.getLat() + "," + originCoords.getLng())
                .queryParam("destination", destCoords.getLat() + "," + destCoords.getLng())
                .queryParam("mode", mode)
                .queryParam("key", googleApiKey)
                .build()
                .toUriString();

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class
        );

        JsonNode root = objectMapper.readTree(response.getBody());

        if (!"OK".equals(root.path("status").asText())) {
            throw new RuntimeException("Google API error: " + root.path("status").asText());
        }

        JsonNode leg = root.path("routes").get(0).path("legs").get(0);

        double distanceMeters = leg.path("distance").path("value").asDouble();
        double durationSeconds = leg.path("duration").path("value").asDouble();

        return new RoutingResult(
                originCoords,
                destCoords,
                BigDecimal.valueOf(distanceMeters / 1000.0).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(durationSeconds / 3600.0).setScale(2, RoundingMode.HALF_UP)
        );
    }

    private String getGoogleTravelMode(String vehicleType) {
        if (vehicleType == null) return "driving";

        switch (vehicleType.toUpperCase()) {
            case "BIKE":
            case "BICYCLE":
                return "bicycling";
            case "WALK":
            case "FOOT":
                return "walking";
            default:
                return "driving";
        }
    }

    // =========================================================
    // ORS ROUTING (YOUR EXISTING LOGIC)
    // =========================================================
    private RoutingResult calculateWithOpenRouteService(String origin, String destination, String vehicleType) throws Exception {

        Coordinates originCoords = geocodeLocation(origin);
        Coordinates destCoords = geocodeLocation(destination);

        String profile = getVehicleProfile(vehicleType);

        String url = UriComponentsBuilder
                .fromHttpUrl(orsApiUrl + "/v2/directions/" + profile)
                .queryParam("api_key", orsApiKey)
                .queryParam("start", originCoords.getLng() + "," + originCoords.getLat())
                .queryParam("end", destCoords.getLng() + "," + destCoords.getLat())
                .build()
                .toUriString();

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class
        );

        JsonNode jsonNode = objectMapper.readTree(response.getBody());

        JsonNode summary = jsonNode
                .path("features").get(0)
                .path("properties")
                .path("segments").get(0)
                .path("summary");

        double distanceMeters = summary.path("distance").asDouble();
        double durationSeconds = summary.path("duration").asDouble();

        return new RoutingResult(
                originCoords,
                destCoords,
                BigDecimal.valueOf(distanceMeters / 1000.0).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(durationSeconds / 3600.0).setScale(2, RoundingMode.HALF_UP)
        );
    }

    // =========================================================
    // MAPBOX (OPTIONAL - KEEP YOUR EXISTING IMPLEMENTATION)
    // =========================================================
    private RoutingResult calculateWithMapbox(String origin, String destination, String vehicleType) throws Exception {

        if (mapboxToken == null || mapboxToken.isEmpty()) {
            throw new IllegalStateException("Mapbox token not configured");
        }

        Coordinates o = geocodeLocation(origin);
        Coordinates d = geocodeLocation(destination);

        String url = "https://api.mapbox.com/directions/v5/mapbox/driving/"
                + o.getLng() + "," + o.getLat() + ";"
                + d.getLng() + "," + d.getLat()
                + "?access_token=" + mapboxToken
                + "&geometries=geojson";

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class
        );

        JsonNode root = objectMapper.readTree(response.getBody());

        JsonNode route = root.path("routes").get(0);

        return new RoutingResult(
                o,
                d,
                BigDecimal.valueOf(route.path("distance").asDouble() / 1000.0).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(route.path("duration").asDouble() / 3600.0).setScale(2, RoundingMode.HALF_UP)
        );
    }

    // =========================================================
    // SIMPLE GEOCODER (NOMINATIM)
    // =========================================================
    private Coordinates geocodeLocation(String location) throws Exception {

        String encoded = URLEncoder.encode(location, StandardCharsets.UTF_8);

        String url = "https://nominatim.openstreetmap.org/search?format=json&q="
                + encoded + "&limit=1&countrycodes=za";

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "TrailersApp/1.0");

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        JsonNode node = objectMapper.readTree(response.getBody());

        if (!node.isArray() || node.size() == 0) {
            throw new RuntimeException("Location not found: " + location);
        }

        JsonNode r = node.get(0);

        return new Coordinates(
                r.path("lat").asDouble(),
                r.path("lon").asDouble()
        );
    }

    // =========================================================
    // VEHICLE PROFILE (ORS)
    // =========================================================
    private String getVehicleProfile(String vehicleType) {
        if (vehicleType == null) return "driving-car";

        switch (vehicleType.toUpperCase()) {
            case "TRUCK":
            case "TRAILER":
            case "HGV":
                return "driving-hgv";
            default:
                return "driving-car";
        }
    }

    // =========================================================
    // DATA CLASSES
    // =========================================================
    @Data
    public static class RoutingResult {
        private final Coordinates origin;
        private final Coordinates destination;
        private final BigDecimal distanceKm;
        private final BigDecimal durationHours;
    }

    @Data
    public static class Coordinates {
        private final double lat;
        private final double lng;
    }
}
