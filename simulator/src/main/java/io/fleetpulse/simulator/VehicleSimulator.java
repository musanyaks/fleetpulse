package io.fleetpulse.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

/**
 * Vehicle driving along a Route corridor: position = interpolation over the
 * waypoint list, advanced by distance each tick.
 * Scatter behaviour: each vehicle gets its own start offset, cruise-speed
 * personality, and optional reverse direction - so the fleet spreads along
 * the corridor instead of clumping.
 */
public final class VehicleSimulator {

    private static final double TICK_SECONDS = 2.0;

    private final String vehicleId;
    private final String make;
    private final String model;
    private final String driverId;
    private final String driverName;
    private final Route route;
    private final SimulatorProperties props;
    private final MqttAsyncClient client;
    private final Random rnd;
    private final ObjectMapper mapper = new ObjectMapper();

    private final double cruiseFloor;    // per-vehicle speed personality
    private final double cruiseCeil;
    private double speed = 0;
    private double engineTemp;
    private double fuelLevel;
    private double odometer;
    private double distanceKm;          // position along the corridor
    private int direction = 1;          // +1 outbound, -1 returning
    private int speedingTicks;
    private int overheatTicks;
    private int refuelTicks;
    private boolean warmedUp = false;

    public VehicleSimulator(String vehicleId, String make, String model,
                            String driverId, String driverName, Route route,
                            double startFraction, boolean reverse,
                            SimulatorProperties props, MqttAsyncClient client, Random rnd) {
        this.vehicleId = vehicleId;
        this.make = make;
        this.model = model;
        this.driverId = driverId;
        this.driverName = driverName;
        this.route = route;
        this.props = props;
        this.client = client;
        this.rnd = rnd;
        this.distanceKm = startFraction * route.totalKm();   // spread along corridor
        if (reverse) { this.direction = -1; }
        this.cruiseFloor = 38 + rnd.nextDouble() * 22;       // 38-60
        this.cruiseCeil  = cruiseFloor + 25 + rnd.nextDouble() * 25;  // up to 110
        this.engineTemp = 82 + rnd.nextDouble() * 8;
        this.fuelLevel = 15 + rnd.nextDouble() * 80;
        this.odometer = 10_000 + rnd.nextDouble() * 150_000;
    }

    public void tick() {
        try {
            stepBehaviour();
            double km = speed * TICK_SECONDS / 3600.0;
            advanceAlongRoute(km);
            fuelLevel = Math.max(0, fuelLevel - km * 0.09 * (1 + speed / 200));
            odometer += km;
            publish();
        } catch (Exception ignored) {
            // never let an exception cancel the scheduled task
        }
    }

    private void stepBehaviour() {
        if (!warmedUp) {
            speed = cruiseFloor + rnd.nextDouble() * (cruiseCeil - cruiseFloor);
            warmedUp = true;
            return;
        }
        if (speedingTicks == 0 && rnd.nextDouble() < 0.02) speedingTicks = 3 + rnd.nextInt(6);
        double target = speedingTicks > 0 ? cruiseCeil + 20 + rnd.nextDouble() * 30
                                          : cruiseFloor + rnd.nextDouble() * (cruiseCeil - cruiseFloor);
        speed += Math.max(-12, Math.min(12, target - speed));
        if (speedingTicks > 0) speedingTicks--;
        if (rnd.nextDouble() < 0.03) speed = 0;
        if (refuelTicks == 0 && fuelLevel < 55 && rnd.nextDouble() < 0.003) refuelTicks = 8;
        if (refuelTicks > 0) {
            speed = 0;
            fuelLevel = Math.min(96, fuelLevel + 11);
            refuelTicks--;
        }              // red light / jam

        if (overheatTicks == 0 && rnd.nextDouble() < 0.004) overheatTicks = 10 + rnd.nextInt(10);
        double drift = (speed > 105 ? 0.5 : speed > 1 ? 0.05 : -0.15)
                     + (rnd.nextDouble() - 0.5) * 0.6;
        engineTemp = Math.max(75, Math.min(99, engineTemp + drift));
        if (overheatTicks > 0) { engineTemp += 1.4; overheatTicks--; }
    }

    private void advanceAlongRoute(double km) {
        if (km <= 0) return;
        distanceKm += direction * km;
        if (distanceKm >= route.totalKm()) {          // reached end -> turn around
            distanceKm = route.totalKm();
            direction = -1;
        } else if (distanceKm <= 0) {                  // reached start -> head out again
            distanceKm = 0;
            direction = 1;
        }
    }

    private void publish() throws Exception {
        double[] ll = route.positionAtKm(Math.max(0, Math.min(route.totalKm(), distanceKm)));

        ObjectNode json = mapper.createObjectNode();
        json.put("vehicleId", vehicleId);
        json.put("timestamp", Instant.now().truncatedTo(ChronoUnit.MILLIS).toString());
        json.put("latitude", round(ll[0], 6));
        json.put("longitude", round(ll[1], 6));
        json.put("speedKph", round(speed, 1));
        json.put("engineTempC", round(engineTemp, 1));
        json.put("fuelLevelPct", round(fuelLevel, 1));
        json.put("rpm", Math.round(speed * 18 + (speed > 1 ? 750 : 0)));
        json.put("odometerKm", round(odometer, 1));
        json.put("ignition", speed > 0 ? "ON" : "OFF");
        json.put("make", make);
        json.put("model", model);
        json.put("driverId", driverId);
        json.put("driverName", driverName);
        json.put("routeId", route.id());
        json.put("batteryVoltage", round((speed > 0 ? 26.8 : 24.3) + rnd.nextDouble() * 0.6, 1));

        MqttMessage message = new MqttMessage(mapper.writeValueAsBytes(json));
        message.setQos(0);
        client.publish("fleet/" + vehicleId + "/telemetry", message);
    }

    private static double round(double v, int dp) {
        double f = Math.pow(10, dp);
        return Math.round(v * f) / f;
    }
}
