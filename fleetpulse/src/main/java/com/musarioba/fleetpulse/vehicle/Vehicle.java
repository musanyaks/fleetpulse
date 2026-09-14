package com.musarioba.fleetpulse.vehicle;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "vehicles")
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String registrationNumber;

    @Column(nullable = false, length = 100)
    private String make;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Vehicle() {}

    public Vehicle(String registrationNumber, String make, String model) {
        this.registrationNumber = registrationNumber;
        this.make = make;
        this.model = model;
    }

    public Long getId() { return id; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String v) { this.registrationNumber = v; }
    public String getMake() { return make; }
    public void setMake(String v) { this.make = v; }
    public String getModel() { return model; }
    public void setModel(String v) { this.model = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Instant getCreatedAt() { return createdAt; }
}
