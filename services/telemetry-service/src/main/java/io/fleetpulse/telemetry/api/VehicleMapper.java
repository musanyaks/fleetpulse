package io.fleetpulse.telemetry.api;

import io.fleetpulse.telemetry.domain.VehicleEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VehicleMapper {
    VehicleDto toDto(VehicleEntity entity);
    List<VehicleDto> toDtoList(List<VehicleEntity> entities);
}
