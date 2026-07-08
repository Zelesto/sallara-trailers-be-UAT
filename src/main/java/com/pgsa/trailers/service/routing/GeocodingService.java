package com.pgsa.trailers.service.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public Coordinates geocode(String location) {
        if (location == null || location.isBlank()) {
            throw new RuntimeException("Location is null or empty");
        }

        try {
            // Clean up the address - remove extra spaces, etc.
            String cleanedLocation = cleanAddress(location);
            String encoded = URLEncoder.encode(cleanedLocation, StandardCharsets.UTF_8);

            String url = UriComponentsBuilder
                    .fromHttpUrl("https://nominatim.openstreetmap.org/search")
                    .queryParam("format", "json")
                    .queryParam("q", cleanedLocation)
                    .queryParam("limit", "1")
                    .queryParam("addressdetails", "1")
                    .build()
                    .toUriString();

            log.debug("Geocoding URL: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "TrailersApp/1.0 (routing-service)");
            headers.set("Accept", "application/json");

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Geocoding API failed with status: {}", response.getStatusCode());
                throw new RuntimeException("Geocoding API failed: " + response.getStatusCode());
            }

            if (response.getBody() == null || response.getBody().isBlank()) {
                log.error("Geocoding API returned empty response for: {}", location);
                // Try a fallback with just the city
                return geocodeFallback(location);
            }

            JsonNode arr = objectMapper.readTree(response.getBody());

            if (!arr.isArray() || arr.isEmpty()) {
                log.warn("Location not found: {}, trying fallback", location);
                return geocodeFallback(location);
            }

            JsonNode node = arr.get(0);
            double lat = node.path("lat").asDouble();
            double lon = node.path("lon").asDouble();

            if (lat == 0.0 && lon == 0.0) {
                log.warn("Invalid coordinates for: {}, trying fallback", location);
                return geocodeFallback(location);
            }

            log.info("Geocoded '{}' to lat: {}, lon: {}", location, lat, lon);
            return new Coordinates(lat, lon);

        } catch (Exception e) {
            log.error("Geocoding failed for: {}, trying fallback", location, e);
            return geocodeFallback(location);
        }
    }

    private Coordinates geocodeFallback(String location) {
        // Try to extract city name and use a predefined coordinate
        String city = extractCityName(location);
        return getPredefinedCoordinates(city);
    }

    private String cleanAddress(String address) {
        // Remove "street", "rd", etc. to simplify
        String cleaned = address
                .replaceAll("street", "st")
                .replaceAll("Street", "St")
                .replaceAll("road", "rd")
                .replaceAll("Road", "Rd")
                .replaceAll("avenue", "ave")
                .replaceAll("Avenue", "Ave")
                .replaceAll("\\s+", " ") // Remove extra spaces
                .trim();
        
        // If address has a unit number like "63-lr", remove it
        cleaned = cleaned.replaceAll("\\d+-[a-zA-Z]+", "");
        
        return cleaned;
    }

    private String extractCityName(String location) {
        // Extract city name from address
        String[] parts = location.split(",");
        if (parts.length >= 2) {
            // Usually the city is the second part
            String cityPart = parts[1].trim();
            // Remove any numbers
            cityPart = cityPart.replaceAll("\\d+", "").trim();
            return cityPart;
        }
        // If only one part, use it
        return location.trim();
    }

    private Coordinates getPredefinedCoordinates(String city) {
        // Predefined coordinates for major South African cities
        String lowerCity = city.toLowerCase();
        
        if (lowerCity.contains("cape town") || lowerCity.contains("capetown")) {
            return new Coordinates(-33.9249, 18.4241);
        } else if (lowerCity.contains("polokwane")) {
            return new Coordinates(-23.9037, 29.4546);
        } else if (lowerCity.contains("johannesburg") || lowerCity.contains("joburg")) {
            return new Coordinates(-26.2041, 28.0473);
        } else if (lowerCity.contains("pretoria")) {
            return new Coordinates(-25.7479, 28.2293);
        } else if (lowerCity.contains("durban")) {
            return new Coordinates(-29.8587, 31.0218);
        } else if (lowerCity.contains("boksburg")) {
            return new Coordinates(-26.2125, 28.2596);
        } else if (lowerCity.contains("germiston")) {
            return new Coordinates(-26.2263, 28.1579);
        } else if (lowerCity.contains("port elizabeth") || lowerCity.contains("portelizabeth")) {
            return new Coordinates(-33.9608, 25.6022);
        } else if (lowerCity.contains("east london") || lowerCity.contains("eastlondon")) {
            return new Coordinates(-33.0152, 27.9116);
        } else if (lowerCity.contains("bloemfontein")) {
            return new Coordinates(-29.1167, 26.2167);
        } else if (lowerCity.contains("kimberley")) {
            return new Coordinates(-28.7378, 24.7622);
        } else if (lowerCity.contains("nelspruit")) {
            return new Coordinates(-25.4745, 30.9703);
        } else if (lowerCity.contains("rustenburg")) {
            return new Coordinates(-25.6587, 27.2322);
        } else if (lowerCity.contains("pietermaritzburg") || lowerCity.contains("maritzburg")) {
            return new Coordinates(-29.6006, 30.3794);
        } else {
            // Default to Johannesburg
            log.warn("Unknown city: {}, defaulting to Johannesburg", city);
            return new Coordinates(-26.2041, 28.0473);
        }
    }
}
