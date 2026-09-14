package io.fleetpulse.ingestion.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fleetpulse.common.TelemetryMessage;
import io.fleetpulse.common.Topics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class TelemetryIngestor {

    private static final Logger log = LoggerFactory.getLogger(TelemetryIngestor.class);
    private static final Pattern TOPIC_PATTERN = Pattern.compile("fleet/([^/]+)/telemetry");
    private static final Pattern VEHICLE_ID = Pattern.compile("[A-Z0-9-]{4,16}");

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, TelemetryMessage> kafkaTemplate;
    private final Counter received;
    private final Counter accepted;
    private final Counter rejected;

    public TelemetryIngestor(ObjectMapper objectMapper,
                             KafkaTemplate<String, TelemetryMessage> kafkaTemplate,
                             MeterRegistry meters) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.received = Counter.builder("fleetpulse.telemetry.received").register(meters);
        this.accepted = Counter.builder("fleetpulse.telemetry.accepted").register(meters);
        this.rejected = Counter.builder("fleetpulse.telemetry.rejected").register(meters);
    }

    public void accept(String topic, byte[] payload) {
        received.increment();
        try {
            TelemetryMessage msg = objectMapper.readValue(payload, TelemetryMessage.class);
            List<String> errors = validate(msg, extractVehicleId(topic));
            if (!errors.isEmpty()) {
                rejected.increment();
                log.debug("Rejected telemetry: {}", errors);
                return;
            }
            // Keyed by vehicleId → all readings of one vehicle land on the same partition, in order
            kafkaTemplate.send(Topics.TELEMETRY_RAW, msg.vehicleId(), msg);
            accepted.increment();
        } catch (Exception e) {
            rejected.increment();
            log.warn("Failed to ingest payload from {}: {}", topic, e.getMessage());
        }
    }

    private String extractVehicleId(String topic) {
        var matcher = TOPIC_PATTERN.matcher(topic);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private List<String> validate(TelemetryMessage m, String topicVehicleId) {
        var errors = new ArrayList<String>();
        Instant now = Instant.now();

        if (m.vehicleId() == null || !VEHICLE_ID.matcher(m.vehicleId()).matches()) errors.add("vehicleId");
        if (topicVehicleId != null && !topicVehicleId.equals(m.vehicleId())) errors.add("topic/payload vehicleId mismatch");
        if (m.timestamp() == null
                || m.timestamp().isAfter(now.plusSeconds(300))
                || m.timestamp().isBefore(now.minusSeconds(86_400))) errors.add("timestamp");
        if (Math.abs(m.latitude()) > 90) errors.add("latitude");
        if (Math.abs(m.longitude()) > 180) errors.add("longitude");
        if (m.speedKph() < 0 || m.speedKph() > 250) errors.add("speedKph");
        if (m.engineTempC() != null && (m.engineTempC() < -20 || m.engineTempC() > 180)) errors.add("engineTempC");
        if (m.fuelLevelPct() != null && (m.fuelLevelPct() < 0 || m.fuelLevelPct() > 100)) errors.add("fuelLevelPct");
        if (m.rpm() != null && (m.rpm() < 0 || m.rpm() > 9_000)) errors.add("rpm");
        return errors;
    }
}