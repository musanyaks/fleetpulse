package io.fleetpulse.telemetry.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record AlertDto(
        @JsonProperty("vehicle_id") String vehicleId,
        String type,
        String severity,
        @JsonProperty("observed_value") Double observedValue,
        Double threshold,
        String message,
        @JsonProperty("ts") Instant ts) {}
