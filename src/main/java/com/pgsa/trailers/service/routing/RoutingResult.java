package com.pgsa.trailers.service.routing;

import java.math.BigDecimal;

/**
 * Unified routing result used across all providers (ORS, Google, Mapbox)
 * Keeps structure consistent for enterprise routing engine
 */
public class RoutingResult {

    private final Coordinates origin;
    private final Coordinates destination;

    // Core metrics
    private final BigDecimal distanceKm;
    private final BigDecimal durationHours;

    // Provider metadata
    private final String provider;
    private final String geometry;

    public RoutingResult(
            Coordinates origin,
            Coordinates destination,
            BigDecimal distanceKm,
            BigDecimal durationHours,
            String provider,
            String geometry
    ) {
        this.origin = origin;
        this.destination = destination;
        this.distanceKm = distanceKm;
        this.durationHours = durationHours;
        this.provider = provider;
        this.geometry = geometry;
    }

    public Coordinates getOrigin() {
        return origin;
    }

    public Coordinates getDestination() {
        return destination;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public BigDecimal getDurationHours() {
        return durationHours;
    }

    public String getProvider() {
        return provider;
    }

    public String getGeometry() {
        return geometry;
    }

    // Convenience helpers

    public BigDecimal getDistanceMiles() {
        if (distanceKm == null) return BigDecimal.ZERO;
        return distanceKm.multiply(BigDecimal.valueOf(0.621371));
    }

    public BigDecimal getDurationMinutes() {
        if (durationHours == null) return BigDecimal.ZERO;
        return durationHours.multiply(BigDecimal.valueOf(60));
    }

    @Override
    public String toString() {
        return "RoutingResult{" +
                "origin=" + origin +
                ", destination=" + destination +
                ", distanceKm=" + distanceKm +
                ", durationHours=" + durationHours +
                ", provider='" + provider + '\'' +
                '}';
    }
}
