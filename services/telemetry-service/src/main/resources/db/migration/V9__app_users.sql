CREATE TABLE IF NOT EXISTS app_users (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username   TEXT NOT NULL UNIQUE,
    name       TEXT NOT NULL,
    email      TEXT,
    role       TEXT NOT NULL,
    department TEXT,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    last_login TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO app_users (username, name, email, role, department) VALUES
  ('admin','Musa Rioba','musa.rioba@fleetpulse.co.ke','ADMIN','Management'),
  ('manager','John Mwangi','john.mwangi@fleetpulse.co.ke','FLEET_MANAGER','Operations'),
  ('dispatcher','Amina Yusuf','amina.yusuf@fleetpulse.co.ke','DISPATCHER','Operations'),
  ('driver','Peter Otieno','peter.otieno@fleetpulse.co.ke','DRIVER','Transport'),
  ('maintenance','Samuel Wanyama','samuel.wanyama@fleetpulse.co.ke','MAINTENANCE_MANAGER','Maintenance'),
  ('analyst','Diana Anyango','diana.anyango@fleetpulse.co.ke','ANALYST','Finance'),
  ('grace','Grace Njeri','grace.njeri@fleetpulse.co.ke','MAINTENANCE_MANAGER','Safety'),
  ('kevin','Kevin Ochieno','kevin.ochieno@fleetpulse.co.ke','DISPATCHER','IT'),
  ('jane','Jane Muthoni','jane.muthoni@fleetpulse.co.ke','ANALYST','Human Resources'),
  ('david','David Muthoka','david.muthoka@fleetpulse.co.ke','DISPATCHER','Operations')
ON CONFLICT (username) DO NOTHING;
