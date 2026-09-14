package io.fleetpulse.simulator;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fleetpulse.simulator")
public record SimulatorProperties(String broker, int fleetSize, int intervalMs,
                                  double baseLat, double baseLon) {}