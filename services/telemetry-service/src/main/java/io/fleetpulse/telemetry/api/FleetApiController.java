package io.fleetpulse.telemetry.api;

import io.fleetpulse.telemetry.domain.AlertJpaRepository;
import io.fleetpulse.telemetry.domain.VehicleJpaRepository;
import io.fleetpulse.telemetry.live.LiveVehicleState;
import io.fleetpulse.telemetry.live.LiveVehicleStateService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class FleetApiController {

    private final LiveVehicleStateService liveState;
    private final AlertJpaRepository alerts;
    private final VehicleJpaRepository vehicles;
    private final AlertMapper alertMapper;
    private final VehicleMapper vehicleMapper;

    public FleetApiController(LiveVehicleStateService liveState,
                              AlertJpaRepository alerts, VehicleJpaRepository vehicles,
                              AlertMapper alertMapper, VehicleMapper vehicleMapper) {
        this.liveState = liveState;
        this.alerts = alerts;
        this.vehicles = vehicles;
        this.alertMapper = alertMapper;
        this.vehicleMapper = vehicleMapper;
    }

    @GetMapping("/vehicles/live")
    public Collection<LiveVehicleState> live() {
        return liveState.snapshot();
    }

    @GetMapping("/vehicles/summary")
    public Map<String, Long> summary() {
        return liveState.summary();
    }

    @GetMapping("/vehicles")
    public List<VehicleDto> vehicles(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "50") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 200), Sort.by("vehicleId"));
        return vehicleMapper.toDtoList(vehicles.findAll(pageable).getContent());
    }

    @GetMapping("/alerts/recent")
    public List<AlertDto> recentAlerts(@RequestParam(defaultValue = "50") int limit) {
        var pageable = PageRequest.of(0, Math.min(limit, 500));
        return alertMapper.toDtoList(alerts.findAllByOrderByTsDesc(pageable));
    }
}
