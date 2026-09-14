package com.musarioba.fleetpulse.telemetry;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TelemetryConsumer {
    @KafkaListener(topics = TelemetryProducer.TOPIC, groupId = "fleetpulse-analytics")
    public void consume(TelemetryEvent event) {
        // Starter processing point.
        // Next phase: persist time-series data, update Redis vehicle state,
        // calculate driver/fleet KPIs and emit alerts.
        System.out.printf(
                "Telemetry: %s speed=%.1f fuel=%.1f temp=%.1f%n",
                event.vehicleRegistrationNumber(),
                event.speed(), event.fuelLevel(), event.engineTemperature());
    }
}
