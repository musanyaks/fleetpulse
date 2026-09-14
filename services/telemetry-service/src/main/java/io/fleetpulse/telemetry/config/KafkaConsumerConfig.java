package io.fleetpulse.telemetry.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fleetpulse.common.TelemetryMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, TelemetryMessage> telemetryConsumerFactory(
            ObjectMapper objectMapper, @Value("${spring.kafka.bootstrap-servers}") String servers) {
        JsonDeserializer<TelemetryMessage> valueDeserializer =
                new JsonDeserializer<>(TelemetryMessage.class, objectMapper, false);
        valueDeserializer.addTrustedPackages("*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(valueDeserializer));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TelemetryMessage> telemetryListenerContainerFactory(
            ConsumerFactory<String, TelemetryMessage> consumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, TelemetryMessage>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);                  // batch = throughput
        factory.setConcurrency(3);                       // 3 consumers × batched polls
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        return factory;
    }
}