package io.fleetpulse.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

public final class VehicleSimulator {

    private static final double TICK_SECONDS = 2.0;
    private static final double KM_PER_DEG_LAT = 111.32;

    private final String vehicleId;
    private final String make;
    private final String model;
    private final SimulatorProperties props;
    private final MqttAsyncClient client;
    private final Random rnd;
    private final ObjectMapper mapper = new ObjectMapper();

    private double lat, lon, heading;
    private double speed;         // km/h
    private double engineTemp;    // °C
    private double fuelLevel;     // %
    private double odometer;      // km
    private int speedingTicks;
    private int overheatTicks;

    public VehicleSimulator(String vehicleId, String make, String model,
                            SimulatorProperties props, MqttAsyncClient client, Random rnd) {
        this.vehicleId = vehicleId;
        this.make = make;
        this.model = model;
        this.props = props;
        this.client = client;
        this.rnd = rnd;
        this.lat = props.baseLat() + (rnd.nextDouble() - 0.5) * 0.8;
        this.lon = props.baseLon() + (rnd.nextDouble() - 0.5) * 0.8;
        this.heading = rnd.nextDouble() * 360;
        this.speed = 40 + rnd.nextDouble() * 40;
        this.engineTemp = 82 + rnd.nextDouble() * 8;
        this.fuelLevel = 35 + rnd.nextDouble() * 60;
        this.odometer = 10_000 + rnd.nextDouble() * 150_000;
    }

    public void tick() {
        try {
            stepBehaviour();
            double km = speed * TICK_SECONDS / 3600.0;
            advancePosition(km);
            fuelLevel = Math.max(0, fuelLevel - km * 0.09 * (1 + speed / 200));
            odometer += km;
            publish();
        } catch (Exception ignored) {
            // never let an exception cancel the scheduled task
        }
    }

    private void stepBehaviour() {
        if (speedingTicks == 0 && rnd.nextDouble() < 0.02) speedingTicks = 3 + rnd.nextInt(6);
        double target = speedingTicks > 0 ? 115 + rnd.nextDouble() * 45 : 45 + rnd.nextDouble() * 50;
        speed += Math.max(-12, Math.min(12, target - speed));
        if (speedingTicks > 0) speedingTicks--;
        if (rnd.nextDouble() < 0.03) speed = 0;                       // red light / jam

        if (overheatTicks == 0 && rnd.nextDouble() < 0.004) overheatTicks = 10 + rnd.nextInt(10);
        double drift = (speed > 105 ? 0.5 : speed > 1 ? 0.05 : -0.15)
                     + (rnd.nextDouble() - 0.5) * 0.6;
        engineTemp = Math.max(75, Math.min(99, engineTemp + drift));
        if (overheatTicks > 0) { engineTemp += 1.4; overheatTicks--; }
    }

    private void advancePosition(double km) {
        if (speed < 1) return;
        heading = (heading + (rnd.nextDouble() - 0.5) * 20 + 360) % 360;
        double rad = Math.toRadians(heading);
        lat  += (km * Math.cos(rad)) / KM_PER_DEG_LAT;
        lon  += (km * Math.sin(rad)) / (KM_PER_DEG_LAT * Math.cos(Math.toRadians(lat)));
    }

    private void publish() throws Exception {
        ObjectNode json = mapper.createObjectNode();
        json.put("vehicleId", vehicleId);
        json.put("timestamp", Instant.now().truncatedTo(ChronoUnit.MILLIS).toString());
        json.put("latitude", round(lat, 6));
        json.put("longitude", round(lon, 6));
        json.put("speedKph", round(speed, 1));
        json.put("engineTempC", round(engineTemp, 1));
        json.put("fuelLevelPct", round(fuelLevel, 1));
        json.put("rpm", Math.round(speed * 18 + (speed > 1 ? 750 : 0)));
        json.put("odometerKm", round(odometer, 1));
        json.put("ignition", speed > 0 ? "ON" : "OFF");
        json.put("make", make);
        json.put("model", model);

        MqttMessage message = new MqttMessage(mapper.writeValueAsBytes(json));
        message.setQos(0);
        client.publish("fleet/" + vehicleId + "/telemetry", message);
    }

    private static double round(double v, int dp) {
        double f = Math.pow(10, dp);
        return Math.round(v * f) / f;
    }
}
