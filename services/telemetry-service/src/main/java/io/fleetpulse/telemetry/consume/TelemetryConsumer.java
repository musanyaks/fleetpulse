package io.fleetpulse.telemetry.consume;

import io.fleetpulse.common.TelemetryMessage;
import io.fleetpulse.common.Topics;
import io.fleetpulse.telemetry.alert.AlertPublisher;
import io.fleetpulse.telemetry.live.LiveVehicleStateService;
import io.fleetpulse.telemetry.rules.AlertCooldown;
import io.fleetpulse.telemetry.rules.TelemetryRule;
import io.fleetpulse.telemetry.vehicle.VehicleProfile;
import io.fleetpulse.telemetry.vehicle.VehicleRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class TelemetryConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelemetryConsumer.class);

    private static final String INSERT_SQL = """
            INSERT INTO telemetry_readings
              (vehicle_id, ts, latitude, longitude, speed_kph,
               engine_temp_c, fuel_level_pct, rpm, odometer_km, ignition)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbc;
    private final VehicleRegistry registry;
    private final List<TelemetryRule> rules;          // all TelemetryRule beans, injected as a list
    private final AlertPublisher alertPublisher;
    private final AlertCooldown cooldown;
    private final LiveVehicleStateService liveState;
    private final Counter consumed;
    private final Counter persisted;
    private final Counter writeFailures;

    public TelemetryConsumer(JdbcTemplate jdbc, VehicleRegistry registry, List<TelemetryRule> rules,
                             AlertPublisher alertPublisher, AlertCooldown cooldown,
                             LiveVehicleStateService liveState, MeterRegistry meters) {
        this.jdbc = jdbc;
        this.registry = registry;
        this.rules = rules;
        this.alertPublisher = alertPublisher;
        this.cooldown = cooldown;
        this.liveState = liveState;
        this.consumed = Counter.builder("fleetpulse.telemetry.consumed").register(meters);
        this.persisted = Counter.builder("fleetpulse.telemetry.persisted").register(meters);
        this.writeFailures = Counter.builder("fleetpulse.telemetry.write.failures").register(meters);
    }

    @KafkaListener(topics = Topics.TELEMETRY_RAW, groupId = "telemetry-service",
                   containerFactory = "telemetryListenerContainerFactory")
    public void onBatch(List<TelemetryMessage> batch) {
        consumed.increment(batch.size());
        persistBatch(batch);
        batch.forEach(this::process);
    }

    private void persistBatch(List<TelemetryMessage> batch) {
        try {
            jdbc.batchUpdate(INSERT_SQL, batch, 500, (ps, m) -> {
                ps.setString(1, m.vehicleId());
                ps.setObject(2, OffsetDateTime.ofInstant(m.timestamp(), ZoneOffset.UTC));
                ps.setDouble(3, m.latitude());
                ps.setDouble(4, m.longitude());
                ps.setDouble(5, m.speedKph());
                ps.setObject(6, m.engineTempC());
                ps.setObject(7, m.fuelLevelPct());
                ps.setObject(8, m.rpm());
                ps.setObject(9, m.odometerKm());
                ps.setString(10, m.ignition());
            });
            persisted.increment(batch.size());
        } catch (Exception e) {
            writeFailures.increment();
            log.error("TimescaleDB batch write failed for {} messages", batch.size(), e);
        }
    }

    private void process(TelemetryMessage m) {
        liveState.update(m);                                        // feeds the digital-twin seed
        VehicleProfile profile = registry.profileFor(m.vehicleId());
        for (TelemetryRule rule : rules) {
            rule.evaluate(m, profile)
                .filter(a -> cooldown.tryAcquire(m.vehicleId() + ":" + a.type(), Duration.ofMinutes(5)))
                .ifPresent(alertPublisher::publish);
        }
    }
}