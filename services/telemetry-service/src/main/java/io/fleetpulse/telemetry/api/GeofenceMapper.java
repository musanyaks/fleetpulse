package io.fleetpulse.telemetry.api;

import io.fleetpulse.telemetry.domain.GeofenceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GeofenceMapper {

    @Mapping(source = "centerLat", target = "lat")
    @Mapping(source = "centerLon", target = "lon")
    GeofenceDto toDto(GeofenceEntity entity);

    List<GeofenceDto> toDtoList(List<GeofenceEntity> entities);
}
