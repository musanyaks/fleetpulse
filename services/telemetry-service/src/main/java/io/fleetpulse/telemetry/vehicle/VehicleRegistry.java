package io.fleetpulse.telemetry.vehicle;

import io.fleetpulse.common.TelemetryMessage;
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

    public VehicleProfile profileFor(TelemetryMessage m) {
        return cache.computeIfAbsent(m.vehicleId(), id -> {
            jdbc.update("""
                    INSERT INTO vehicles (vehicle_id, plate, make, model, speed_limit_kph)
                    VALUES (?, ?, ?, ?, 100.0)
                    ON CONFLICT (vehicle_id) DO UPDATE
                      SET make  = COALESCE(vehicles.make,  EXCLUDED.make),
                          model = COALESCE(vehicles.model, EXCLUDED.model)
                    """, id, id, m.make(), m.model());
            return jdbc.queryForObject(
                    "SELECT vehicle_id, speed_limit_kph FROM vehicles WHERE vehicle_id = ?",
                    (rs, i) -> new VehicleProfile(rs.getString("vehicle_id"), rs.getDouble("speed_limit_kph")),
                    id);
        });
    }
}
