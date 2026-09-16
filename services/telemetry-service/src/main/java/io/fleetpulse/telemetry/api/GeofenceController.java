package io.fleetpulse.telemetry.api;

import io.fleetpulse.common.AlertType;
import io.fleetpulse.telemetry.domain.AlertJpaRepository;
import io.fleetpulse.telemetry.domain.GeofenceEntity;
import io.fleetpulse.telemetry.domain.GeofenceJpaRepository;
import io.fleetpulse.telemetry.geofence.GeofenceEvaluator;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/geofences")
public class GeofenceController {

    private final GeofenceJpaRepository zones;
    private final AlertJpaRepository alerts;
    private final GeofenceMapper mapper;
    private final GeofenceEvaluator evaluator;

    public GeofenceController(GeofenceJpaRepository zones, AlertJpaRepository alerts,
                              GeofenceMapper mapper, GeofenceEvaluator evaluator) {
        this.zones = zones;
        this.alerts = alerts;
        this.mapper = mapper;
        this.evaluator = evaluator;
    }

    @GetMapping
    public List<GeofenceDto> list() {
        return mapper.toDtoList(zones.findAll());
    }

    /** Write path — SecurityConfig restricts non-GET /api/v1/** to ADMIN / FLEET_MANAGER. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GeofenceDto create(@RequestBody GeofenceDto dto) {
        if (dto.name() == null || dto.name().isBlank()
                || dto.lat() == null || dto.lon() == null
                || Math.abs(dto.lat()) > 90 || Math.abs(dto.lon()) > 180
                || dto.radiusM() == null || dto.radiusM() < 50 || dto.radiusM() > 50_000) {
            throw new IllegalArgumentException("Invalid zone payload");
        }
        GeofenceEntity saved = zones.save(GeofenceEntity.builder()
                .name(dto.name()).type(dto.type() == null ? "CUSTOMER" : dto.type())
                .centerLat(dto.lat()).centerLon(dto.lon()).radiusM(dto.radiusM())
                .active(true).createdAt(Instant.now())
                .build());
        evaluator.invalidate();
        return mapper.toDto(saved);
    }

    @GetMapping("/events")
    public List<AlertDto> events() {
        return alerts.findByTypeOrderByTsDesc(AlertType.GEOFENCE, PageRequest.of(0, 100))
                .stream().map(a -> new AlertDto(a.getVehicleId(), a.getType().name(),
                        a.getSeverity().name(), a.getObservedValue(), a.getThreshold(),
                        a.getMessage(), a.getTs()))
                .toList();
    }
}
