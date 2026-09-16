package io.fleetpulse.telemetry.api;

import io.fleetpulse.common.AlertType;
import io.fleetpulse.telemetry.domain.AlertEntity;
import io.fleetpulse.telemetry.domain.AlertJpaRepository;
import io.fleetpulse.telemetry.live.LiveVehicleState;
import io.fleetpulse.telemetry.live.LiveVehicleStateService;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Driver scoring per spec:
 *   DriverScore = 100 − speeding − harshBraking − harshAcceleration − …
 * Penalties derive from real alerts attributed to the driver via the
 * vehicle→driver pairing carried on the telemetry stream.
 */
@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController {

    private final LiveVehicleStateService liveState;
    private final AlertJpaRepository alerts;

    public DriverController(LiveVehicleStateService liveState, AlertJpaRepository alerts) {
        this.liveState = liveState;
        this.alerts = alerts;
    }

    @GetMapping("/summary")
    public List<DriverScoreDto> summary() {
        Collection<LiveVehicleState> live = liveState.snapshot();
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);

        // alerts last 24h, counted per vehicle per type
        Map<String, Map<AlertType, Long>> perVehicle = alerts.findAllByOrderByTsDesc(PageRequest.of(0, 2000))
                .stream()
                .filter(a -> a.getTs().isAfter(since))
                .collect(Collectors.groupingBy(AlertEntity::getVehicleId,
                        Collectors.groupingBy(AlertEntity::getType, Collectors.counting())));

        // group vehicles by driver
        Map<String, List<LiveVehicleState>> byDriver = live.stream()
                .filter(v -> v.driverId() != null)
                .collect(Collectors.groupingBy(LiveVehicleState::driverId,
                        LinkedHashMap::new, Collectors.toList()));

        return byDriver.entrySet().stream()
                .map(e -> score(e.getKey(), e.getValue(), perVehicle))
                .sorted(Comparator.comparingDouble(DriverScoreDto::score).reversed())
                .toList();
    }

    private DriverScoreDto score(String driverId, List<LiveVehicleState> vehicles,
                                 Map<String, Map<AlertType, Long>> perVehicle) {
        long speeding = 0, braking = 0, accel = 0;
        for (LiveVehicleState v : vehicles) {
            Map<AlertType, Long> counts = perVehicle.getOrDefault(v.vehicleId(), Map.of());
            speeding += counts.getOrDefault(AlertType.SPEEDING, 0L);
            braking  += counts.getOrDefault(AlertType.HARSH_BRAKING, 0L);
            accel    += counts.getOrDefault(AlertType.HARSH_ACCELERATION, 0L);
        }
        double s = 100
                - Math.min(40, speeding * 4)
                - Math.min(24, braking * 3)
                - Math.min(16, accel * 2);
        double finalScore = Math.max(40, s);

        LiveVehicleState primary = vehicles.get(0);
        return new DriverScoreDto(driverId,
                primary.driverName() != null ? primary.driverName() : driverId,
                vehicles.stream().map(LiveVehicleState::vehicleId).toList(),
                primary.status(), finalScore, speeding, braking, accel);
    }
}
