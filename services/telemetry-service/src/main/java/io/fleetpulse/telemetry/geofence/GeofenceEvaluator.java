package io.fleetpulse.telemetry.geofence;

import io.fleetpulse.common.AlertMessage;
import io.fleetpulse.common.AlertType;
import io.fleetpulse.common.Severity;
import io.fleetpulse.common.TelemetryMessage;
import io.fleetpulse.telemetry.domain.GeofenceEntity;
import io.fleetpulse.telemetry.domain.GeofenceJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Edge-triggered geofence state machine.
 * Per vehicle+zone: fires ENTER on outside→inside, EXIT on inside→outside,
 * and DWELL once if the vehicle stays inside past the limit.
 * First sighting of a vehicle is silent — no startup alert storm.
 */
@Component
public class GeofenceEvaluator {

    private static final Logger log = LoggerFactory.getLogger(GeofenceEvaluator.class);
    private static final Duration ZONE_REFRESH = Duration.ofSeconds(60);
    private static final Duration DWELL_LIMIT = Duration.ofMinutes(10);

    private record ZoneState(boolean inside, Instant since, boolean dwellFired) {}

    private final GeofenceJpaRepository repo;
    private final Map<String, ZoneState> states = new ConcurrentHashMap<>();
    private volatile List<GeofenceEntity> zoneCache = List.of();
    private volatile Instant cacheLoadedAt = Instant.EPOCH;

    public GeofenceEvaluator(GeofenceJpaRepository repo) {
        this.repo = repo;
    }

    /** Called for every telemetry reading. Returns 0..1 alerts per zone. */
    public List<AlertMessage> evaluate(TelemetryMessage m) {
        refreshIfNeeded();
        List<AlertMessage> out = new ArrayList<>();
        Instant now = Instant.now();

        for (GeofenceEntity z : zoneCache) {
            if (!z.isActive()) continue;

            boolean inside = haversineM(m.latitude(), m.longitude(),
                    z.getCenterLat(), z.getCenterLon()) <= z.getRadiusM();
            String key = m.vehicleId() + "|" + z.getId();
            ZoneState prev = states.get(key);

            if (prev == null) {                       // first sighting — arm silently
                states.put(key, new ZoneState(inside, now, false));
                continue;
            }
            if (inside != prev.inside()) {            // transition → event
                states.put(key, new ZoneState(inside, now, false));
                if (inside) {
                    out.add(alert(m, z, severityOfEnter(z),
                            "Entered %s zone: %s".formatted(z.getType().toLowerCase(), z.getName())));
                } else {
                    out.add(alert(m, z, Severity.INFO,
                            "Left zone: %s".formatted(z.getName())));
                }
                continue;
            }
            if (inside && !prev.dwellFired()
                    && Duration.between(prev.since(), now).compareTo(DWELL_LIMIT) > 0) {
                states.put(key, new ZoneState(true, prev.since(), true));   // one-shot
                out.add(alert(m, z, Severity.WARNING,
                        "Dwelling in %s zone: %s for over %d min".formatted(
                                z.getType().toLowerCase(), z.getName(), DWELL_LIMIT.toMinutes())));
            }
        }
        return out;
    }

    public void invalidate() {                        // called after zone create/update
        cacheLoadedAt = Instant.EPOCH;
    }

    private void refreshIfNeeded() {
        if (Duration.between(cacheLoadedAt, Instant.now()).compareTo(ZONE_REFRESH) < 0) return;
        zoneCache = repo.findAll();
        cacheLoadedAt = Instant.now();
        log.debug("Geofence zone cache refreshed: {} zones", zoneCache.size());
    }

    private Severity severityOfEnter(GeofenceEntity z) {
        return switch (z.getType()) {
            case "RESTRICTED", "DANGER" -> Severity.CRITICAL;
            case "DEPOT", "CUSTOMER" -> Severity.INFO;
            default -> Severity.INFO;
        };
    }

    private AlertMessage alert(TelemetryMessage m, GeofenceEntity z, Severity severity, String message) {
        return new AlertMessage(m.vehicleId(), AlertType.GEOFENCE, severity,
                m.speedKph(), z.getRadiusM(), message, m.timestamp());
    }

    private static double haversineM(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6_371_000, r = Math.PI / 180;
        double dLa = (lat2 - lat1) * r, dLo = (lon2 - lon1) * r;
        double h = Math.sin(dLa / 2) * Math.sin(dLa / 2)
                 + Math.cos(lat1 * r) * Math.cos(lat2 * r) * Math.sin(dLo / 2) * Math.sin(dLo / 2);
        return 2 * R * Math.asin(Math.sqrt(h));
    }
}
