package com.pgsa.trailers.service.routing.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgsa.trailers.service.routing.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class ORSRoutingProvider implements RoutingProvider {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openrouteservice.api.key:}")
    private String apiKey;

    @Override
    public String name() {
        return "openrouteservice";
    }

    @Override
    public boolean supports(String vehicleType) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("ORS API key not configured");
            return false;
        }
        return true;
    }

    @Override
    public RoutingResult calculate(Coordinates origin,
                                   Coordinates destination,
                                   String vehicleType,
                                   Map<String, Object> context) {

        try {
            // Use driving-car for all vehicles (ORS doesn't have truck-specific for free tier)
            String url = "https://api.openrouteservice.org/v2/directions/driving-car";

            String body = String.format(
                "{\"coordinates\": [[%f, %f], [%f, %f]]}",
                origin.getLng(), origin.getLat(),
                destination.getLng(), destination.getLat()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", apiKey);

            log.debug("Calling ORS API: {} -> {}", origin, destination);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("ORS API error: {}", response.getStatusCode());
                throw new RuntimeException("ORS API returned: " + response.getStatusCode());
            }

            if (response.getBody() == null) {
                throw new RuntimeException("ORS API returned empty response");
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            // Check for API error response
            if (root.has("error")) {
                String error = root.path("error").asText();
                throw new RuntimeException("ORS API error: " + error);
            }

            JsonNode features = root.path("features");
            if (!features.isArray() || features.isEmpty()) {
                throw new RuntimeException("ORS returned no features");
            }

            JsonNode segment = features.path(0)
                    .path("properties")
                    .path("segments")
                    .path(0);

            if (segment.isMissingNode()) {
                throw new RuntimeException("ORS returned invalid route structure");
            }

            double meters = segment.path("distance").asDouble();
            double seconds = segment.path("duration").asDouble();

            if (meters <= 0 || seconds <= 0) {
                throw new RuntimeException("ORS returned invalid distance/duration");
            }

            String geometry = features.path(0)
                    .path("geometry")
                    .asText(null);

            log.info("✅ ORS route calculated: {} km, {} hours", 
                meters / 1000.0, seconds / 3600.0);

            return new RoutingResult(
                    origin,
                    destination,
                    BigDecimal.valueOf(meters / 1000.0),
                    BigDecimal.valueOf(seconds / 3600.0),
                    name(),
                    geometry
            );

        } catch (Exception e) {
            log.error("ORS routing failed: {}", e.getMessage());
            throw new RuntimeException("ORS routing failed: " + e.getMessage(), e);
        }
    }
}
