package io.fleetpulse.telemetry.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "fleet_groups")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FleetGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
