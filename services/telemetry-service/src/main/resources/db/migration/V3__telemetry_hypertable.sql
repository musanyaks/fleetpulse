CREATE TABLE IF NOT EXISTS telemetry_readings (
    vehicle_id     TEXT NOT NULL,
    ts             TIMESTAMPTZ NOT NULL,
    latitude       DOUBLE PRECISION NOT NULL,
    longitude      DOUBLE PRECISION NOT NULL,
    speed_kph      DOUBLE PRECISION NOT NULL,
    engine_temp_c  DOUBLE PRECISION,
    fuel_level_pct DOUBLE PRECISION,
    rpm            DOUBLE PRECISION,
    odometer_km    DOUBLE PRECISION,
    ignition       TEXT,
    ingested_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

SELECT create_hypertable('telemetry_readings', 'ts',
    partitioning_column => 'vehicle_id',
    number_partitions   => 8,
    chunk_time_interval => INTERVAL '1 day',
    if_not_exists       => TRUE);

CREATE INDEX IF NOT EXISTS idx_telemetry_vehicle_ts
    ON telemetry_readings (vehicle_id, ts DESC);