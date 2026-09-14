package com.musarioba.fleetpulse.telemetry;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryController {
    private final TelemetryProducer producer;

    public TelemetryController(TelemetryProducer producer) {
        this.producer = producer;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String ingest(@Valid @RequestBody TelemetryEvent event) {
        producer.publish(event);
        return "Telemetry accepted";
    }
}
