package io.fleetpulse.telemetry.api;

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

    /** Register a vehicle. It appears across the platform immediately (no telemetry yet —
     *  it goes live when the simulator/registry starts broadcasting for it). */
    @PostMapping("/vehicles")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleDto addVehicle(@RequestBody Map<String, String> body) {
        String plate = body.getOrDefault("vehicleId", "").trim().toUpperCase();
        String make  = trimOrNull(body.get("make"));
        String model = trimOrNull(body.get("model"));
        String yearS = trimOrNull(body.get("year"));
        String vin   = trimOrNull(body.get("vin"));

        if (!plate.matches("[A-Z]{3}\\d{3}[A-Z]"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Registration must be a Kenyan plate: K + 2 letters + 3 digits + letter (e.g. KDA482X)");
        if (vehicles.existsById(plate))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A vehicle with this registration already exists");

        VehicleEntity v = VehicleEntity.builder()
                .vehicleId(plate)
                .plate(plate)
                .make(make)
                .model(model)
                .speedLimitKph(100)
                .registeredAt(Instant.now())
                .build();
        if (yearS != null) {
            try {
                int y = Integer.parseInt(yearS);
                if (y < 1990 || y > 2100) throw new NumberFormatException();
                v.setYear(y);
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Year must be 1990-2100");
            }
        }
        if (vin != null) {
            if (vin.length() < 6 || vin.length() > 24)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VIN must be 6-24 characters");
            v.setVin(vin);
        }
        return mapper.toDto(vehicles.save(v));
    }

    /** Edit vehicle attributes. Identity (plate, make, model) is immutable here —
     *  the simulator broadcasts them; full CRUD belongs to vehicle-service. */
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
            v.setSpeedLimitKph(n.doubleValue());
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

    private String trimOrNull(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }
}
