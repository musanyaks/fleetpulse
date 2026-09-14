package io.fleetpulse.telemetry.rules;

import io.fleetpulse.common.AlertMessage;
import io.fleetpulse.common.AlertType;
import io.fleetpulse.common.Severity;
import io.fleetpulse.common.TelemetryMessage;
import io.fleetpulse.telemetry.vehicle.VehicleProfile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpeedRule implements TelemetryRule {

    @Override
    public Optional<AlertMessage> evaluate(TelemetryMessage m, VehicleProfile profile) {
        if (m.speedKph() <= profile.speedLimitKph()) return Optional.empty();

        double over = m.speedKph() - profile.speedLimitKph();
        Severity severity = over >= 30 ? Severity.CRITICAL : Severity.WARNING;

        return Optional.of(new AlertMessage(
                m.vehicleId(), AlertType.SPEEDING, severity,
                m.speedKph(), profile.speedLimitKph(),
                "Speeding: %.0f km/h (limit %.0f, +%.0f over)".formatted(
                        m.speedKph(), profile.speedLimitKph(), over),
                m.timestamp()));
    }
}