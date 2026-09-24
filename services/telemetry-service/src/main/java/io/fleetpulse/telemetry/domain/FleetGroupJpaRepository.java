package io.fleetpulse.telemetry.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FleetGroupJpaRepository extends JpaRepository<FleetGroupEntity, Long> {
    Optional<FleetGroupEntity> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
