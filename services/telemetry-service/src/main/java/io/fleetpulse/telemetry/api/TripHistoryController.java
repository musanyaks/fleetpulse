package io.fleetpulse.telemetry.api;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Day-level trip aggregates from TimescaleDB.
 * "A day" means a NAIROBI calendar day (the fleet's business timezone) —
 * boundaries are computed in Java and passed as UTC instants, so container/DB
 * timezones are irrelevant. A "departure" = stopped -> moving transition.
 * A 1h baseline before the day start catches midnight-spanning transitions.
 */
@RestController
@RequestMapping("/api/v1/trips")
public class TripHistoryController {

    private static final ZoneId FLEET_TZ = ZoneId.of("Africa/Nairobi");

    private final JdbcTemplate jdbc;

    public TripHistoryController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** /summary?date=YYYY-MM-DD — omit for today (today = Nairobi's date). */
    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestParam(required = false) String date) {
        LocalDate day;
        try {
            day = (date == null || date.isBlank()) ? LocalDate.now(FLEET_TZ) : LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date must be YYYY-MM-DD");
        }
        if (day.isAfter(LocalDate.now(FLEET_TZ))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date cannot be in the future");
        }

        Instant start    = day.atStartOfDay(FLEET_TZ).toInstant();          // 00:00 EAT
        Instant end      = day.plusDays(1).atStartOfDay(FLEET_TZ).toInstant();
        Instant baseline = start.minus(Duration.ofHours(1));

        Integer departures = jdbc.queryForObject("""
                WITH ordered AS (
                    SELECT vehicle_id, ts,
                           (speed_kph < 1) AS stopped,
                           lag(speed_kph < 1) OVER (PARTITION BY vehicle_id ORDER BY ts) AS prev_stopped
                    FROM telemetry_readings
                    WHERE ts >= '%s'::timestamptz AND ts < '%s'::timestamptz
                )
                SELECT count(*) FROM ordered
                WHERE stopped = false AND prev_stopped = true AND ts >= '%s'::timestamptz
                """.formatted(baseline, end, start), Integer.class);

        Double distanceKm = jdbc.queryForObject("""
                SELECT COALESCE(sum(
                    6371000 * 2 * asin(sqrt(
                        power(sin(radians(latitude - prev_lat) / 2), 2) +
                        cos(radians(prev_lat)) * cos(radians(latitude)) *
                        power(sin(radians(longitude - prev_lon) / 2), 2)
                    ))
                ) / 1000, 0)
                FROM (
                    SELECT ts, latitude, longitude,
                           lag(latitude)  OVER (PARTITION BY vehicle_id ORDER BY ts) AS prev_lat,
                           lag(longitude) OVER (PARTITION BY vehicle_id ORDER BY ts) AS prev_lon
                    FROM telemetry_readings
                    WHERE ts >= '%s'::timestamptz AND ts < '%s'::timestamptz
                ) t
                WHERE ts >= '%s'::timestamptz AND prev_lat IS NOT NULL
                """.formatted(baseline, end, start), Double.class);

        Long vehicles = jdbc.queryForObject(
                "SELECT count(DISTINCT vehicle_id) FROM telemetry_readings " +
                "WHERE ts >= '%s'::timestamptz AND ts < '%s'::timestamptz"
                        .formatted(start, end), Long.class);

        return Map.of(
                "date", day.toString(),
                "isToday", day.equals(LocalDate.now(FLEET_TZ)),
                "departures", departures != null ? departures : 0,
                "distanceKm", distanceKm != null ? Math.round(distanceKm * 10) / 10.0 : 0,
                "vehiclesActive", vehicles != null ? vehicles : 0);
    }

    /** Backward-compatible alias. */
    @GetMapping("/today")
    public Map<String, Object> today() {
        return summary(null);
    }
}
