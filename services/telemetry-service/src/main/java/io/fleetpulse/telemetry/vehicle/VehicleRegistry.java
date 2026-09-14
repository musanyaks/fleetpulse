package io.fleetpulse.telemetry.vehicle;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VehicleRegistry {

    private final JdbcTemplate jdbc;
    private final Map<String, VehicleProfile> cache = new ConcurrentHashMap<>();

    public VehicleRegistry(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public VehicleProfile profileFor(String vehicleId) {
        return cache.computeIfAbsent(vehicleId, id -> {
            jdbc.update("""
                    INSERT INTO vehicles (vehicle_id, plate, speed_limit_kph)
                    VALUES (?, ?, ?)
                    ON CONFLICT (vehicle_id) DO NOTHING
                    """, id, id, 100.0);
            return jdbc.queryForObject(
                    "SELECT vehicle_id, speed_limit_kph FROM vehicles WHERE vehicle_id = ?",
                    (rs, i) -> new VehicleProfile(rs.getString("vehicle_id"), rs.getDouble("speed_limit_kph")),
                    id);
        });
    }
}