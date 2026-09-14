package io.fleetpulse.telemetry.live;

import io.fleetpulse.common.TelemetryMessage;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LiveVehicleStateService {

    private final Map<String, LiveVehicleState> latest = new ConcurrentHashMap<>();

    public void update(TelemetryMessage m) {
        latest.put(m.vehicleId(), LiveVehicleState.from(m));
    }

    public Collection<LiveVehicleState> snapshot() {
        return List.copyOf(latest.values());
    }

    public Map<String, Long> summary() {
        long moving = latest.values().stream().filter(s -> "MOVING".equals(s.status())).count();
        return Map.of("total", (long) latest.size(), "moving", moving, "idle", latest.size() - moving);
    }
}