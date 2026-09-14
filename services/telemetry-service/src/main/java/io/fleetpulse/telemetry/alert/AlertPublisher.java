package io.fleetpulse.telemetry.alert;

import io.fleetpulse.common.AlertMessage;
import io.fleetpulse.common.Topics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class AlertPublisher {

    private static final Logger log = LoggerFactory.getLogger(AlertPublisher.class);
    private static final String INSERT_SQL = """
            INSERT INTO alerts (vehicle_id, type, severity, observed_value, threshold, message, ts)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private final KafkaTemplate<String, AlertMessage> kafka;
    private final JdbcTemplate jdbc;
    private final Counter published;

    public AlertPublisher(KafkaTemplate<String, AlertMessage> kafka, JdbcTemplate jdbc, MeterRegistry meters) {
        this.kafka = kafka;
        this.jdbc = jdbc;
        this.published = Counter.builder("fleetpulse.alerts.published").register(meters);
    }

    public void publish(AlertMessage alert) {
        kafka.send(Topics.ALERTS, alert.vehicleId(), alert);
        jdbc.update(INSERT_SQL,
                alert.vehicleId(), alert.type().name(), alert.severity().name(),
                alert.observedValue(), alert.threshold(), alert.message(),
                OffsetDateTime.ofInstant(alert.timestamp(), ZoneOffset.UTC));
        published.increment();
        log.debug("Alert: {}", alert);
    }
}