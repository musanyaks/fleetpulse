package io.fleetpulse.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fleetpulse.mqtt")
public record FleetPulseMqttProperties(String broker, String clientId, String topicFilter) {}