package com.musarioba.fleetpulse.telemetry;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TelemetryProducer {
    public static final String TOPIC = "telemetry.raw";
    private final KafkaTemplate<String, TelemetryEvent> kafkaTemplate;

    public TelemetryProducer(KafkaTemplate<String, TelemetryEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TelemetryEvent event) {
        kafkaTemplate.send(TOPIC, event.vehicleRegistrationNumber(), event);
    }
}
