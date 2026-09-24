package io.fleetpulse.telemetry.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessUnitJpaRepository extends JpaRepository<BusinessUnitEntity, Long> {
    Optional<BusinessUnitEntity> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
