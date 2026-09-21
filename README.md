# 🛰️ FleetPulse — Real-Time IoT Fleet Analytics Platform

![CI](https://github.com/musanyaks/fleetpulse/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.7-black)
![TimescaleDB](https://img.shields.io/badge/TimescaleDB-2.15-orange)
![License](https://img.shields.io/badge/License-MIT-yellow)

> Production-shaped, event-driven fleet telemetry platform: **25 simulated vehicles on real Kenyan highway corridors → MQTT → Kafka → TimescaleDB → live rule engine, geofencing, driver scoring and an operations dashboard** — in one `docker compose up`.


---

## What it does

| Capability | How |
|---|---|
| **Telemetry ingestion** | Vehicles publish GPS/sensor telemetry over MQTT; a validated edge service forwards to Kafka (keyed by vehicle for per-vehicle ordering) |
| **Time-series storage** | TimescaleDB hypertable with continuous aggregates (1-min rollups) |
| **Real-time rules** | Speeding (per-vehicle limits), engine overheating, low fuel, harsh braking/acceleration — with per-vehicle alert cooldowns |
| **Geofencing** | Edge-triggered enter/exit/dwell state machine, REST CRUD, map overlay |
| **Driver scoring** | `score = 100 − penalties`, computed from the live alert stream, per-driver attribution |
| **Fuel analytics** | Range consumption, efficiency, corridor distribution, **refuel detection from tank-level rises** |
| **Live dashboard** | Ten pages: Overview, Vehicles, Live Tracking, Trips, Drivers, Fuel, Alerts, Geofences, Reports, Settings — all driven by the pipeline |
| **Security** | Form login + HTTP Basic, RBAC (6 roles), **Redis-backed sessions** |
| **Observability** | Prometheus endpoints on both services, Grafana provisioned |

## Architecture

```text
 Vehicle Simulator ──► MQTT (Mosquitto) ──► Ingestion Service ──► Kafka
 (Java, corridors)        (QoS 1)          (validate, key, count)     │
                                                                       ▼
                     ┌─────────────────────────────► Telemetry Service (Java 21)
                     │                                │ batch inserts, rule engine,
                     │                                │ geofence evaluator, scoring
                     ▼                                ▼
                 Grafana ◄── Prometheus      TimescaleDB (hypertable) + PostgreSQL
                     │                                │
                 Dashboard ◄──── REST API ────────────┘ + Redis (sessions)
```

**Modules** (multi-repo-style Maven monorepo):

```text
fleetpulse/
├── common/                  shared message contracts (records, validation)
├── services/
│   ├── ingestion-service/   MQTT → Kafka edge (validation, metrics)
│   └── telemetry-service/   Kafka → TimescaleDB, rules, geofences, REST API, dashboard
├── simulator/               25-vehicle fleet simulator on 5 Kenyan corridors
└── infrastructure/          mosquitto, grafana provisioning
```

## Quick Start

```bash
git clone https://github.com/musanyaks/fleetpulse && cd fleetpulse
docker compose up -d --build        # everything: brokers, DB, services, simulator, dashboard
```

- **Dashboard:** http://localhost:8082 — sign in `manager / manager123`
- **Grafana:** http://localhost:3000 (`admin/admin`)
- **API (Basic):** `curl -u analyst:analyst123 localhost:8082/api/v1/vehicles/live`

Demo users: `admin/admin123` · `manager/manager123` · `dispatcher/dispatch123` · `analyst/analyst123` · `maintenance/maint123` · `driver/driver123`

## Engineering highlights

- **Two persistence paths by design**: JPA for domain CRUD, raw JDBC batching for the 10k+/sec telemetry hot path
- **Edge-triggered geofence evaluator**: enter/exit/dwell state machine, silent first-sighting (no startup alert storm), zone cache refresh
- **Refuel detection**: tank-level rises between polls mark real refuel events at GPS positions
- **Timezone correctness**: "a day" = `Africa/Nairobi`, boundaries computed once in Java and passed as UTC instants
- **Session resilience**: Redis-backed HTTP sessions + `restart: unless-stopped` — restarts don't log users out or strand state
- **Zero-dependency dashboards**: server-rendered pages, inline SVG charts, vendored Leaflet, per-vehicle image chain
- **Parse-verified frontends**: every page's script is machine-checked (`node --check`) before deploy

## API surface (v1)

```text
GET  /api/v1/vehicles/live          live fleet snapshot
GET  /api/v1/vehicles/summary       moving/idle counts
GET  /api/v1/vehicles               roster (paged)
POST /api/v1/vehicles               register vehicle (RBAC: write)
PUT  /api/v1/vehicles/{id}          edit attributes (RBAC: write)
GET  /api/v1/drivers/summary        driver scores + 24h events
GET  /api/v1/trips/summary?date=    per-day departures/distance (Nairobi tz)
GET  /api/v1/geofences              zones · POST to create · /events feed
GET  /api/v1/reports/{utilization|drivers|fuel|violations|geofence|daily}
GET  /api/v1/me · /actuator/prometheus (ADMIN)
```

## Configuration

Everything is env-driven (12-factor):

| Variable | Default | Purpose |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | broker address |
| `DB_HOST` / `DB_USER` / `DB_PASSWORD` | `localhost/fleetpulse/fleetpulse` | TimescaleDB |
| `REDIS_HOST` | `localhost` | sessions |
| `MQTT_HOST` | `localhost` | broker for ingestion/simulator |
| `FLEET_SIZE` | `25` | simulated fleet size |

## Roadmap

- [ ] **Redis live-state** — vehicle state offloaded from JVM (multi-replica telemetry)
- [ ] **Dead-letter queue + retry** for consumer write failures
- [ ] **JWT auth + users in PostgreSQL** (replaces in-memory demo users)
- [ ] **Trips module** — persisted trip state machine (origin/destination/stops)
- [ ] **Nightly rollups** — per-day fleet stats for month-over-month trends
- [ ] **Per-vehicle history endpoint** — instant trails + telemetry sparklines
- [ ] **Testcontainers CI** — repository/report queries exercised against real Postgres
- [ ] **CI-built images → GHCR** — push-to-deploy on a VPS

## Lessons learned (highlights)

- MapStruct maps by name — mismatches produce silent `null`s; strictify with `unmappedTargetPolicy`
- The Postgres function `round(double, int)` doesn't exist — raw SQL has no compile-time net, hence Testcontainers on the roadmap
- "Today" computed in three places (JVM, DB, browser) drifts across timezones — one fleet-timezone definition at the domain layer
- Maps inside re-rendered panels need an explicit lifecycle — and never shadow library globals (`const L` vs Leaflet's `L`)

*(Full post-mortems in the git history — each incident closed with its fix.)*

## License

[MIT](LICENSE)
