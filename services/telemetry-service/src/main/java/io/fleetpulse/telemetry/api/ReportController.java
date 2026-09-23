package io.fleetpulse.telemetry.api;

import io.fleetpulse.telemetry.live.LiveVehicleState;
import io.fleetpulse.telemetry.live.LiveVehicleStateService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Supplier;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced fleet reports — heavy aggregation pushed into TimescaleDB.
 * Results are cached 60 s per (report, range): the dashboard polls these
 * every few seconds, and the window-function scans are too heavy for that.
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private static final ZoneId FLEET_TZ = ZoneId.of("Africa/Nairobi");
    private static final long TTL_MS = 60_000;

    private record Cached(Map<String, Object> payload, long at) {}
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private final JdbcTemplate jdbc;
    private final LiveVehicleStateService liveState;

    public ReportController(JdbcTemplate jdbc, LiveVehicleStateService liveState) {
        this.jdbc = jdbc;
        this.liveState = liveState;
    }

    private Map<String, Object> cached(String key, Supplier<Map<String, Object>> compute) {
        Cached hit = cache.get(key);
        if (hit != null && System.currentTimeMillis() - hit.at() < TTL_MS) return hit.payload();
        Map<String, Object> payload = compute.get();
        cache.put(key, new Cached(payload, System.currentTimeMillis()));
        return payload;
    }

    private record Range(Instant start, Instant end, LocalDate from, LocalDate to) {}

    private Range range(String from, String to) {
        LocalDate f, t;
        try {
            f = (from == null || from.isBlank()) ? LocalDate.now(FLEET_TZ).minusDays(6) : LocalDate.parse(from);
            t = (to == null || to.isBlank()) ? LocalDate.now(FLEET_TZ) : LocalDate.parse(to);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dates must be YYYY-MM-DD");
        }
        if (f.isAfter(t)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be <= to");
        LocalDate today = LocalDate.now(FLEET_TZ);
        if (t.isAfter(today)) t = today;
        if (f.isAfter(today)) f = today;
        return new Range(f.atStartOfDay(FLEET_TZ).toInstant(),
                         t.plusDays(1).atStartOfDay(FLEET_TZ).toInstant(), f, t);
    }

    private Map<String, Object> envelope(Range r, String report, List<Map<String, Object>> rows) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("report", report);
        m.put("from", r.from().toString());
        m.put("to", r.to().toString());
        m.put("rows", rows);
        return m;
    }

    private static final String PTS_CTE = """
        WITH pts AS (
            SELECT vehicle_id, ts, speed_kph, engine_temp_c, fuel_level_pct, latitude, longitude,
                   lag(latitude)  OVER w AS prev_lat,
                   lag(longitude) OVER w AS prev_lon
            FROM telemetry_readings
            WHERE ts >= '%s'::timestamptz AND ts < '%s'::timestamptz
            WINDOW w AS (PARTITION BY vehicle_id ORDER BY ts)
        )
        """;

    private static final String DISTANCE_EXPR = """
        COALESCE(round(((sum(
            6371000 * 2 * asin(sqrt(
                power(sin(radians(latitude - prev_lat) / 2), 2) +
                cos(radians(prev_lat)) * cos(radians(latitude)) *
                power(sin(radians(longitude - prev_lon) / 2), 2)
            ))
        ) FILTER (WHERE prev_lat IS NOT NULL AND speed_kph >= 1)) / 1000)::numeric, 1), 0)
        """;

    /** 1. Fleet utilization. */
    @GetMapping("/utilization")
    public Map<String, Object> utilization(@RequestParam(required = false) String from,
                                           @RequestParam(required = false) String to) {
        Range r = range(from, to);
        return cached("utilization|" + r.from() + "|" + r.to(),
            () -> envelope(r, "utilization", jdbc.queryForList("""
            %s
            SELECT p.vehicle_id,
                   trim(coalesce(v.make || ' ' || v.model, '')) AS vehicle,
                   count(*) AS samples,
                   count(*) FILTER (WHERE p.speed_kph >= 1) AS moving_samples,
                   round(100.0 * count(*) FILTER (WHERE p.speed_kph >= 1) / greatest(count(*), 1), 1)
                       AS utilization_pct,
                   %s AS distance_km,
                   COALESCE(round((avg(p.speed_kph) FILTER (WHERE p.speed_kph >= 1))::numeric, 1), 0)
                       AS avg_speed_kph,
                   round(max(p.speed_kph)::numeric, 1) AS max_speed_kph,
                   COALESCE(round((avg(p.engine_temp_c))::numeric, 1), 0) AS avg_engine_c
            FROM pts p LEFT JOIN vehicles v USING (vehicle_id)
            GROUP BY p.vehicle_id, v.make, v.model
            ORDER BY distance_km DESC
            """.formatted(PTS_CTE.formatted(r.start(), r.end()), DISTANCE_EXPR))));
    }

    /** 2. Driver performance. */
    @GetMapping("/drivers")
    public Map<String, Object> drivers(@RequestParam(required = false) String from,
                                       @RequestParam(required = false) String to) {
        Range r = range(from, to);
        return cached("drivers|" + r.from() + "|" + r.to(), () -> {
            List<Map<String, Object>> alertRows = jdbc.queryForList("""
                SELECT vehicle_id,
                       count(*) FILTER (WHERE type = 'SPEEDING') AS speeding,
                       count(*) FILTER (WHERE type = 'HARSH_BRAKING') AS harsh_braking,
                       count(*) FILTER (WHERE type = 'HARSH_ACCELERATION') AS harsh_accel,
                       count(*) FILTER (WHERE severity = 'CRITICAL') AS critical
                FROM alerts
                WHERE ts >= '%s'::timestamptz AND ts < '%s'::timestamptz
                GROUP BY vehicle_id
                """.formatted(r.start(), r.end()));

            List<Map<String, Object>> distRows = jdbc.queryForList("""
                %s
                SELECT vehicle_id, %s AS distance_km
                FROM pts GROUP BY vehicle_id
                """.formatted(PTS_CTE.formatted(r.start(), r.end()), DISTANCE_EXPR));

            Map<String, Map<String, Object>> alertsByVehicle = new HashMap<>();
            alertRows.forEach(m -> alertsByVehicle.put(String.valueOf(m.get("vehicle_id")), m));
            Map<String, Double> distByVehicle = new HashMap<>();
            distRows.forEach(m -> distByVehicle.put(String.valueOf(m.get("vehicle_id")),
                    ((Number) m.get("distance_km")).doubleValue()));

            Map<String, String> nameByDriver = new LinkedHashMap<>();
            Map<String, List<String>> platesByDriver = new LinkedHashMap<>();
            for (LiveVehicleState v : liveState.snapshot()) {
                if (v.driverId() == null) continue;
                nameByDriver.putIfAbsent(v.driverId(), v.driverName() != null ? v.driverName() : v.driverId());
                platesByDriver.computeIfAbsent(v.driverId(), k -> new ArrayList<>()).add(v.vehicleId());
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            nameByDriver.forEach((driverId, name) -> {
                long speeding = 0, braking = 0, accel = 0, critical = 0;
                double distance = 0;
                for (String plate : platesByDriver.get(driverId)) {
                    Map<String, Object> a = alertsByVehicle.get(plate);
                    if (a != null) {
                        speeding += ((Number) a.getOrDefault("speeding", 0)).longValue();
                        braking  += ((Number) a.getOrDefault("harsh_braking", 0)).longValue();
                        accel    += ((Number) a.getOrDefault("harsh_accel", 0)).longValue();
                        critical += ((Number) a.getOrDefault("critical", 0)).longValue();
                    }
                    distance += distByVehicle.getOrDefault(plate, 0.0);
                }
                double score = Math.max(40, 100
                        - Math.min(40, speeding * 4)
                        - Math.min(24, braking * 3)
                        - Math.min(16, accel * 2));
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("driver_id", driverId);
                row.put("driver", name);
                row.put("vehicles", String.join(", ", platesByDriver.get(driverId)));
                row.put("distance_km", distance);
                row.put("score", score);
                row.put("speeding", speeding);
                row.put("harsh_braking", braking);
                row.put("harsh_accel", accel);
                row.put("critical", critical);
                rows.add(row);
            });
            rows.sort(Comparator.comparingDouble(m -> -((Number) m.get("score")).doubleValue()));
            return envelope(r, "drivers", rows);
        });
    }

    /** 3. Fuel: first()/last() + distance. */
    @GetMapping("/fuel")
    public Map<String, Object> fuel(@RequestParam(required = false) String from,
                                    @RequestParam(required = false) String to) {
        Range r = range(from, to);
        return cached("fuel|" + r.from() + "|" + r.to(),
            () -> envelope(r, "fuel", jdbc.queryForList("""
            %s,
            fuel AS (
                SELECT vehicle_id,
                       first(fuel_level_pct, ts) AS fuel_start_pct,
                       last(fuel_level_pct, ts)  AS fuel_end_pct,
                       round(avg(fuel_level_pct)::numeric, 1) AS avg_fuel_pct
                FROM telemetry_readings
                WHERE ts >= '%s'::timestamptz AND ts < '%s'::timestamptz
                  AND fuel_level_pct IS NOT NULL
                GROUP BY vehicle_id
            )
            SELECT f.vehicle_id,
                   trim(coalesce(v.make || ' ' || v.model, '')) AS vehicle,
                   round(f.fuel_start_pct::numeric, 1) AS fuel_start_pct,
                   round(f.fuel_end_pct::numeric, 1) AS fuel_end_pct,
                   f.avg_fuel_pct,
                   round(greatest(f.fuel_start_pct - f.fuel_end_pct, 0)::numeric, 1) AS fuel_used_pct,
                   d.distance_km,
                   CASE WHEN f.fuel_start_pct - f.fuel_end_pct > 0.5
                        THEN round((d.distance_km / (f.fuel_start_pct - f.fuel_end_pct))::numeric, 1)
                        ELSE NULL END AS km_per_fuel_pct
            FROM fuel f
            JOIN (SELECT vehicle_id, %s AS distance_km FROM pts GROUP BY vehicle_id) d USING (vehicle_id)
            LEFT JOIN vehicles v USING (vehicle_id)
            ORDER BY d.distance_km DESC
            """.formatted(PTS_CTE.formatted(r.start(), r.end()),
                          r.start(), r.end(), DISTANCE_EXPR))));
    }

    /** 4. Violations. */
    @GetMapping("/violations")
    public Map<String, Object> violations(@RequestParam(required = false) String from,
                                          @RequestParam(required = false) String to) {
        Range r = range(from, to);
        return cached("violations|" + r.from() + "|" + r.to(),
            () -> envelope(r, "violations", jdbc.queryForList("""
            SELECT vehicle_id, type, severity, count(*) AS events,
                   round(max(observed_value)::numeric, 1) AS max_observed
            FROM alerts
            WHERE ts >= '%s'::timestamptz AND ts < '%s'::timestamptz
            GROUP BY vehicle_id, type, severity
            ORDER BY events DESC, vehicle_id
            """.formatted(r.start(), r.end()))));
    }

    /** 5. Geofence activity. */
    @GetMapping("/geofence")
    public Map<String, Object> geofence(@RequestParam(required = false) String from,
                                        @RequestParam(required = false) String to) {
        Range r = range(from, to);
        return cached("geofence|" + r.from() + "|" + r.to(),
            () -> envelope(r, "geofence", jdbc.queryForList("""
            SELECT vehicle_id,
                   count(*) FILTER (WHERE message LIKE 'Entered%%') AS entries,
                   count(*) FILTER (WHERE message LIKE 'Left%%') AS exits,
                   count(*) FILTER (WHERE message LIKE 'Dwelling%%') AS dwellings,
                   count(*) AS total_events
            FROM alerts
            WHERE type = 'GEOFENCE' AND ts >= '%s'::timestamptz AND ts < '%s'::timestamptz
            GROUP BY vehicle_id
            ORDER BY entries DESC
            """.formatted(r.start(), r.end()))));
    }

    /** 6. Daily summary. */
    @GetMapping("/daily")
    public Map<String, Object> daily(@RequestParam(required = false) String from,
                                     @RequestParam(required = false) String to) {
        Range r = range(from, to);
        return cached("daily|" + r.from() + "|" + r.to(), () -> {
            String tz = "Africa/Nairobi";

            List<Map<String, Object>> distRows = jdbc.queryForList("""
                %s
                SELECT to_char(ts AT TIME ZONE '%s', 'YYYY-MM-DD') AS day,
                       %s AS distance_km
                FROM pts
                WHERE prev_lat IS NOT NULL AND speed_kph >= 1
                GROUP BY day ORDER BY day
                """.formatted(PTS_CTE.formatted(r.start(), r.end()), tz, DISTANCE_EXPR));

            List<Map<String, Object>> vehRows = jdbc.queryForList("""
                SELECT to_char(ts AT TIME ZONE '%s', 'YYYY-MM-DD') AS day,
                       count(DISTINCT vehicle_id) AS active_vehicles
                FROM telemetry_readings
                WHERE ts >= '%s'::timestamptz AND ts < '%s'::timestamptz
                GROUP BY day ORDER BY day
                """.formatted(tz, r.start(), r.end()));

            List<Map<String, Object>> alertRows = jdbc.queryForList("""
                SELECT to_char(ts AT TIME ZONE '%s', 'YYYY-MM-DD') AS day, count(*) AS alerts
                FROM alerts
                WHERE ts >= '%s'::timestamptz AND ts < '%s'::timestamptz
                GROUP BY day ORDER BY day
                """.formatted(tz, r.start(), r.end()));

            LinkedHashMap<String, Map<String, Object>> byDay = new LinkedHashMap<>();
            distRows.forEach(m -> byDay.computeIfAbsent(String.valueOf(m.get("day")), k -> {
                Map<String, Object> n = new LinkedHashMap<>();
                n.put("day", k); n.put("distance_km", 0.0); n.put("active_vehicles", 0); n.put("alerts", 0);
                return n;
            }).put("distance_km", ((Number) m.get("distance_km")).doubleValue()));
            vehRows.forEach(m -> byDay.computeIfAbsent(String.valueOf(m.get("day")), k -> {
                Map<String, Object> n = new LinkedHashMap<>();
                n.put("day", k); n.put("distance_km", 0.0); n.put("active_vehicles", 0); n.put("alerts", 0);
                return n;
            }).put("active_vehicles", ((Number) m.get("active_vehicles")).longValue()));
            alertRows.forEach(m -> byDay.computeIfAbsent(String.valueOf(m.get("day")), k -> {
                Map<String, Object> n = new LinkedHashMap<>();
                n.put("day", k); n.put("distance_km", 0.0); n.put("active_vehicles", 0); n.put("alerts", 0);
                return n;
            }).put("alerts", ((Number) m.get("alerts")).longValue()));

            return envelope(r, "daily", new ArrayList<>(byDay.values()));
        });
    }
}
