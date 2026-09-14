package io.fleetpulse.telemetry.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fleetpulse.common.AlertMessage;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public KafkaTemplate<String, AlertMessage> alertKafkaTemplate(
            ObjectMapper objectMapper, @Value("${spring.kafka.bootstrap-servers}") String servers) {
        var factory = new DefaultKafkaProducerFactory<String, AlertMessage>(   // explicit witness
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers),
                new StringSerializer(),
                new JsonSerializer<>(objectMapper));
        return new KafkaTemplate<>(factory);
    }
}