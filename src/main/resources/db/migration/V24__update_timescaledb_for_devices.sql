-- ============================================================================
-- V24: Actualizar TimescaleDB para compatibilidad con devices
-- Fecha: 2025-12-29
-- Descripcion: Agregar columnas device_id y sector_id a sensor_readings
-- Referencia: https://docs.timescale.com/use-timescale/latest/schema-management/
-- NOTA: Este script se ejecuta en PostgreSQL metadata, NO en TimescaleDB directamente
--       Los cambios en TimescaleDB se deben ejecutar manualmente en iot.sensor_readings
-- ============================================================================

-- IMPORTANTE: Este archivo contiene los comandos que se deben ejecutar
-- en la base de datos TimescaleDB (greenhouse_timeseries o greenhouse_timeseries_dev)
-- NO se pueden ejecutar automaticamente con Flyway porque es una BD diferente.

-- Guardar el script como referencia en una tabla de metadata
CREATE TABLE IF NOT EXISTS metadata.migration_scripts (
    id SERIAL PRIMARY KEY,
    migration_version VARCHAR(20) NOT NULL,
    target_database VARCHAR(100) NOT NULL,
    script_content TEXT NOT NULL,
    executed BOOLEAN DEFAULT FALSE,
    executed_at TIMESTAMPTZ,
    executed_by VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Insertar el script para TimescaleDB
INSERT INTO metadata.migration_scripts (migration_version, target_database, script_content, notes)
VALUES (
    'V24',
    'greenhouse_timeseries',
    $SCRIPT$
-- ============================================================================
-- EJECUTAR EN TIMESCALEDB (greenhouse_timeseries o greenhouse_timeseries_dev)
-- ============================================================================

-- 1. Agregar columna device_id (referencia a metadata.devices.id)
ALTER TABLE iot.sensor_readings
ADD COLUMN IF NOT EXISTS device_id UUID;

-- 2. Agregar columna sector_id para queries por sector
ALTER TABLE iot.sensor_readings
ADD COLUMN IF NOT EXISTS sector_id UUID;

-- 3. Crear indice para queries por device_id
CREATE INDEX IF NOT EXISTS idx_sensor_readings_device_id
ON iot.sensor_readings(device_id, time DESC)
WHERE device_id IS NOT NULL;

-- 4. Crear indice para queries por sector
CREATE INDEX IF NOT EXISTS idx_sensor_readings_sector_id
ON iot.sensor_readings(sector_id, time DESC)
WHERE sector_id IS NOT NULL;

-- 5. Comentarios
COMMENT ON COLUMN iot.sensor_readings.device_id IS 'FK a metadata.devices.id (nueva tabla unificada)';
COMMENT ON COLUMN iot.sensor_readings.sector_id IS 'FK a metadata.sectors.id para queries por sector';

-- 6. OPCIONAL: Actualizar registros existentes (ejecutar en lotes)
-- NOTA: Este proceso puede tardar mucho tiempo si hay muchos registros
-- Se recomienda ejecutar manualmente y monitorear

/*
-- Actualizar device_id basandose en sensor_id y greenhouse_id
UPDATE iot.sensor_readings sr
SET device_id = d.id
FROM metadata.devices d
WHERE d.mqtt_field_name = sr.sensor_id
AND d.greenhouse_id = sr.greenhouse_id
AND sr.device_id IS NULL
AND sr.time > NOW() - INTERVAL '7 days';  -- Solo ultimos 7 dias primero
*/

$SCRIPT$,
    'Script para ejecutar manualmente en TimescaleDB. Agregar columnas device_id y sector_id a sensor_readings.'
);

-- Log
DO $$
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'IMPORTANTE - ACCION MANUAL REQUERIDA';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'El script para TimescaleDB se ha guardado en:';
    RAISE NOTICE '  metadata.migration_scripts (migration_version = V24)';
    RAISE NOTICE '';
    RAISE NOTICE 'Para ejecutarlo:';
    RAISE NOTICE '  1. Conectar a greenhouse_timeseries (o _dev)';
    RAISE NOTICE '  2. Ejecutar el script guardado';
    RAISE NOTICE '  3. Marcar executed = TRUE en migration_scripts';
    RAISE NOTICE '========================================';
END $$;
