# FleetPulse — Real-Time IoT Fleet Analytics (MVP)

Simulator → MQTT → Kafka → TimescaleDB pipeline with a real-time rule
engine, alerting, and a live fleet REST API.

## Modules
| Module | Role |
|---|---|
| `common` | Shared Kafka message contracts (records, topics) |
| `services/ingestion-service` | MQTT → Kafka, validation at the edge |
| `services/telemetry-service` | Kafka → TimescaleDB, rule engine, alerts, live API |
| `simulator` | MQTT fleet simulator (no hardware needed) |

## Quick start
```bash
docker compose up -d
mvn -q -DskipTests package
java -jar services/ingestion-service/target/ingestion-service-0.1.0.jar
java -jar services/telemetry-service/target/telemetry-service-0.1.0.jar
FLEET_SIZE=50 java -jar simulator/target/simulator-0.1.0.jar
