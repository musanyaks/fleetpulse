package io.fleetpulse.telemetry.api;

public record GeofenceDto(
        Long id, String name, String type,
        Double lat, Double lon, Double radiusM, boolean active) {}
