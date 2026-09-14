CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id       TEXT PRIMARY KEY,
    plate            TEXT NOT NULL,
    make             TEXT,
    model            TEXT,
    speed_limit_kph  DOUBLE PRECISION NOT NULL DEFAULT 100,
    registered_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS alerts (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vehicle_id     TEXT NOT NULL,
    type           TEXT NOT NULL,
    severity       TEXT NOT NULL,
    observed_value DOUBLE PRECISION,
    threshold      DOUBLE PRECISION,
    message        TEXT,
    ts             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_alerts_vehicle_ts ON alerts (vehicle_id, ts DESC);
CREATE INDEX IF NOT EXISTS idx_alerts_ts ON alerts (ts DESC);