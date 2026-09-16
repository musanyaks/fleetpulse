package io.fleetpulse.telemetry.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "vehicles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VehicleEntity {

    @Id
    @Column(name = "vehicle_id")
    private String vehicleId;

    @Column(nullable = false)
    private String plate;

    private String make;
    private String model;

    @Column(name = "speed_limit_kph", nullable = false)
    private double speedLimitKph;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;
}
