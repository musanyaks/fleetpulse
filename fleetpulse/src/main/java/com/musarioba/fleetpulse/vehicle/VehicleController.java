package com.musarioba.fleetpulse.vehicle;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {
    private final VehicleRepository repository;

    public VehicleController(VehicleRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Vehicle> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Vehicle findById(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Vehicle create(@Valid @RequestBody CreateVehicleRequest request) {
        return repository.save(new Vehicle(request.registrationNumber(), request.make(), request.model()));
    }

    public record CreateVehicleRequest(
            @NotBlank String registrationNumber,
            @NotBlank String make,
            @NotBlank String model) {}
}
