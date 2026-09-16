package io.fleetpulse.telemetry.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "geofences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GeofenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /** DEPOT | CUSTOMER | RESTRICTED | DANGER */
    @Column(nullable = false)
    private String type;

    @Column(name = "center_lat", nullable = false)
    private double centerLat;

    @Column(name = "center_lon", nullable = false)
    private double centerLon;

    @Column(name = "radius_m", nullable = false)
    private double radiusM;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
