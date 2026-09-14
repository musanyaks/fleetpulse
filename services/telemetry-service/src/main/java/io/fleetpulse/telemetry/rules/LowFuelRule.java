package io.fleetpulse.telemetry.rules;

import io.fleetpulse.common.AlertMessage;
import io.fleetpulse.common.AlertType;
import io.fleetpulse.common.Severity;
import io.fleetpulse.common.TelemetryMessage;
import io.fleetpulse.telemetry.vehicle.VehicleProfile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LowFuelRule implements TelemetryRule {

    @Override
    public Optional<AlertMessage> evaluate(TelemetryMessage m, VehicleProfile profile) {
        if (m.fuelLevelPct() == null || m.fuelLevelPct() > 10) return Optional.empty();
        return Optional.of(new AlertMessage(
                m.vehicleId(), AlertType.LOW_FUEL, Severity.INFO,
                m.fuelLevelPct(), 10.0,
                "Fuel level low: %.1f%%".formatted(m.fuelLevelPct()),
                m.timestamp()));
    }
}