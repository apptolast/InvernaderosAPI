-- ============================================================================
-- V26: SIMPLIFICACION DE TIMESCALEDB
-- Fecha: 2025-12-29
-- Descripcion: Simplifica sensor_readings a readings
-- NOTA: Esta migracion se ejecuta en TimescaleDB, NO en PostgreSQL metadata
--       Debe ejecutarse manualmente: psql -h localhost -p 30432 -U admin -d greenhouse_timeseries -f V26...
-- ============================================================================
-- ADVERTENCIA: TimescaleDB requiere manejo especial de hypertables
-- Documentacion: https://docs.timescale.com/use-timescale/latest/schema-management/
-- ============================================================================

-- ==========================================================================
-- PASO 1: Crear nueva tabla readings simplificada
-- ==========================================================================
CREATE TABLE IF NOT EXISTS iot.readings (
    time TIMESTAMPTZ NOT NULL,
    device_id UUID NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    metadata JSONB DEFAULT '{}'::jsonb,
    PRIMARY KEY (time, device_id)
);

-- Convertir a hypertable
SELECT create_hypertable(
    'iot.readings',
    'time',
    chunk_time_interval => INTERVAL '7 days',
    if_not_exists => TRUE
);

-- Indices
CREATE INDEX IF NOT EXISTS idx_readings_device_time
    ON iot.readings(device_id, time DESC);

CREATE INDEX IF NOT EXISTS idx_readings_time
    ON iot.readings(time DESC);

-- ==========================================================================
-- PASO 2: Migrar datos de sensor_readings a readings
-- ==========================================================================
-- Solo migrar registros que tengan device_id
INSERT INTO iot.readings (time, device_id, value, metadata)
SELECT
    sr.time,
    sr.device_id,
    sr.value,
    jsonb_build_object(
        'sensor_id', sr.sensor_id,
        'sensor_type', sr.sensor_type,
        'unit', sr.unit,
        'greenhouse_id', sr.greenhouse_id::TEXT,
        'tenant_id', sr.tenant_id::TEXT
    ) AS metadata
FROM iot.sensor_readings sr
WHERE sr.device_id IS NOT NULL
ON CONFLICT (time, device_id) DO NOTHING;

-- ==========================================================================
-- PASO 3: Crear continuous aggregates simplificados
-- ==========================================================================
-- Hourly aggregation
CREATE MATERIALIZED VIEW IF NOT EXISTS iot.readings_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) AS bucket,
    device_id,
    AVG(value) AS avg_value,
    MIN(value) AS min_value,
    MAX(value) AS max_value,
    COUNT(*) AS count
FROM iot.readings
GROUP BY bucket, device_id
WITH NO DATA;

-- Daily aggregation
CREATE MATERIALIZED VIEW IF NOT EXISTS iot.readings_daily
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', time) AS bucket,
    device_id,
    AVG(value) AS avg_value,
    MIN(value) AS min_value,
    MAX(value) AS max_value,
    COUNT(*) AS count
FROM iot.readings
GROUP BY bucket, device_id
WITH NO DATA;

-- Indices para las vistas
CREATE INDEX IF NOT EXISTS idx_readings_hourly_device
    ON iot.readings_hourly(device_id, bucket DESC);

CREATE INDEX IF NOT EXISTS idx_readings_daily_device
    ON iot.readings_daily(device_id, bucket DESC);

-- ==========================================================================
-- PASO 4: Configurar politicas de retencion y compresion
-- ==========================================================================
-- Compresion despues de 7 dias
ALTER TABLE iot.readings SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'device_id',
    timescaledb.compress_orderby = 'time DESC'
);

-- Politica de compresion automatica
SELECT add_compression_policy('iot.readings', INTERVAL '7 days', if_not_exists => TRUE);

-- Politica de retencion (2 anios)
SELECT add_retention_policy('iot.readings', INTERVAL '2 years', if_not_exists => TRUE);

-- Politicas de refresh para continuous aggregates
SELECT add_continuous_aggregate_policy('iot.readings_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE
);

SELECT add_continuous_aggregate_policy('iot.readings_daily',
    start_offset => INTERVAL '3 days',
    end_offset => INTERVAL '1 day',
    schedule_interval => INTERVAL '1 day',
    if_not_exists => TRUE
);

-- ==========================================================================
-- PASO 5: Comentarios
-- ==========================================================================
COMMENT ON TABLE iot.readings IS 'Lecturas de dispositivos IoT simplificadas (time, device_id, value, metadata)';
COMMENT ON MATERIALIZED VIEW iot.readings_hourly IS 'Agregaciones horarias de lecturas por dispositivo';
COMMENT ON MATERIALIZED VIEW iot.readings_daily IS 'Agregaciones diarias de lecturas por dispositivo';

-- ==========================================================================
-- PASO 6: Resumen
-- ==========================================================================
DO $$
DECLARE
    old_count BIGINT;
    new_count BIGINT;
BEGIN
    SELECT COUNT(*) INTO old_count FROM iot.sensor_readings WHERE device_id IS NOT NULL;
    SELECT COUNT(*) INTO new_count FROM iot.readings;

    RAISE NOTICE '';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'SIMPLIFICACION TIMESCALEDB COMPLETADA';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Registros en sensor_readings (con device_id): %', old_count;
    RAISE NOTICE 'Registros migrados a readings: %', new_count;
    RAISE NOTICE '';
    RAISE NOTICE 'Nueva estructura:';
    RAISE NOTICE '  - iot.readings (hypertable principal)';
    RAISE NOTICE '  - iot.readings_hourly (continuous aggregate)';
    RAISE NOTICE '  - iot.readings_daily (continuous aggregate)';
    RAISE NOTICE '';
    RAISE NOTICE 'NOTA: sensor_readings se mantiene como backup.';
    RAISE NOTICE 'Eliminar manualmente despues de validar:';
    RAISE NOTICE '  DROP TABLE iot.sensor_readings CASCADE;';
    RAISE NOTICE '========================================';
END $$;

-- ==========================================================================
-- NOTA IMPORTANTE
-- ==========================================================================
-- La tabla sensor_readings NO se elimina automaticamente.
-- Esto permite:
--   1. Validar que readings funciona correctamente
--   2. Mantener acceso a datos historicos con sensor_id
--   3. Rollback si hay problemas
--
-- Para eliminar sensor_readings despues de validar (30+ dias):
--   DROP MATERIALIZED VIEW IF EXISTS iot.sensor_readings_hourly CASCADE;
--   DROP MATERIALIZED VIEW IF EXISTS iot.cagg_sensor_readings_hourly CASCADE;
--   DROP MATERIALIZED VIEW IF EXISTS iot.cagg_sensor_readings_daily CASCADE;
--   DROP MATERIALIZED VIEW IF EXISTS iot.cagg_sensor_readings_monthly CASCADE;
--   DROP MATERIALIZED VIEW IF EXISTS iot.cagg_greenhouse_conditions_realtime CASCADE;
--   DROP MATERIALIZED VIEW IF EXISTS iot.cagg_sensor_health_hourly CASCADE;
--   DROP TABLE IF EXISTS iot.sensor_readings CASCADE;
-- ============================================================================
