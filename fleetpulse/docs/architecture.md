# Architecture

## MVP

Vehicle Simulator → MQTT → Java Ingestion → Kafka → Consumer → PostgreSQL/Redis → REST API

## Production evolution

Split ingestion, telemetry, analytics, alerting and maintenance into independently deployable services.

## Core Kafka topics

- telemetry.raw
- telemetry.processed
- vehicle.location
- vehicle.events
- speeding.events
- maintenance.events
- alerts
