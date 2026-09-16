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

import java.util.List;
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

    /** Fixed fleet — realistic Kenyan registration plates (K LL NNN L) and makes common on Kenyan roads. */
    record FleetVehicle(String plate, String make, String model) {}

    static final List<FleetVehicle> MANIFEST = List.of(
        new FleetVehicle("KDA482X", "Isuzu",            "FRR 90"),
        new FleetVehicle("KCX125Y", "Isuzu",            "NPR"),
        new FleetVehicle("KBZ341M", "Mitsubishi Fuso",  "Canter"),
        new FleetVehicle("KCF210R", "Toyota",           "Hiace"),
        new FleetVehicle("KDG776T", "Isuzu",            "FVZ"),
        new FleetVehicle("KBX549L", "UD Trucks",        "Quester"),
        new FleetVehicle("KDC903V", "Hino",             "300"),
        new FleetVehicle("KCE118N", "Scania",           "R440"),
        new FleetVehicle("KDF655P", "Isuzu",            "NQR"),
        new FleetVehicle("KCY238B", "Mitsubishi Fuso",  "Fighter"),
        new FleetVehicle("KDA091H", "Toyota",           "Dyna"),
        new FleetVehicle("KCZ447D", "Isuzu",            "ELF"),
        new FleetVehicle("KDB720J", "UD Trucks",        "Croner"),
        new FleetVehicle("KCC365S", "Toyota",           "Hiace Matatu"),
        new FleetVehicle("KDD584K", "Isuzu",            "FVR"),
        new FleetVehicle("KCG129U", "Hino",             "500"),
        new FleetVehicle("KCH836Y", "Mitsubishi Fuso",  "Canter"),
        new FleetVehicle("KCJ272N", "Isuzu",            "NPR Pro"),
        new FleetVehicle("KCK610F", "Scania",           "P280"),
        new FleetVehicle("KCL495T", "Toyota",           "Coaster"),
        new FleetVehicle("KCM958L", "Isuzu",            "FTS 4x4"),
        new FleetVehicle("KCN337R", "UD Trucks",        "Kuzer"),
        new FleetVehicle("KCP764H", "Mitsubishi Fuso",  "Rosa"),
        new FleetVehicle("KCQ148M", "Isuzu",            "Giga CXZ"),
        new FleetVehicle("KCR521B", "Hino",             "700")
    );

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
            int size = Math.min(props.fleetSize(), MANIFEST.size());
            if (props.fleetSize() > MANIFEST.size()) {
                log.warn("FLEET_SIZE={} exceeds manifest of {} — running the fixed manifest fleet.",
                        props.fleetSize(), MANIFEST.size());
            }

            ScheduledExecutorService pool = Executors.newScheduledThreadPool(Math.min(size, 16));
            for (int i = 0; i < size; i++) {
                FleetVehicle v = MANIFEST.get(i);
                var sim = new VehicleSimulator(v.plate(), v.make(), v.model(), props, client, rnd);
                // staggered start so the fleet doesn't tick in lockstep
                pool.scheduleAtFixedRate(sim::tick, rnd.nextInt(props.intervalMs()),
                        props.intervalMs(), TimeUnit.MILLISECONDS);
            }
            log.info("🚚 Simulating {} Kenyan fleet vehicles -> {} every {}ms",
                    size, props.broker(), props.intervalMs());
            new CountDownLatch(1).await();   // run forever
        };
    }
}
