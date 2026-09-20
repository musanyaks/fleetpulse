package io.fleetpulse.telemetry.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "drivers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DriverEntity {

    @Id
    @Column(name = "driver_id")
    private String driverId;

    @Column(nullable = false)
    private String name;

    private String phone;
    private String email;
    private String branch;
    @Column(name = "joined_at")
    private String joinedAt;
    @Column(name = "experience")
    private String experience;
    @Column(name = "license_exp")
    private String licenseExp;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
