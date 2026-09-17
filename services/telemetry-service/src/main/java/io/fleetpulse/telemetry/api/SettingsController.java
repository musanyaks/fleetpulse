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

    /**
     * Edit vehicle attributes. Identity fields (plate, make, model) are immutable here —
     * they define what the simulator broadcasts; full CRUD belongs to vehicle-service.
     * Driver reassignment also lives there: the simulator owns the broadcast manifest.
     */
    @PutMapping("/vehicles/{vehicleId}")
    public VehicleDto updateVehicle(@PathVariable String vehicleId,
                                    @RequestBody Map<String, Object> body) {
        VehicleEntity v = vehicles.findById(vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown vehicle"));

        if (body.containsKey("speedLimitKph")) {
            Object lim = body.get("speedLimitKph");
            if (!(lim instanceof Number n) || n.doubleValue() < 20 || n.doubleValue() > 200)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "speedLimitKph must be 20-200");
            registry.updateSpeedLimit(vehicleId, n.doubleValue());
            v.setSpeedLimitKph(n.doubleValue());   // keep entity in sync — flush would else revert it
        }
        if (body.containsKey("year")) {
            Object y = body.get("year");
            if (!(y instanceof Number n) || n.intValue() < 1990 || n.intValue() > 2100)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "year out of range");
            v.setYear(n.intValue());
        }
        for (String f : List.of("vin", "lastService", "nextService", "insuranceExpiry", "inspectionExpiry")) {
            if (!body.containsKey(f)) continue;
            Object val = body.get(f);
            String s = val == null ? null : val.toString().trim();
            if (s != null && s.isEmpty()) s = null;
            switch (f) {
                case "vin" -> v.setVin(s);
                case "lastService" -> v.setLastService(s);
                case "nextService" -> v.setNextService(s);
                case "insuranceExpiry" -> v.setInsuranceExpiry(s);
                case "inspectionExpiry" -> v.setInspectionExpiry(s);
            }
        }
        vehicles.save(v);
        return mapper.toDto(v);
    }
}
