package com.pgsa.trailers.service.routing.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgsa.trailers.service.routing.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

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
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public RoutingResult calculate(Coordinates origin,
                                   Coordinates destination,
                                   String vehicleType,
                                   Map<String, Object> context) {

        try {
            String url = "https://api.openrouteservice.org/v2/directions/driving-hgv";

            String body = """
            {
              "coordinates": [
                [%f, %f],
                [%f, %f]
              ]
            }
            """.formatted(
                    origin.getLng(), origin.getLat(),
                    destination.getLng(), destination.getLat()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", apiKey);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("ORS invalid response: " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            JsonNode segment = root.path("features")
                    .path(0)
                    .path("properties")
                    .path("segments")
                    .path(0);

            if (segment.isMissingNode()) {
                throw new RuntimeException("ORS returned invalid route structure");
            }

            double meters = segment.path("distance").asDouble();
            double seconds = segment.path("duration").asDouble();

            String geometry = root.path("features")
                    .path(0)
                    .path("geometry")
                    .asText(null);

            return new RoutingResult(
                    origin,
                    destination,
                    BigDecimal.valueOf(meters / 1000.0),
                    BigDecimal.valueOf(seconds / 3600.0),
                    name(),
                    geometry
            );

        } catch (Exception e) {
            throw new RuntimeException("ORS routing failed", e);
        }
    }
}
