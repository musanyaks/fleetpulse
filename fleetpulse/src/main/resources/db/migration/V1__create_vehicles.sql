CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    registration_number VARCHAR(32) NOT NULL UNIQUE,
    make VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vehicles_status ON vehicles(status);

INSERT INTO vehicles (registration_number, make, model)
VALUES
('KDA-482X', 'Toyota', 'Hilux'),
('KCB-719A', 'Isuzu', 'NPR'),
('KDG-205M', 'Mercedes-Benz', 'Sprinter')
ON CONFLICT (registration_number) DO NOTHING;
