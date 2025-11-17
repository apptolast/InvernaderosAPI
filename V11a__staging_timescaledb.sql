-- =====================================================
-- V11: CREATE STAGING INFRASTRUCTURE - TimescaleDB
-- =====================================================
-- Description: Tablas staging e intermedias para operaciones masivas
--              con millones de registros
-- Environment: ALL (DEV and PROD)
-- Database: greenhouse_timeseries (TimescaleDB)
-- Author: Claude Code
-- Date: 2025-11-16
-- =====================================================

-- =====================================================
-- 1. CREATE STAGING SCHEMA
-- =====================================================

CREATE SCHEMA IF NOT EXISTS staging;

COMMENT ON SCHEMA staging IS 'Schema para tablas staging e intermedias antes de inserción en producción';

-- =====================================================
-- 2. STAGING TABLE - Raw Sensor Readings
-- =====================================================
-- Tabla para recibir datos sin validar desde MQTT/API

CREATE TABLE IF NOT EXISTS staging.sensor_readings_raw (
    -- Mismo estructura que iot.sensor_readings pero sin constraints estrictos
    id BIGSERIAL PRIMARY KEY,
    time TIMESTAMPTZ NOT NULL,
    sensor_id VARCHAR(100),  -- Más permisivo para capturar datos malformados
    greenhouse_id VARCHAR(100),  -- Acepta VARCHAR o UUID en string
    tenant_id VARCHAR(100),  -- Acepta VARCHAR o UUID en string
    sensor_type VARCHAR(50),
    value DOUBLE PRECISION,
    unit VARCHAR(50),
    metadata JSONB,

    -- Campos adicionales para staging
    received_at TIMESTAMPTZ DEFAULT NOW(),
    source VARCHAR(50) DEFAULT 'MQTT',  -- MQTT, API, BULK_IMPORT
    validation_status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, VALID, INVALID
    validation_errors TEXT,
    processed_at TIMESTAMPTZ,
    batch_id UUID,

    -- Índices para búsqueda rápida
    CONSTRAINT chk_validation_status CHECK (validation_status IN ('PENDING', 'VALID', 'INVALID'))
);

CREATE INDEX idx_staging_raw_validation_status ON staging.sensor_readings_raw(validation_status);
CREATE INDEX idx_staging_raw_batch_id ON staging.sensor_readings_raw(batch_id);
CREATE INDEX idx_staging_raw_received_at ON staging.sensor_readings_raw(received_at DESC);

COMMENT ON TABLE staging.sensor_readings_raw IS 'Tabla staging para recibir datos crudos sin validación previa';

-- =====================================================
-- 3. STAGING TABLE - Validated Sensor Readings
-- =====================================================
-- Tabla para datos validados listos para insertar

CREATE TABLE IF NOT EXISTS staging.sensor_readings_validated (
    id BIGSERIAL PRIMARY KEY,
    time TIMESTAMPTZ NOT NULL,
    sensor_id VARCHAR(50) NOT NULL,
    greenhouse_id UUID NOT NULL,
    tenant_id UUID,
    sensor_type VARCHAR(30) NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    unit VARCHAR(20),
    metadata JSONB,

    -- Información de staging
    validated_at TIMESTAMPTZ DEFAULT NOW(),
    batch_id UUID NOT NULL,
    original_raw_id BIGINT,  -- Referencia a staging.sensor_readings_raw

    FOREIGN KEY (original_raw_id) REFERENCES staging.sensor_readings_raw(id) ON DELETE SET NULL
);

CREATE INDEX idx_staging_validated_batch_id ON staging.sensor_readings_validated(batch_id);
CREATE INDEX idx_staging_validated_time ON staging.sensor_readings_validated(time DESC);

COMMENT ON TABLE staging.sensor_readings_validated IS 'Datos validados listos para migrar a producción';

-- =====================================================
-- 4. BULK IMPORT LOG
-- =====================================================
-- Auditoría de operaciones masivas

CREATE TABLE IF NOT EXISTS staging.bulk_import_log (
    id BIGSERIAL PRIMARY KEY,
    batch_id UUID UNIQUE NOT NULL,
    operation_type VARCHAR(50) NOT NULL,  -- INSERT, UPDATE, DELETE, MIGRATE
    source VARCHAR(50),  -- MQTT, API, CSV_IMPORT, MANUAL
    total_records INT NOT NULL DEFAULT 0,
    successful_records INT NOT NULL DEFAULT 0,
    failed_records INT NOT NULL DEFAULT 0,
    validation_errors_count INT NOT NULL DEFAULT 0,

    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    duration_seconds INT,

    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',  -- RUNNING, COMPLETED, FAILED, ROLLED_BACK
    error_message TEXT,

    -- Usuario/sistema que ejecutó la operación
    executed_by VARCHAR(100),

    -- Metadata adicional
    import_metadata JSONB,

    CONSTRAINT chk_bulk_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'ROLLED_BACK'))
);

CREATE INDEX idx_bulk_log_batch_id ON staging.bulk_import_log(batch_id);
CREATE INDEX idx_bulk_log_started_at ON staging.bulk_import_log(started_at DESC);
CREATE INDEX idx_bulk_log_status ON staging.bulk_import_log(status);

COMMENT ON TABLE staging.bulk_import_log IS 'Log de auditoría para operaciones masivas de importación';

-- =====================================================
-- 5. DATA VALIDATION RULES TABLE
-- =====================================================
-- Reglas de validación configurables por sensor

CREATE TABLE IF NOT EXISTS staging.validation_rules (
    id SERIAL PRIMARY KEY,
    sensor_type VARCHAR(30) NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,  -- RANGE, REQUIRED, FORMAT, CUSTOM
    rule_config JSONB NOT NULL,  -- {"min": 0, "max": 100} para RANGE
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),

    UNIQUE (sensor_type, rule_name)
);

CREATE INDEX idx_validation_rules_sensor_type ON staging.validation_rules(sensor_type);
CREATE INDEX idx_validation_rules_active ON staging.validation_rules(is_active) WHERE is_active = TRUE;

COMMENT ON TABLE staging.validation_rules IS 'Reglas de validación configurables para datos de sensores';

-- Insertar reglas de validación por defecto
INSERT INTO staging.validation_rules (sensor_type, rule_name, rule_type, rule_config) VALUES
('TEMPERATURE', 'valid_range', 'RANGE', '{"min": -50, "max": 100, "unit": "°C"}'::JSONB),
('HUMIDITY', 'valid_range', 'RANGE', '{"min": 0, "max": 100, "unit": "%"}'::JSONB),
('SOIL_MOISTURE', 'valid_range', 'RANGE', '{"min": 0, "max": 100, "unit": "%"}'::JSONB),
('LIGHT_INTENSITY', 'valid_range', 'RANGE', '{"min": 0, "max": 200000, "unit": "lux"}'::JSONB),
('CO2', 'valid_range', 'RANGE', '{"min": 0, "max": 5000, "unit": "ppm"}'::JSONB),
('PRESSURE', 'valid_range', 'RANGE', '{"min": 800, "max": 1200, "unit": "hPa"}'::JSONB)
ON CONFLICT (sensor_type, rule_name) DO NOTHING;

-- =====================================================
-- 6. STORED PROCEDURE - Validate Sensor Readings
-- =====================================================

CREATE OR REPLACE FUNCTION staging.proc_validate_sensor_readings(
    p_batch_id UUID DEFAULT NULL
)
RETURNS TABLE(
    total_processed INT,
    total_valid INT,
    total_invalid INT,
    validation_summary JSONB
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_total_processed INT := 0;
    v_total_valid INT := 0;
    v_total_invalid INT := 0;
    v_validation_summary JSONB := '[]'::JSONB;
    v_batch_id UUID;
    rec RECORD;
    v_is_valid BOOLEAN;
    v_errors TEXT[];
BEGIN
    -- Si no se proporciona batch_id, usar UUID generado
    v_batch_id := COALESCE(p_batch_id, gen_random_uuid());

    RAISE NOTICE 'Iniciando validación de batch: %', v_batch_id;

    -- Procesar cada registro pendiente
    FOR rec IN
        SELECT * FROM staging.sensor_readings_raw
        WHERE validation_status = 'PENDING'
          AND (p_batch_id IS NULL OR batch_id = p_batch_id)
        ORDER BY received_at
    LOOP
        v_total_processed := v_total_processed + 1;
        v_is_valid := TRUE;
        v_errors := ARRAY[]::TEXT[];

        -- Validación 1: Campos requeridos
        IF rec.time IS NULL THEN
            v_is_valid := FALSE;
            v_errors := array_append(v_errors, 'Campo time es obligatorio');
        END IF;

        IF rec.sensor_id IS NULL OR trim(rec.sensor_id) = '' THEN
            v_is_valid := FALSE;
            v_errors := array_append(v_errors, 'Campo sensor_id es obligatorio');
        END IF;

        IF rec.greenhouse_id IS NULL OR trim(rec.greenhouse_id) = '' THEN
            v_is_valid := FALSE;
            v_errors := array_append(v_errors, 'Campo greenhouse_id es obligatorio');
        END IF;

        IF rec.sensor_type IS NULL OR trim(rec.sensor_type) = '' THEN
            v_is_valid := FALSE;
            v_errors := array_append(v_errors, 'Campo sensor_type es obligatorio');
        END IF;

        IF rec.value IS NULL THEN
            v_is_valid := FALSE;
            v_errors := array_append(v_errors, 'Campo value es obligatorio');
        END IF;

        -- Validación 2: Formato UUID
        BEGIN
            PERFORM rec.greenhouse_id::UUID;
        EXCEPTION WHEN OTHERS THEN
            v_is_valid := FALSE;
            v_errors := array_append(v_errors, 'greenhouse_id no es un UUID válido');
        END;

        IF rec.tenant_id IS NOT NULL THEN
            BEGIN
                PERFORM rec.tenant_id::UUID;
            EXCEPTION WHEN OTHERS THEN
                v_is_valid := FALSE;
                v_errors := array_append(v_errors, 'tenant_id no es un UUID válido');
            END;
        END IF;

        -- Validación 3: Rangos de valores según reglas
        IF v_is_valid AND rec.sensor_type IS NOT NULL THEN
            DECLARE
                v_rule RECORD;
            BEGIN
                FOR v_rule IN
                    SELECT * FROM staging.validation_rules
                    WHERE sensor_type = rec.sensor_type
                      AND rule_type = 'RANGE'
                      AND is_active = TRUE
                LOOP
                    IF rec.value < (v_rule.rule_config->>'min')::DOUBLE PRECISION OR
                       rec.value > (v_rule.rule_config->>'max')::DOUBLE PRECISION THEN
                        v_is_valid := FALSE;
                        v_errors := array_append(v_errors,
                            format('Valor %s fuera de rango [%s, %s] para sensor tipo %s',
                                rec.value,
                                v_rule.rule_config->>'min',
                                v_rule.rule_config->>'max',
                                rec.sensor_type
                            )
                        );
                    END IF;
                END LOOP;
            END;
        END IF;

        -- Actualizar estado de validación en staging.sensor_readings_raw
        IF v_is_valid THEN
            UPDATE staging.sensor_readings_raw
            SET validation_status = 'VALID',
                validation_errors = NULL,
                processed_at = NOW(),
                batch_id = v_batch_id
            WHERE id = rec.id;

            -- Insertar en tabla validada
            INSERT INTO staging.sensor_readings_validated (
                time, sensor_id, greenhouse_id, tenant_id, sensor_type,
                value, unit, metadata, batch_id, original_raw_id
            ) VALUES (
                rec.time,
                rec.sensor_id,
                rec.greenhouse_id::UUID,
                CASE WHEN rec.tenant_id IS NOT NULL THEN rec.tenant_id::UUID ELSE NULL END,
                rec.sensor_type,
                rec.value,
                rec.unit,
                rec.metadata,
                v_batch_id,
                rec.id
            );

            v_total_valid := v_total_valid + 1;
        ELSE
            UPDATE staging.sensor_readings_raw
            SET validation_status = 'INVALID',
                validation_errors = array_to_string(v_errors, '; '),
                processed_at = NOW(),
                batch_id = v_batch_id
            WHERE id = rec.id;

            v_total_invalid := v_total_invalid + 1;
        END IF;

        -- Log progreso cada 1000 registros
        IF v_total_processed % 1000 = 0 THEN
            RAISE NOTICE 'Procesados % registros (válidos: %, inválidos: %)',
                v_total_processed, v_total_valid, v_total_invalid;
        END IF;
    END LOOP;

    -- Crear resumen
    v_validation_summary := jsonb_build_object(
        'batch_id', v_batch_id,
        'total_processed', v_total_processed,
        'total_valid', v_total_valid,
        'total_invalid', v_total_invalid,
        'validation_rate',
            CASE WHEN v_total_processed > 0
                THEN ROUND((v_total_valid::NUMERIC / v_total_processed::NUMERIC) * 100, 2)
                ELSE 0
            END
    );

    RAISE NOTICE 'Validación completada: %', v_validation_summary;

    RETURN QUERY SELECT v_total_processed, v_total_valid, v_total_invalid, v_validation_summary;
END;
$$;

COMMENT ON FUNCTION staging.proc_validate_sensor_readings IS
'Valida datos crudos en staging.sensor_readings_raw y los mueve a staging.sensor_readings_validated';

-- =====================================================
-- 7. STORED PROCEDURE - Bulk Insert to Production
-- =====================================================

CREATE OR REPLACE FUNCTION staging.proc_migrate_staging_to_production(
    p_batch_id UUID,
    p_delete_staging_after BOOLEAN DEFAULT TRUE
)
RETURNS TABLE(
    inserted_count INT,
    duration_seconds INT,
    status VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_start_time TIMESTAMPTZ;
    v_end_time TIMESTAMPTZ;
    v_inserted_count INT := 0;
    v_duration_seconds INT;
    v_log_id BIGINT;
BEGIN
    v_start_time := clock_timestamp();

    RAISE NOTICE 'Iniciando migración de batch % a producción', p_batch_id;

    -- Crear registro en log
    INSERT INTO staging.bulk_import_log (
        batch_id, operation_type, source, status, executed_by
    ) VALUES (
        p_batch_id, 'MIGRATE', 'STAGING', 'RUNNING', current_user
    ) RETURNING id INTO v_log_id;

    -- Insertar datos validados en producción
    BEGIN
        WITH inserted AS (
            INSERT INTO iot.sensor_readings (
                time, sensor_id, greenhouse_id, tenant_id,
                sensor_type, value, unit, metadata
            )
            SELECT
                time, sensor_id, greenhouse_id, tenant_id,
                sensor_type, value, unit, metadata
            FROM staging.sensor_readings_validated
            WHERE batch_id = p_batch_id
            ON CONFLICT (time, sensor_id) DO NOTHING
            RETURNING 1
        )
        SELECT COUNT(*) INTO v_inserted_count FROM inserted;

        v_end_time := clock_timestamp();
        v_duration_seconds := EXTRACT(EPOCH FROM (v_end_time - v_start_time))::INT;

        -- Actualizar log con éxito
        UPDATE staging.bulk_import_log
        SET status = 'COMPLETED',
            successful_records = v_inserted_count,
            completed_at = v_end_time,
            duration_seconds = v_duration_seconds
        WHERE id = v_log_id;

        RAISE NOTICE 'Migración completada: % registros en % segundos',
            v_inserted_count, v_duration_seconds;

        -- Limpiar staging si se solicita
        IF p_delete_staging_after THEN
            DELETE FROM staging.sensor_readings_validated WHERE batch_id = p_batch_id;
            DELETE FROM staging.sensor_readings_raw WHERE batch_id = p_batch_id;
            RAISE NOTICE 'Datos de staging eliminados para batch %', p_batch_id;
        END IF;

        RETURN QUERY SELECT v_inserted_count, v_duration_seconds, 'COMPLETED'::VARCHAR;

    EXCEPTION WHEN OTHERS THEN
        -- Rollback y registrar error
        v_end_time := clock_timestamp();
        v_duration_seconds := EXTRACT(EPOCH FROM (v_end_time - v_start_time))::INT;

        UPDATE staging.bulk_import_log
        SET status = 'FAILED',
            error_message = SQLERRM,
            completed_at = v_end_time,
            duration_seconds = v_duration_seconds
        WHERE id = v_log_id;

        RAISE EXCEPTION 'Error en migración: %', SQLERRM;
    END;
END;
$$;

COMMENT ON FUNCTION staging.proc_migrate_staging_to_production IS
'Migra datos validados desde staging a producción con auditoría completa';

-- =====================================================
-- 8. STORED PROCEDURE - Cleanup Old Staging Data
-- =====================================================

CREATE OR REPLACE FUNCTION staging.proc_cleanup_staging(
    p_days_to_keep INT DEFAULT 7
)
RETURNS TABLE(
    deleted_raw_count INT,
    deleted_validated_count INT,
    deleted_logs_count INT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_deleted_raw INT := 0;
    v_deleted_validated INT := 0;
    v_deleted_logs INT := 0;
    v_cutoff_date TIMESTAMPTZ;
BEGIN
    v_cutoff_date := NOW() - (p_days_to_keep || ' days')::INTERVAL;

    RAISE NOTICE 'Limpiando datos de staging anteriores a %', v_cutoff_date;

    -- Eliminar datos raw procesados
    WITH deleted AS (
        DELETE FROM staging.sensor_readings_raw
        WHERE processed_at < v_cutoff_date
          AND validation_status IN ('VALID', 'INVALID')
        RETURNING 1
    )
    SELECT COUNT(*) INTO v_deleted_raw FROM deleted;

    -- Eliminar datos validados migrados
    WITH deleted AS (
        DELETE FROM staging.sensor_readings_validated v
        WHERE validated_at < v_cutoff_date
          AND EXISTS (
              SELECT 1 FROM staging.bulk_import_log l
              WHERE l.batch_id = v.batch_id
                AND l.status = 'COMPLETED'
          )
        RETURNING 1
    )
    SELECT COUNT(*) INTO v_deleted_validated FROM deleted;

    -- Archivar logs antiguos completados
    WITH deleted AS (
        DELETE FROM staging.bulk_import_log
        WHERE completed_at < v_cutoff_date
          AND status IN ('COMPLETED', 'FAILED')
        RETURNING 1
    )
    SELECT COUNT(*) INTO v_deleted_logs FROM deleted;

    RAISE NOTICE 'Limpieza completada: raw=%, validated=%, logs=%',
        v_deleted_raw, v_deleted_validated, v_deleted_logs;

    RETURN QUERY SELECT v_deleted_raw, v_deleted_validated, v_deleted_logs;
END;
$$;

COMMENT ON FUNCTION staging.proc_cleanup_staging IS
'Limpia datos antiguos de staging para mantener la base de datos eficiente';

-- =====================================================
-- 9. CONTINUOUS AGGREGATES - Hourly Sensor Readings
-- =====================================================

CREATE MATERIALIZED VIEW IF NOT EXISTS iot.sensor_readings_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) AS hour,
    sensor_id,
    greenhouse_id,
    tenant_id,
    sensor_type,
    COUNT(*) as reading_count,
    AVG(value) as avg_value,
    MIN(value) as min_value,
    MAX(value) as max_value,
    STDDEV(value) as stddev_value,
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY value) as median_value
FROM iot.sensor_readings
GROUP BY hour, sensor_id, greenhouse_id, tenant_id, sensor_type;

-- Refresh policy: actualizar cada hora
SELECT add_continuous_aggregate_policy('iot.sensor_readings_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE
);

COMMENT ON MATERIALIZED VIEW iot.sensor_readings_hourly IS
'Agregación continua por hora de lecturas de sensores con estadísticas';

-- =====================================================
-- 10. CONTINUOUS AGGREGATES - Daily by Tenant
-- =====================================================

CREATE MATERIALIZED VIEW IF NOT EXISTS iot.sensor_readings_daily_by_tenant
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', time) AS day,
    tenant_id,
    sensor_type,
    COUNT(*) as total_readings,
    COUNT(DISTINCT greenhouse_id) as unique_greenhouses,
    COUNT(DISTINCT sensor_id) as unique_sensors,
    AVG(value) as avg_value,
    MIN(value) as min_value,
    MAX(value) as max_value
FROM iot.sensor_readings
WHERE tenant_id IS NOT NULL
GROUP BY day, tenant_id, sensor_type;

-- Refresh policy: actualizar diariamente
SELECT add_continuous_aggregate_policy('iot.sensor_readings_daily_by_tenant',
    start_offset => INTERVAL '3 days',
    end_offset => INTERVAL '1 day',
    schedule_interval => INTERVAL '1 day',
    if_not_exists => TRUE
);

COMMENT ON MATERIALIZED VIEW iot.sensor_readings_daily_by_tenant IS
'Agregación diaria de lecturas por tenant para análisis multi-tenant';

-- =====================================================
-- VERIFICATION
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '================================================================';
    RAISE NOTICE 'STAGING INFRASTRUCTURE CREATED SUCCESSFULLY';
    RAISE NOTICE '================================================================';
    RAISE NOTICE 'Schemas creados:';
    RAISE NOTICE '  - staging (tablas staging e intermedias)';
    RAISE NOTICE '';
    RAISE NOTICE 'Tablas staging creadas:';
    RAISE NOTICE '  - staging.sensor_readings_raw (datos crudos)';
    RAISE NOTICE '  - staging.sensor_readings_validated (datos validados)';
    RAISE NOTICE '  - staging.bulk_import_log (auditoría)';
    RAISE NOTICE '  - staging.validation_rules (reglas configurables)';
    RAISE NOTICE '';
    RAISE NOTICE 'Procedimientos almacenados creados:';
    RAISE NOTICE '  - staging.proc_validate_sensor_readings() - Validar datos';
    RAISE NOTICE '  - staging.proc_migrate_staging_to_production() - Migrar a producción';
    RAISE NOTICE '  - staging.proc_cleanup_staging() - Limpiar datos antiguos';
    RAISE NOTICE '';
    RAISE NOTICE 'Continuous Aggregates creados:';
    RAISE NOTICE '  - iot.sensor_readings_hourly (agregación por hora)';
    RAISE NOTICE '  - iot.sensor_readings_daily_by_tenant (agregación diaria)';
    RAISE NOTICE '================================================================';
END $$;
