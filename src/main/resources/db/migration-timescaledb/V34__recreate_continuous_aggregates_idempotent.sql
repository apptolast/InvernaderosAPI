-- =============================================================================
-- V34: Recrear continuous aggregates de forma idempotente
-- Fecha: 2026-03-18
--
-- V33 creo los aggregates sin IF NOT EXISTS (no soportado por TimescaleDB).
-- Esta migracion los dropea y recrea para garantizar consistencia.
-- =============================================================================

-- Drop aggregates existentes (si los hay)
DROP MATERIALIZED VIEW IF EXISTS iot.readings_hourly CASCADE;
DROP MATERIALIZED VIEW IF EXISTS iot.readings_daily CASCADE;

-- Recrear: agregaciones horarias por code
CREATE MATERIALIZED VIEW iot.readings_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) AS bucket,
    code,
    AVG(value::double precision)    AS avg_value,
    MIN(value::double precision)    AS min_value,
    MAX(value::double precision)    AS max_value,
    STDDEV(value::double precision) AS stddev_value,
    COUNT(*)                        AS count_readings
FROM iot.sensor_readings
GROUP BY bucket, code
WITH NO DATA;

SELECT add_continuous_aggregate_policy('iot.readings_hourly',
    start_offset  => INTERVAL '3 hours',
    end_offset    => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');

-- Recrear: agregaciones diarias por code
CREATE MATERIALIZED VIEW iot.readings_daily
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', time) AS bucket,
    code,
    AVG(value::double precision)    AS avg_value,
    MIN(value::double precision)    AS min_value,
    MAX(value::double precision)    AS max_value,
    STDDEV(value::double precision) AS stddev_value,
    COUNT(*)                        AS count_readings
FROM iot.sensor_readings
GROUP BY bucket, code
WITH NO DATA;

SELECT add_continuous_aggregate_policy('iot.readings_daily',
    start_offset  => INTERVAL '3 days',
    end_offset    => INTERVAL '1 day',
    schedule_interval => INTERVAL '6 hours');
