CREATE TABLE IF NOT EXISTS fleet_groups (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        TEXT NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS business_units (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        TEXT NOT NULL UNIQUE,
    description TEXT,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO fleet_groups (name, description) VALUES
  ('Trucks','Heavy duty trucks'),
  ('Vans','Delivery vans'),
  ('Pickups','Light vehicles'),
  ('Buses','Matatus & buses')
ON CONFLICT (name) DO NOTHING;

INSERT INTO business_units (name, description) VALUES
  ('Logistics','Main logistics operations'),
  ('Distribution','Regional distribution'),
  ('Transport','Fleet transport services'),
  ('Sales & Operations','Sales and customer service')
ON CONFLICT (name) DO NOTHING;
