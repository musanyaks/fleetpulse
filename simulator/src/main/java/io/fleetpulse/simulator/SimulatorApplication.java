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

    record FleetVehicle(String plate, String make, String model, String driverId, String driverName) {}

    static final List<FleetVehicle> MANIFEST = List.of(
        new FleetVehicle("KDA482X", "Isuzu",            "FRR 90",         "D001", "John Otieno"),
        new FleetVehicle("KCX125Y", "Isuzu",            "NPR",            "D002", "Mary Achieng"),
        new FleetVehicle("KBZ341M", "Mitsubishi Fuso",  "Canter",         "D003", "Peter Kamau"),
        new FleetVehicle("KCF210R", "Toyota",           "Hiace",          "D004", "David Mwangi"),
        new FleetVehicle("KDG776T", "Isuzu",            "FVZ",            "D005", "Grace Wanjiku"),
        new FleetVehicle("KBX549L", "UD Trucks",        "Quester",        "D006", "Samuel Kariuki"),
        new FleetVehicle("KDC903V", "Hino",             "300",            "D007", "Esther Njeri"),
        new FleetVehicle("KCE118N", "Scania",           "R440",           "D008", "Brian Omondi"),
        new FleetVehicle("KDF655P", "Isuzu",            "NQR",            "D009", "Faith Chebet"),
        new FleetVehicle("KCY238B", "Mitsubishi Fuso",  "Fighter",        "D010", "Joseph Mutua"),
        new FleetVehicle("KDA091H", "Toyota",           "Dyna",           "D011", "Alice Nyambura"),
        new FleetVehicle("KCZ447D", "Isuzu",            "ELF",            "D012", "Kevin Kiptoo"),
        new FleetVehicle("KDB720J", "UD Trucks",        "Croner",         "D013", "Susan Atieno"),
        new FleetVehicle("KCC365S", "Toyota",           "Hiace Matatu",   "D014", "James Mwaura"),
        new FleetVehicle("KDD584K", "Isuzu",            "FVR",            "D015", "Lucy Wambui"),
        new FleetVehicle("KCG129U", "Hino",             "500",            "D016", "Michael Ochieng"),
        new FleetVehicle("KCH836Y", "Mitsubishi Fuso",  "Canter",         "D017", "Jane Muthoni"),
        new FleetVehicle("KCJ272N", "Isuzu",            "NPR Pro",        "D018", "Patrick Kilonzo"),
        new FleetVehicle("KCK610F", "Scania",           "P280",           "D019", "Christina Adhiambo"),
        new FleetVehicle("KCL495T", "Toyota",           "Coaster",        "D020", "Dennis Rotich"),
        new FleetVehicle("KCM958L", "Isuzu",            "FTS 4x4",        "D021", "Rebecca Nduta"),
        new FleetVehicle("KCN337R", "UD Trucks",        "Kuzer",          "D022", "Simon Njoroge"),
        new FleetVehicle("KCP764H", "Mitsubishi Fuso",  "Rosa",           "D023", "Caroline Wangari"),
        new FleetVehicle("KCQ148M", "Isuzu",            "Giga CXZ",       "D024", "Victor Onyango"),
        new FleetVehicle("KCR521B", "Hino",             "700",            "D025", "Naomi Chelimo")
    );

    /** Real Kenyan highway corridors (approximate waypoints along the actual roads). */
    static final List<Route> ROUTES = List.of(
        new Route("NBO-MSA", "Nairobi", "Mombasa", List.of(
            new double[]{-1.286,36.817}, new double[]{-1.370,36.902}, new double[]{-1.450,36.966},
            new double[]{-1.497,37.050}, new double[]{-1.582,37.235}, new double[]{-1.617,37.455},
            new double[]{-1.900,37.550}, new double[]{-2.007,37.628}, new double[]{-2.414,38.019},
            new double[]{-2.911,38.300}, new double[]{-3.396,38.556}, new double[]{-3.530,38.750},
            new double[]{-3.865,39.470}, new double[]{-4.043,39.668})),
        new Route("NBO-KSM", "Nairobi", "Kisumu", List.of(
            new double[]{-1.286,36.817}, new double[]{-1.112,36.640}, new double[]{-0.717,36.433},
            new double[]{-0.303,36.080}, new double[]{-0.280,35.700}, new double[]{-0.367,35.283},
            new double[]{-0.250,35.100}, new double[]{-0.091,34.768})),
        new Route("NBO-ELD", "Nairobi", "Eldoret", List.of(
            new double[]{-1.286,36.817}, new double[]{-1.112,36.640}, new double[]{-0.717,36.433},
            new double[]{-0.303,36.080}, new double[]{-0.100,35.550}, new double[]{ 0.150,35.420},
            new double[]{ 0.350,35.300}, new double[]{ 0.514,35.270})),
        new Route("NBO-NKR", "Nairobi", "Nakuru", List.of(
            new double[]{-1.286,36.817}, new double[]{-1.112,36.640}, new double[]{-0.900,36.550},
            new double[]{-0.717,36.433}, new double[]{-0.520,36.320}, new double[]{-0.303,36.080})),
        new Route("NBO-GSA", "Nairobi", "Garissa", List.of(
            new double[]{-1.286,36.817}, new double[]{-1.233,37.100}, new double[]{-1.000,37.500},
            new double[]{-0.700,38.300}, new double[]{-0.453,39.646}))
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

            ScheduledExecutorService pool = Executors.newScheduledThreadPool(Math.min(size, 16));
            for (int i = 0; i < size; i++) {
                FleetVehicle v = MANIFEST.get(i);
                Route route = ROUTES.get(i % ROUTES.size());     // spread the fleet across corridors
                double startFraction = (double) i / size + rnd.nextDouble() * 0.08;  // scatter along corridor
                boolean reverse = (i % 2) == 1;                                       // mixed directions
                var sim = new VehicleSimulator(v.plate(), v.make(), v.model(),
                        v.driverId(), v.driverName(), route, Math.min(0.95, startFraction),
                        reverse, props, client, rnd);
                pool.scheduleAtFixedRate(sim::tick, rnd.nextInt(props.intervalMs()),
                        props.intervalMs(), TimeUnit.MILLISECONDS);
            }
            log.info("🚚 Simulating {} vehicles on {} highway corridors -> {} every {}ms",
                    size, ROUTES.size(), props.broker(), props.intervalMs());
            new CountDownLatch(1).await();
        };
    }
}
