package io.fleetpulse.telemetry.api;

import io.fleetpulse.telemetry.domain.VehicleEntity;
import io.fleetpulse.telemetry.domain.VehicleJpaRepository;
import io.fleetpulse.telemetry.vehicle.VehicleRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class SettingsController {

    private final VehicleJpaRepository vehicles;
    private final VehicleRegistry registry;
    private final VehicleMapper mapper;

    public SettingsController(VehicleJpaRepository vehicles, VehicleRegistry registry, VehicleMapper mapper) {
        this.vehicles = vehicles;
        this.registry = registry;
        this.mapper = mapper;
    }

    /** Session card on the Settings page — username + roles of the caller. */
    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        return Map.of(
                "username", auth.getName(),
                "roles", auth.getAuthorities().stream()
                        .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                        .sorted().toList());
    }

    @GetMapping("/settings/speed-limits")
    public List<VehicleDto> speedLimits() {
        return mapper.toDtoList(vehicles.findAll());
    }

    /** Non-GET under /api/v1/** → ADMIN / FLEET_MANAGER only (SecurityConfig). */
    @PutMapping("/settings/speed-limits/{vehicleId}")
    public VehicleDto setSpeedLimit(@PathVariable String vehicleId,
                                    @RequestBody Map<String, Double> body) {
        Double limit = body.get("speedLimitKph");
        if (limit == null || limit < 20 || limit > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "speedLimitKph must be between 20 and 200");
        }
        VehicleEntity entity = vehicles.findById(vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown vehicle"));
        registry.updateSpeedLimit(vehicleId, limit);   // persists + refreshes rule-engine cache
        entity.setSpeedLimitKph(limit);
        return mapper.toDto(entity);
    }
}
