-- =============================================================================
-- V32: Create device_status_log table for GREENHOUSE/STATUS MQTT data
-- Fecha: 2026-03-15
--
-- Tabla simplificada para almacenar el estado de devices y settings
-- recibidos por MQTT topic GREENHOUSE/STATUS.
--
-- Formato MQTT: {"id":"SET-00036","value":15}
-- - SET-XXXXX -> metadata.settings (code)
-- - DEV-XXXXX -> metadata.devices (code)
--
-- La columna 'code' enlaza con metadata.settings.code o metadata.devices.code
-- para obtener greenhouse_id, tenant_id, unit, data_type, etc.
-- =============================================================================

DO $$ BEGIN RAISE NOTICE '=== V32 MIGRATION START: device_status_log ==='; END $$;
DO $$ BEGIN RAISE NOTICE 'Timestamp: %', NOW(); END $$;

-- =============================================================================
-- FASE 1: Crear tabla
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 1: Creating iot.device_status_log table...'; END $$;

CREATE TABLE IF NOT EXISTS iot.device_status_log (
    time  TIMESTAMPTZ   NOT NULL,
    code  VARCHAR(20)   NOT NULL,
    value VARCHAR(100)  NOT NULL,
    PRIMARY KEY (time, code)
);

COMMENT ON TABLE iot.device_status_log IS 'Time-series log of device/setting status changes from GREENHOUSE/STATUS MQTT topic';
COMMENT ON COLUMN iot.device_status_log.time IS 'Timestamp of the status reading';
COMMENT ON COLUMN iot.device_status_log.code IS 'Device or setting code (e.g., SET-00036, DEV-00031). Links to metadata.settings.code or metadata.devices.code';
COMMENT ON COLUMN iot.device_status_log.value IS 'Value as string. Type info available via metadata.data_types through the settings/devices tables';

-- =============================================================================
-- FASE 2: Convertir a hypertable (chunks de 7 dias)
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 2: Converting to hypertable...'; END $$;

SELECT create_hypertable('iot.device_status_log', 'time',
    chunk_time_interval => INTERVAL '7 days');

-- =============================================================================
-- FASE 3: Crear indices
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 3: Creating indexes...'; END $$;

CREATE INDEX idx_device_status_log_code_time
    ON iot.device_status_log (code, time DESC);

-- =============================================================================
-- FASE 4: Configurar compresion (despues de 7 dias)
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 4: Configuring compression...'; END $$;

ALTER TABLE iot.device_status_log SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'code',
    timescaledb.compress_orderby = 'time DESC'
);

SELECT add_compression_policy('iot.device_status_log', INTERVAL '7 days');

-- =============================================================================
-- FASE 5: Configurar retencion (2 anos)
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 5: Configuring retention policy...'; END $$;

SELECT add_retention_policy('iot.device_status_log', INTERVAL '2 years');

-- =============================================================================
-- VERIFICACION
-- =============================================================================
DO $$
BEGIN
    RAISE NOTICE '=== V32 MIGRATION COMPLETE ===';
    RAISE NOTICE 'Table: iot.device_status_log (hypertable)';
    RAISE NOTICE 'Columns: time (TIMESTAMPTZ), code (VARCHAR(20)), value (VARCHAR(100))';
    RAISE NOTICE 'PK: (time, code)';
    RAISE NOTICE 'Index: idx_device_status_log_code_time (code, time DESC)';
    RAISE NOTICE 'Compression: after 7 days, segmentby=code, orderby=time DESC';
    RAISE NOTICE 'Retention: 2 years';
    RAISE NOTICE 'Timestamp: %', NOW();
END $$;
