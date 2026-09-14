package com.musarioba.fleetpulse.telemetry;

import jakarta.validation.constraints.*;
import java.time.Instant;

public record TelemetryEvent(
        @NotBlank String vehicleRegistrationNumber,
        @NotNull Instant timestamp,
        @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
        @PositiveOrZero double speed,
        @PositiveOrZero double engineTemperature,
        @PositiveOrZero @DecimalMax("100.0") double fuelLevel,
        @PositiveOrZero double rpm,
        boolean harshBraking,
        boolean harshAcceleration) {}
