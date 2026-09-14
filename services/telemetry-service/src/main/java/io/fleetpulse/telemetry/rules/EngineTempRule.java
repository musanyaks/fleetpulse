package io.fleetpulse.telemetry.rules;

import io.fleetpulse.common.AlertMessage;
import io.fleetpulse.common.AlertType;
import io.fleetpulse.common.Severity;
import io.fleetpulse.common.TelemetryMessage;
import io.fleetpulse.telemetry.vehicle.VehicleProfile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EngineTempRule implements TelemetryRule {

    private static final double WARNING_C = 105.0;
    private static final double CRITICAL_C = 115.0;

    @Override
    public Optional<AlertMessage> evaluate(TelemetryMessage m, VehicleProfile profile) {
        if (m.engineTempC() == null || m.engineTempC() < WARNING_C) return Optional.empty();

        Severity severity = m.engineTempC() >= CRITICAL_C ? Severity.CRITICAL : Severity.WARNING;
        return Optional.of(new AlertMessage(
                m.vehicleId(), AlertType.ENGINE_OVERHEATING, severity,
                m.engineTempC(), severity == Severity.CRITICAL ? CRITICAL_C : WARNING_C,
                "Engine temperature %.1f°C".formatted(m.engineTempC()),
                m.timestamp()));
    }
}