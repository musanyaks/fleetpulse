package io.fleetpulse.telemetry.api;

import java.time.Instant;

public record VehicleDto(
        String vehicleId, String plate, String make, String model,
        double speedLimitKph, Instant registeredAt) {}
