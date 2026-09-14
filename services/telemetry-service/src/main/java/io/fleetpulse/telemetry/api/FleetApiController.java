package io.fleetpulse.telemetry.api;

import io.fleetpulse.telemetry.live.LiveVehicleState;
import io.fleetpulse.telemetry.live.LiveVehicleStateService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class FleetApiController {

    private final LiveVehicleStateService liveState;
    private final JdbcTemplate jdbc;

    public FleetApiController(LiveVehicleStateService liveState, JdbcTemplate jdbc) {
        this.liveState = liveState;
        this.jdbc = jdbc;
    }

    @GetMapping("/vehicles/live")
    public Collection<LiveVehicleState> live() {
        return liveState.snapshot();
    }

    @GetMapping("/vehicles/summary")
    public Map<String, Long> summary() {
        return liveState.summary();
    }

    @GetMapping("/alerts/recent")
    public List<Map<String, Object>> recentAlerts(@RequestParam(defaultValue = "50") int limit) {
        return jdbc.queryForList("""
                SELECT vehicle_id, type, severity, observed_value, threshold, message, ts
                FROM alerts ORDER BY ts DESC LIMIT ?
                """, Math.min(limit, 500));
    }
}