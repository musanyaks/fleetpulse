package io.fleetpulse.telemetry.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverJpaRepository extends JpaRepository<DriverEntity, String> {}
