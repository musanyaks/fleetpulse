package io.fleetpulse.telemetry.api;

import io.fleetpulse.telemetry.domain.GeofenceEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GeofenceMapper {
    GeofenceDto toDto(GeofenceEntity entity);
    List<GeofenceDto> toDtoList(List<GeofenceEntity> entities);
}
