package io.fleetpulse.simulator;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableConfigurationProperties(SimulatorProperties.class)
public class SimulatorApplication {

    private static final Logger log = LoggerFactory.getLogger(SimulatorApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SimulatorApplication.class, args);
    }

    @Bean
    CommandLineRunner fleet(SimulatorProperties props) {
        return args -> {
            MqttAsyncClient client = new MqttAsyncClient(
                    props.broker(), "fleet-simulator-" + UUID.randomUUID().toString().substring(0, 8),
                    new MemoryPersistence());
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setAutomaticReconnect(true);
            opts.setCleanSession(true);
            client.connect(opts).waitForCompletion(TimeUnit.SECONDS.toMillis(10));

            Random rnd = new Random();
            ScheduledExecutorService pool =
                    Executors.newScheduledThreadPool(Math.min(props.fleetSize(), 16));

            for (int i = 0; i < props.fleetSize(); i++) {
                var sim = new VehicleSimulator(plate(i, rnd), props, client, rnd);
                // staggered start so the fleet doesn't tick in lockstep
                pool.scheduleAtFixedRate(sim::tick, rnd.nextInt(props.intervalMs()),
                        props.intervalMs(), TimeUnit.MILLISECONDS);
            }
            log.info("🚚 Simulating {} vehicles -> {} every {}ms",
                    props.fleetSize(), props.broker(), props.intervalMs());
            new CountDownLatch(1).await();   // run forever
        };
    }

    private static String plate(int i, Random rnd) {
        return String.format("%c%c%c-%03d%c",
                (char) ('A' + rnd.nextInt(26)), (char) ('A' + rnd.nextInt(26)),
                (char) ('A' + rnd.nextInt(26)), 100 + i, (char) ('A' + rnd.nextInt(26)));
    }
}