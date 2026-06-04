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
import java.util.*;

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

    public RoutingResult calculateRoute(String origin, String destination, String vehicleType) {
        try {
            // Validate inputs
            if (origin == null || origin.trim().isEmpty()) {
                throw new IllegalArgumentException("Origin location cannot be null or empty");
            }
            if (destination == null || destination.trim().isEmpty()) {
                throw new IllegalArgumentException("Destination location cannot be null or empty");
            }
            if (orsApiKey == null || orsApiKey.trim().isEmpty()) {
                throw new IllegalStateException("OpenRouteService API key is not configured");
            }

            log.info("Calculating route from '{}' to '{}' for vehicle type: {}", origin, destination, vehicleType);

            // Step 1: Geocode locations with fallback to closest towns/cities
            Coordinates originCoords = geocodeLocationWithFallback(origin, "origin");
            Coordinates destCoords = geocodeLocationWithFallback(destination, "destination");

            log.info("Coordinates: Origin [lat={}, lng={}], Destination [lat={}, lng={}]",
                    originCoords.getLat(), originCoords.getLng(),
                    destCoords.getLat(), destCoords.getLng());

            // Step 2: Calculate route using OpenRouteService Directions API
            return calculateRouteWithCoordinates(originCoords, destCoords, vehicleType);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Route calculation failed: {}", e.getMessage());
            throw new RuntimeException("Route calculation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error calculating route: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate route: " + e.getMessage());
        }
    }

    /**
     * Geocode location with fallback to closest town/city when address not found
     */
    private Coordinates geocodeLocationWithFallback(String location, String locationType) throws Exception {
        try {
            log.debug("Geocoding {} location: {}", locationType, location);
            Coordinates coords = geocodeLocation(location);
            log.info("Successfully geocoded {} '{}' to coordinates [{}, {}]", 
                    locationType, location, coords.getLat(), coords.getLng());
            return coords;
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Location not found")) {
                log.warn("Location '{}' not found, attempting to find closest town/city", location);
                
                // Try to find closest populated place
                Coordinates fallbackCoords = findClosestPopulatedPlace(location);
                
                if (fallbackCoords != null) {
                    log.info("Using fallback location for '{}': [{}, {}]", 
                            location, fallbackCoords.getLat(), fallbackCoords.getLng());
                    return fallbackCoords;
                } else {
                    // Last resort: try a broader search
                    Coordinates broadCoords = geocodeWithBroadSearch(location);
                    if (broadCoords != null) {
                        log.info("Using broad search result for '{}': [{}, {}]", 
                                location, broadCoords.getLat(), broadCoords.getLng());
                        return broadCoords;
                    }
                    throw new RuntimeException("Could not find location or any nearby town/city for: " + location);
                }
            }
            throw e;
        }
    }

    /**
     * Find the closest populated place (town/city) to the given location name
     */
    private Coordinates findClosestPopulatedPlace(String location) throws Exception {
        String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
        
        // Use Nominatim with viewbox and limit to get multiple results
        String url = UriComponentsBuilder.fromHttpUrl("https://nominatim.openstreetmap.org/search")
                .queryParam("format", "json")
                .queryParam("q", encodedLocation)
                .queryParam("limit", "5")
                .queryParam("countrycodes", "za")
                .queryParam("addressdetails", "1")
                .queryParam("accept-language", "en")
                .build()
                .toUriString();

        HttpHeaders headers = createNominatimHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
        );

        if (response.getStatusCode() != HttpStatus.OK) {
            return null;
        }

        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        
        if (!jsonNode.isArray() || jsonNode.size() == 0) {
            return null;
        }

        // Find the first result that is a town, city, or populated place
        for (JsonNode result : jsonNode) {
            String type = result.path("type").asText();
            JsonNode address = result.path("address");
            
            // Check if this is a populated place (town, city, village, municipality)
            boolean isPopulatedPlace = "city".equals(type) || 
                                      "town".equals(type) || 
                                      "village".equals(type) ||
                                      address.has("city") ||
                                      address.has("town") ||
                                      address.has("village") ||
                                      address.has("municipality");
            
            if (isPopulatedPlace || jsonNode.size() == 1) {
                double lat = result.path("lat").asDouble();
                double lon = result.path("lon").asDouble();
                String displayName = result.path("display_name").asText();
                log.debug("Found closest populated place: {} at [{}, {}]", displayName, lat, lon);
                return new Coordinates(lat, lon);
            }
        }
        
        // If no populated place found, return the first result anyway
        JsonNode firstResult = jsonNode.get(0);
        return new Coordinates(
                firstResult.path("lat").asDouble(),
                firstResult.path("lon").asDouble()
        );
    }

    /**
     * Perform a broader search when exact location not found
     */
    private Coordinates geocodeWithBroadSearch(String location) throws Exception {
        String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
        
        // Remove specific qualifiers like street names, numbers, etc.
        String broadSearchTerm = simplifyLocationName(location);
        encodedLocation = URLEncoder.encode(broadSearchTerm, StandardCharsets.UTF_8);
        
        String url = UriComponentsBuilder.fromHttpUrl("https://nominatim.openstreetmap.org/search")
                .queryParam("format", "json")
                .queryParam("q", encodedLocation)
                .queryParam("limit", "1")
                .queryParam("countrycodes", "za")
                .queryParam("addressdetails", "1")
                .queryParam("accept-language", "en")
                .build()
                .toUriString();

        HttpHeaders headers = createNominatimHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                if (jsonNode.isArray() && jsonNode.size() > 0) {
                    JsonNode result = jsonNode.get(0);
                    return new Coordinates(
                            result.path("lat").asDouble(),
                            result.path("lon").asDouble()
                    );
                }
            }
        } catch (Exception e) {
            log.warn("Broad search failed for {}: {}", location, e.getMessage());
        }
        
        return null;
    }

    /**
     * Simplify location name by removing common address components
     */
    private String simplifyLocationName(String location) {
        // Remove common address suffixes and numbers
        String simplified = location.replaceAll("\\d+", "") // Remove numbers
                .replaceAll("(?i)\\b(street|st|avenue|ave|road|rd|lane|ln|drive|dr|court|ct|place|pl|way|circle|cir)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
        
        // If simplification removed too much, use original
        if (simplified.length() < 3) {
            return location;
        }
        
        return simplified;
    }

    private Coordinates geocodeLocation(String location) throws Exception {
        log.debug("Geocoding location: {}", location);

        String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);

        String url = UriComponentsBuilder.fromHttpUrl("https://nominatim.openstreetmap.org/search")
                .queryParam("format", "json")
                .queryParam("q", encodedLocation)
                .queryParam("limit", "1")
                .queryParam("countrycodes", "za")
                .queryParam("addressdetails", "1")
                .build()
                .toUriString();

        log.debug("Calling Nominatim API: {}", url);

        HttpHeaders headers = createNominatimHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Geocoding API returned error: " + response.getStatusCode());
            }

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            if (!jsonNode.isArray() || jsonNode.size() == 0) {
                throw new RuntimeException("Location not found: " + location);
            }

            JsonNode firstResult = jsonNode.get(0);
            double lat = firstResult.path("lat").asDouble();
            double lon = firstResult.path("lon").asDouble();

            String displayName = firstResult.path("display_name").asText();
            log.debug("Geocoded '{}' to: {} (lat={}, lon={})", location, displayName, lat, lon);

            return new Coordinates(lat, lon);

        } catch (Exception e) {
            log.error("Geocoding failed for location '{}': {}", location, e.getMessage());
            throw new RuntimeException("Location not found: " + location, e);
        }
    }

    private HttpHeaders createNominatimHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "TrailersApp/1.0 (theo.zwane@company.com)");
        headers.set("Accept-Language", "en");
        headers.set("Accept", "application/json");
        return headers;
    }

    private RoutingResult calculateRouteWithCoordinates(Coordinates originCoords, 
                                                        Coordinates destCoords, 
                                                        String vehicleType) throws Exception {
        String profile = getVehicleProfile(vehicleType);

        String url = UriComponentsBuilder.fromHttpUrl(orsApiUrl + "/v2/directions/" + profile)
                .queryParam("api_key", orsApiKey)
                .queryParam("start", originCoords.getLng() + "," + originCoords.getLat())
                .queryParam("end", destCoords.getLng() + "," + destCoords.getLat())
                .build()
                .toUriString();

        log.debug("Calling ORS API: {}", url.replace(orsApiKey, "***"));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
            );

            log.debug("ORS API Response Status: {}", response.getStatusCode());

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("ORS API returned error: " + response.getStatusCode());
            }

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            if (jsonNode.has("error")) {
                String errorMsg = jsonNode.path("error").path("message").asText("Unknown error");
                throw new RuntimeException("ORS API error: " + errorMsg);
            }

            if (!jsonNode.has("features") || jsonNode.get("features").size() == 0) {
                throw new RuntimeException("No route found between locations");
            }

            JsonNode route = jsonNode.get("features").get(0);
            JsonNode properties = route.path("properties");
            JsonNode summary = properties.path("summary");

            if (summary.isMissingNode()) {
                JsonNode segments = properties.path("segments");
                if (segments.isArray() && segments.size() > 0) {
                    summary = segments.get(0).path("summary");
                }
            }

            if (summary.isMissingNode()) {
                throw new RuntimeException("Could not parse route information from response");
            }

            double distanceMeters = summary.path("distance").asDouble();
            double durationSeconds = summary.path("duration").asDouble();

            BigDecimal distanceKm = new BigDecimal(distanceMeters / 1000.0)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal durationHours = new BigDecimal(durationSeconds / 3600.0)
                    .setScale(2, RoundingMode.HALF_UP);

            log.info("Route calculated successfully: {} km, {} hours ({} minutes)",
                    distanceKm, durationHours, BigDecimal.valueOf(durationSeconds / 60.0).setScale(0, RoundingMode.HALF_UP));

            return new RoutingResult(originCoords, destCoords, distanceKm, durationHours);

        } catch (Exception e) {
            log.error("Error calling ORS API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate route between coordinates", e);
        }
    }

    private String getVehicleProfile(String vehicleType) {
        if (vehicleType == null || vehicleType.trim().isEmpty()) {
            return "driving-car";
        }

        String normalizedType = vehicleType.trim().toUpperCase();

        switch (normalizedType) {
            case "TRUCK":
            case "TRAILER":
            case "HGV":
            case "HEAVY":
                return "driving-hgv";
            case "VAN":
            case "DELIVERY":
                return "driving-car";
            case "BIKE":
            case "BICYCLE":
                return "cycling-regular";
            case "WALK":
            case "FOOT":
                return "foot-walking";
            case "CAR":
            default:
                return "driving-car";
        }
    }

    public RoutingResult calculateRouteDirect(double startLat, double startLng,
                                              double endLat, double endLng,
                                              String vehicleType) {
        try {
            if (orsApiKey == null || orsApiKey.trim().isEmpty()) {
                throw new IllegalStateException("OpenRouteService API key is not configured");
            }

            log.info("Calculating direct route from [{}, {}] to [{}, {}]",
                    startLat, startLng, endLat, endLng);

            return calculateRouteWithCoordinates(
                    new Coordinates(startLat, startLng),
                    new Coordinates(endLat, endLng),
                    vehicleType
            );

        } catch (Exception e) {
            log.error("Error in direct route calculation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate direct route: " + e.getMessage());
        }
    }

    @Data
    @RequiredArgsConstructor
    public static class RoutingResult {
        private final Coordinates origin;
        private final Coordinates destination;
        private final BigDecimal distance;
        private final BigDecimal duration;

        public BigDecimal getDistanceMiles() {
            return distance.multiply(BigDecimal.valueOf(0.621371)).setScale(2, RoundingMode.HALF_UP);
        }

        public BigDecimal getDurationMinutes() {
            return duration.multiply(BigDecimal.valueOf(60)).setScale(0, RoundingMode.HALF_UP);
        }
    }

    @Data
    @RequiredArgsConstructor
    public static class Coordinates {
        private final double lat;
        private final double lng;

        public String toString() {
            return String.format("%.6f,%.6f", lat, lng);
        }
    }
}
