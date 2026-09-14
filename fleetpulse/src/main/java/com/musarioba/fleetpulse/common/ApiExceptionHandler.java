package com.musarioba.fleetpulse.common;

import com.musarioba.fleetpulse.vehicle.VehicleNotFoundException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(VehicleNotFoundException.class)
    ResponseEntity<Map<String, Object>> vehicleNotFound(VehicleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "timestamp", Instant.now(),
                "status", 404,
                "code", "VEHICLE_NOT_FOUND",
                "message", ex.getMessage()
        ));
    }
}
