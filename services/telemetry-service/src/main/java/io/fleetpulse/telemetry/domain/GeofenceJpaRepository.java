package io.fleetpulse.telemetry.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GeofenceJpaRepository extends JpaRepository<GeofenceEntity, Long> {}
