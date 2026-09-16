CREATE TABLE IF NOT EXISTS geofences (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        TEXT NOT NULL UNIQUE,
    type        TEXT NOT NULL,              -- DEPOT | CUSTOMER | RESTRICTED | DANGER
    center_lat  DOUBLE PRECISION NOT NULL,
    center_lon  DOUBLE PRECISION NOT NULL,
    radius_m    DOUBLE PRECISION NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Nairobi demo zones (real coordinates)
INSERT INTO geofences (name, type, center_lat, center_lon, radius_m) VALUES
  ('Westlands Depot',        'DEPOT',      -1.2673, 36.8065, 1500),
  ('Industrial Area',        'RESTRICTED', -1.3000, 36.8500, 2000),
  ('JKIA Restricted',        'RESTRICTED', -1.3192, 36.9278, 2500),
  ('Nairobi CBD Customer',   'CUSTOMER',   -1.2864, 36.8172,  800)
ON CONFLICT (name) DO NOTHING;
