package io.fleetpulse.ingestion.config;

import io.fleetpulse.common.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic telemetryRaw() {
        return TopicBuilder.name(Topics.TELEMETRY_RAW).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic telemetryProcessed() {
        return TopicBuilder.name(Topics.TELEMETRY_PROCESSED).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic alerts() {
        return TopicBuilder.name(Topics.ALERTS).partitions(3).replicas(1).build();
    }
}