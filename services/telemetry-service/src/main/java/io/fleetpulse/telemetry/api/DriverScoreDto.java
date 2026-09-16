package io.fleetpulse.telemetry.api;

import java.util.List;

public record DriverScoreDto(
        String driverId, String name, List<String> plates,
        String status, double score,
        long speeding, long harshBraking, long harshAccel) {}
