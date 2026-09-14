package io.fleetpulse.ingestion.config;

import io.fleetpulse.ingestion.ingest.TelemetryIngestor;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FleetPulseMqttProperties.class)
public class MqttConfig {

    private static final Logger log = LoggerFactory.getLogger(MqttConfig.class);

    @Bean(destroyMethod = "disconnect")
    public MqttAsyncClient mqttClient(FleetPulseMqttProperties props, TelemetryIngestor ingestor) throws MqttException {
        MqttAsyncClient client = new MqttAsyncClient(
                props.broker(), props.clientId(), new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setMaxInflight(1_000);

        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                try {
                    client.subscribe(props.topicFilter(), 1);
                    log.info("Subscribed to {} on {} (reconnect={})", props.topicFilter(), serverURI, reconnect);
                } catch (MqttException e) {
                    throw new IllegalStateException("MQTT subscribe failed", e);
                }
            }
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                ingestor.accept(topic, message.getPayload());
            }
            @Override
            public void connectionLost(Throwable cause) {
                log.warn("MQTT connection lost — auto-reconnect in progress", cause);
            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {}
        });

        client.connect(options);
        return client;
    }
}