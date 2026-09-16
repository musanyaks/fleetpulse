package io.fleetpulse.telemetry.api;

import io.fleetpulse.telemetry.domain.AlertEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AlertMapper {
    AlertDto toDto(AlertEntity entity);
    List<AlertDto> toDtoList(List<AlertEntity> entities);
}
