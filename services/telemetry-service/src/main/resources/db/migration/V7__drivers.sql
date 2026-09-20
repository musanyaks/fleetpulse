CREATE TABLE IF NOT EXISTS drivers (
    driver_id    TEXT PRIMARY KEY,
    name         TEXT NOT NULL,
    phone        TEXT,
    email        TEXT,
    branch       TEXT,
    joined_at    TEXT,
    experience   TEXT,
    license_exp  TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
