package io.fleetpulse.common;

import java.time.Instant;

public record TelemetryMessage(
        String vehicleId,
        Instant timestamp,
        double latitude,
        double longitude,
        double speedKph,
        Double engineTempC,
        Double fuelLevelPct,
        Double rpm,
        Double odometerKm,
        String ignition            // "ON" | "OFF"
) {}