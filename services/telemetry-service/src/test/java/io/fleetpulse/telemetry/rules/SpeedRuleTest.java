package io.fleetpulse.telemetry.rules;

import io.fleetpulse.common.AlertType;
import io.fleetpulse.common.Severity;
import io.fleetpulse.common.TelemetryMessage;
import io.fleetpulse.telemetry.vehicle.VehicleProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SpeedRuleTest {

    private final SpeedRule rule = new SpeedRule();
    private final VehicleProfile profile = new VehicleProfile("KDA-482X", 100);
    private final Instant now = Instant.now();

    private TelemetryMessage reading(double speed) {
        return new TelemetryMessage("KDA-482X", now, -1.28, 36.81, speed,
                null, null, null, null, "ON", "Isuzu", "FRR 90", "D001", "John Otieno", "NBO-MSA");
    }

    @Test
    void silentWhenUnderLimit() {
        assertTrue(rule.evaluate(reading(88), profile).isEmpty());
    }

    @Test
    void warningWhenSlightlyOver() {
        var alert = rule.evaluate(reading(115), profile).orElseThrow();
        assertEquals(Severity.WARNING, alert.severity());
        assertEquals(AlertType.SPEEDING, alert.type());
    }

    @Test
    void criticalWhenFarOver() {
        var alert = rule.evaluate(reading(140), profile).orElseThrow();
        assertEquals(Severity.CRITICAL, alert.severity());
    }
}