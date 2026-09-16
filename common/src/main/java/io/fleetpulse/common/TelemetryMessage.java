package io.fleetpulse.common;

import jakarta.validation.constraints.*;

import java.time.Instant;

public record TelemetryMessage(
        @NotBlank @Pattern(regexp = "[A-Z0-9-]{4,16}") String vehicleId,
        @NotNull Instant timestamp,
        @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
        @DecimalMin("0.0") @DecimalMax("250.0") double speedKph,
        @DecimalMin("-20.0") @DecimalMax("180.0") Double engineTempC,
        @DecimalMin("0.0") @DecimalMax("100.0") Double fuelLevelPct,
        @DecimalMin("0.0") @DecimalMax("9000.0") Double rpm,
        @PositiveOrZero Double odometerKm,
        @Pattern(regexp = "ON|OFF") String ignition,
        @Size(max = 40) String make,
        @Size(max = 40) String model,
        @Size(max = 24) String driverId,
        @Size(max = 60) String driverName
) {}
