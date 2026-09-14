package io.fleetpulse.telemetry.rules;

import io.fleetpulse.common.AlertMessage;
import io.fleetpulse.common.TelemetryMessage;
import io.fleetpulse.telemetry.vehicle.VehicleProfile;

import java.util.Optional;

public interface TelemetryRule {
    Optional<AlertMessage> evaluate(TelemetryMessage reading, VehicleProfile profile);
}