package io.fleetpulse.common;

import java.time.Instant;

public record AlertMessage(
        String vehicleId,
        AlertType type,
        Severity severity,
        double observedValue,
        double threshold,
        String message,
        Instant timestamp
) {}