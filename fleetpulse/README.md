# FleetPulse — Real-Time IoT Fleet Analytics

Production-oriented starter project for real-time vehicle telemetry using Java 21, Spring Boot, PostgreSQL, MQTT, Kafka, Redis and Docker.

## Quick start

Prerequisites:
- JDK 21
- Maven 3.9+
- Docker Desktop

Start infrastructure:

```bash
docker compose up -d
```

Run the API:

```bash
mvn spring-boot:run
```

API:
- Health: `GET http://localhost:8080/actuator/health`
- Vehicles: `GET http://localhost:8080/api/v1/vehicles`
- Telemetry: `POST http://localhost:8080/api/v1/telemetry`

## Architecture

IoT Simulator → MQTT → Ingestion → Kafka → Telemetry processing → PostgreSQL/Redis → REST API/Dashboard

This repository is intentionally structured so the starter can evolve into separate services.
