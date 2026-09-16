package io.fleetpulse.telemetry.live;

import io.fleetpulse.common.TelemetryMessage;

import java.time.Instant;

public record LiveVehicleState(
        String vehicleId, double latitude, double longitude, double speedKph,
        Double engineTempC, Double fuelLevelPct, String status, Instant lastSeen,
        String make, String model, String driverId, String driverName) {

    public static LiveVehicleState from(TelemetryMessage m) {
        return new LiveVehicleState(m.vehicleId(), m.latitude(), m.longitude(), m.speedKph(),
                m.engineTempC(), m.fuelLevelPct(),
                m.speedKph() < 1 ? "IDLE" : "MOVING", m.timestamp(),
                m.make(), m.model(), m.driverId(), m.driverName());
    }
}
