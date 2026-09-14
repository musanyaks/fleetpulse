package com.musarioba.fleetpulse.vehicle;

public class VehicleNotFoundException extends RuntimeException {
    public VehicleNotFoundException(Long id) {
        super("Vehicle not found: " + id);
    }
}
