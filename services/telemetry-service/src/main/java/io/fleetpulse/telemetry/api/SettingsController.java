package io.fleetpulse.telemetry.api;

import io.fleetpulse.telemetry.domain.DriverEntity;
import io.fleetpulse.telemetry.domain.DriverJpaRepository;
import io.fleetpulse.telemetry.domain.VehicleEntity;
import io.fleetpulse.telemetry.domain.VehicleJpaRepository;
import io.fleetpulse.telemetry.vehicle.VehicleRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class SettingsController {

    private final VehicleJpaRepository vehicles;
    private final VehicleRegistry registry;
    private final VehicleMapper mapper;
    private final DriverJpaRepository driversRepo;

    public SettingsController(VehicleJpaRepository vehicles, VehicleRegistry registry,
                              VehicleMapper mapper, DriverJpaRepository driversRepo) {
        this.vehicles = vehicles;
        this.registry = registry;
        this.mapper = mapper;
        this.driversRepo = driversRepo;
    }

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

    @PutMapping("/settings/speed-limits/{vehicleId}")
    public VehicleDto setSpeedLimit(@PathVariable String vehicleId,
                                    @RequestBody Map<String, Double> body) {
        Double limit = body.get("speedLimitKph");
        if (limit == null || limit < 20 || limit > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "speedLimitKph must be between 20 and 200");
        }
        VehicleEntity entity = vehicles.findById(vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown vehicle"));
        registry.updateSpeedLimit(vehicleId, limit);
        entity.setSpeedLimitKph(limit);
        return mapper.toDto(entity);
    }

    /** Register a driver. Appears in the roster immediately; telemetry assignment
     *  happens when the simulator manifest (or future dispatch) links a vehicle. */
    @PostMapping("/drivers")
    @ResponseStatus(HttpStatus.CREATED)
    public DriverEntity addDriver(@RequestBody Map<String, String> body) {
        String id   = body.getOrDefault("driverId", "").trim().toUpperCase();
        String name = body.getOrDefault("name", "").trim();
        if (!id.matches("D\\d{3,6}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "driverId must be D + digits (e.g. D026)");
        if (name.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        if (driversRepo.existsById(id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Driver ID already exists");
        return driversRepo.save(DriverEntity.builder()
                .driverId(id).name(name)
                .phone(trimOrNull(body.get("phone")))
                .email(trimOrNull(body.get("email")))
                .branch(trimOrNull(body.get("branch")))
                .licenseExp(trimOrNull(body.get("licenseExp")))
                .createdAt(Instant.now())
                .build());
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }
}
