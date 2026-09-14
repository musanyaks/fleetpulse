package com.musarioba.fleetpulse.config;

import com.musarioba.fleetpulse.telemetry.TelemetryEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaConfig {
    @Bean
    NewTopic telemetryRawTopic() {
        return new NewTopic("telemetry.raw", 3, (short) 1);
    }

    @Bean
    org.springframework.kafka.support.serializer.JsonSerializer<TelemetryEvent> telemetrySerializer() {
        return new JsonSerializer<>();
    }
}
