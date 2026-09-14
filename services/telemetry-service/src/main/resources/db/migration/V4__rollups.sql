CREATE MATERIALIZED VIEW IF NOT EXISTS telemetry_1min
WITH (timescaledb.continuous) AS
SELECT vehicle_id,
       time_bucket(INTERVAL '1 minute', ts) AS bucket,
       avg(speed_kph)      AS avg_speed_kph,
       max(speed_kph)      AS max_speed_kph,
       avg(engine_temp_c)  AS avg_engine_temp_c,
       avg(fuel_level_pct) AS avg_fuel_level_pct,
       count(*)            AS sample_count
FROM telemetry_readings
GROUP BY vehicle_id, bucket
WITH NO DATA;

SELECT add_continuous_aggregate_policy('telemetry_1min',
    start_offset      => INTERVAL '3 hours',
    end_offset        => INTERVAL '1 minute',
    schedule_interval => INTERVAL '1 minute',
    if_not_exists     => TRUE);