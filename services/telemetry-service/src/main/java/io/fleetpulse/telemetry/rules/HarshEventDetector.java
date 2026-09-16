package io.fleetpulse.telemetry.rules;

import io.fleetpulse.common.AlertMessage;
import io.fleetpulse.common.AlertType;
import io.fleetpulse.common.Severity;
import io.fleetpulse.common.TelemetryMessage;
import io.fleetpulse.telemetry.vehicle.VehicleProfile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Driver-behavior rule: detects harsh braking / acceleration from the speed
 * delta between consecutive readings of the same vehicle (same Kafka partition,
 * so ordering is guaranteed). Stateful by design — one entry per active vehicle.
 */
@Component
public class HarshEventDetector implements TelemetryRule {

    private static final double BRAKE_DROP_KPH = 20.0;   // speed drop within one tick
    private static final double ACCEL_RISE_KPH = 10.0;   // speed rise within one tick
    private static final long STALE_MS = 10_000;         // gap too big → skip (restart, jitter)

    private record Last(double speedKph, long atMs) {}

    private final Map<String, Last> last = new ConcurrentHashMap<>();
    private final AlertCooldown cooldown;

    public HarshEventDetector(AlertCooldown cooldown) {
        this.cooldown = cooldown;
    }

    @Override
    public Optional<AlertMessage> evaluate(TelemetryMessage m, VehicleProfile profile) {
        long now = System.currentTimeMillis();
        Last prev = last.put(m.vehicleId(), new Last(m.speedKph(), now));

        if (prev == null || now - prev.atMs() > STALE_MS) return Optional.empty();

        double delta = m.speedKph() - prev.speedKph();

        if (delta <= -BRAKE_DROP_KPH) {
            if (!cooldown.tryAcquire(m.vehicleId() + ":HARSH_BRAKING", Duration.ofMinutes(2)))
                return Optional.empty();
            return Optional.of(new AlertMessage(m.vehicleId(), AlertType.HARSH_BRAKING,
                    Severity.WARNING, -delta, BRAKE_DROP_KPH,
                    "Harsh braking: %.0f km/h drop".formatted(-delta), m.timestamp()));
        }
        if (delta >= ACCEL_RISE_KPH) {
            if (!cooldown.tryAcquire(m.vehicleId() + ":HARSH_ACCELERATION", Duration.ofMinutes(2)))
                return Optional.empty();
            return Optional.of(new AlertMessage(m.vehicleId(), AlertType.HARSH_ACCELERATION,
                    Severity.WARNING, delta, ACCEL_RISE_KPH,
                    "Harsh acceleration: +%.0f km/h".formatted(delta), m.timestamp()));
        }
        return Optional.empty();
    }
}
