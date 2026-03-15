-- =============================================================================
-- V32: Recrear sensor_readings simplificada (solo 3 columnas)
-- Fecha: 2026-03-15
--
-- La tabla sensor_readings se simplifica a solo 3 columnas:
-- - time: timestamp de la lectura
-- - code: codigo del device/setting (SET-00036, DEV-00031)
-- - value: valor como string (soporta numeros, booleans, texto)
--
-- El code enlaza con metadata.settings.code o metadata.devices.code
-- =============================================================================

DROP TABLE IF EXISTS iot.sensor_readings CASCADE;

CREATE TABLE iot.sensor_readings (
    time  TIMESTAMPTZ   NOT NULL,
    code  VARCHAR(20)   NOT NULL,
    value VARCHAR(100)  NOT NULL,
    PRIMARY KEY (time, code)
);

SELECT create_hypertable('iot.sensor_readings', 'time',
    chunk_time_interval => INTERVAL '7 days');

CREATE INDEX idx_sensor_readings_code_time
    ON iot.sensor_readings (code, time DESC);

ALTER TABLE iot.sensor_readings SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'code',
    timescaledb.compress_orderby = 'time DESC'
);

SELECT add_compression_policy('iot.sensor_readings', INTERVAL '7 days');
SELECT add_retention_policy('iot.sensor_readings', INTERVAL '2 years');
