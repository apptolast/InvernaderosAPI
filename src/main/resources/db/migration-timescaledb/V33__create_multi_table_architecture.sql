-- =============================================================================
-- V33: Arquitectura multi-tabla para separar patrones de acceso
-- Fecha: 2026-03-18
--
-- Crea 3 tablas nuevas + 2 continuous aggregates:
--
-- 1. iot.device_current_values  - Ultimo valor por code (para WebSocket live)
-- 2. iot.sensor_readings_raw    - Archivo fiel sin deduplicacion
-- 3. iot.device_commands        - Comandos enviados desde app al PLC
-- 4. iot.readings_hourly        - Agregacion horaria por code
-- 5. iot.readings_daily         - Agregacion diaria por code
--
-- La tabla iot.sensor_readings (V32) NO se modifica.
-- Todas las tablas temporales siguen la estructura (time, code, value).
-- El code es la clave de cruce con PostgreSQL metadata.
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- Tabla 1: device_current_values
-- Proposito: Ultimo valor conocido por code, para WebSocket live display
-- NO es hypertable (tabla normal, siempre pequena: ~78 rows x N clientes)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS iot.device_current_values (
    code          VARCHAR(20)   PRIMARY KEY,
    value         VARCHAR(100)  NOT NULL,
    first_seen_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    last_seen_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    update_count  BIGINT        NOT NULL DEFAULT 1
);

CREATE INDEX idx_current_values_last_seen
    ON iot.device_current_values(last_seen_at DESC);

-- ─────────────────────────────────────────────────────────────────────────────
-- Tabla 2: sensor_readings_raw
-- Proposito: Archivo fiel, todos los datos sin filtrar ni deduplicar
-- Hypertable con compresion agresiva (3 dias) y retencion 2 anios
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS iot.sensor_readings_raw (
    time  TIMESTAMPTZ   NOT NULL,
    code  VARCHAR(20)   NOT NULL,
    value VARCHAR(100)  NOT NULL,
    PRIMARY KEY (time, code)
);

SELECT create_hypertable('iot.sensor_readings_raw', 'time',
    chunk_time_interval => INTERVAL '7 days');

CREATE INDEX idx_raw_code_time
    ON iot.sensor_readings_raw(code, time DESC);

ALTER TABLE iot.sensor_readings_raw SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'code',
    timescaledb.compress_orderby = 'time DESC'
);

SELECT add_compression_policy('iot.sensor_readings_raw', INTERVAL '3 days');
SELECT add_retention_policy('iot.sensor_readings_raw', INTERVAL '2 years');

-- ─────────────────────────────────────────────────────────────────────────────
-- Tabla 3: device_commands
-- Proposito: Comandos enviados desde la app movil al PLC via MQTT
-- Hypertable con chunks de 30 dias (volumen bajo: acciones humanas)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS iot.device_commands (
    time  TIMESTAMPTZ   NOT NULL,
    code  VARCHAR(20)   NOT NULL,
    value VARCHAR(100)  NOT NULL,
    PRIMARY KEY (time, code)
);

SELECT create_hypertable('iot.device_commands', 'time',
    chunk_time_interval => INTERVAL '30 days');

CREATE INDEX idx_commands_code_time
    ON iot.device_commands(code, time DESC);

ALTER TABLE iot.device_commands SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'code',
    timescaledb.compress_orderby = 'time DESC'
);

SELECT add_compression_policy('iot.device_commands', INTERVAL '7 days');
SELECT add_retention_policy('iot.device_commands', INTERVAL '2 years');

-- ─────────────────────────────────────────────────────────────────────────────
-- Continuous Aggregate: readings_hourly
-- Agregaciones horarias por code sobre sensor_readings (deduplicada)
-- Solo valores numericos (filtra booleans/strings con regex)
-- ─────────────────────────────────────────────────────────────────────────────
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
WHERE value ~ '^-?[0-9]+\.?[0-9]*$'
GROUP BY bucket, code
WITH NO DATA;

SELECT add_continuous_aggregate_policy('iot.readings_hourly',
    start_offset  => INTERVAL '3 hours',
    end_offset    => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');

-- ─────────────────────────────────────────────────────────────────────────────
-- Continuous Aggregate: readings_daily
-- Agregaciones diarias por code sobre sensor_readings (deduplicada)
-- ─────────────────────────────────────────────────────────────────────────────
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
WHERE value ~ '^-?[0-9]+\.?[0-9]*$'
GROUP BY bucket, code
WITH NO DATA;

SELECT add_continuous_aggregate_policy('iot.readings_daily',
    start_offset  => INTERVAL '3 days',
    end_offset    => INTERVAL '1 day',
    schedule_interval => INTERVAL '6 hours');
