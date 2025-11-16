-- =====================================================
-- V11: CREATE STAGING INFRASTRUCTURE - PostgreSQL
-- =====================================================
-- Description: Tablas staging e intermedias para operaciones masivas
--              en PostgreSQL metadata (greenhouses, sensors, etc.)
-- Environment: ALL (DEV and PROD)
-- Database: greenhouse_metadata (PostgreSQL)
-- Author: Claude Code
-- Date: 2025-11-16
-- =====================================================

-- =====================================================
-- 1. CREATE STAGING SCHEMA (si no existe)
-- =====================================================

CREATE SCHEMA IF NOT EXISTS staging;

COMMENT ON SCHEMA staging IS 'Schema para tablas staging e intermedias de operaciones masivas en metadata';

-- =====================================================
-- 2. STAGING TABLE - Greenhouse Updates
-- =====================================================
-- Tabla para cambios masivos pendientes en greenhouses

CREATE TABLE IF NOT EXISTS staging.greenhouse_updates (
    id BIGSERIAL PRIMARY KEY,
    operation VARCHAR(20) NOT NULL,  -- INSERT, UPDATE, DELETE
    greenhouse_id UUID,
    tenant_id UUID,

    -- Datos del greenhouse (todos nullable para flexibilidad)
    name VARCHAR(255),
    greenhouse_code VARCHAR(50),
    mqtt_topic VARCHAR(255),
    mqtt_publish_interval_seconds INT,
    external_id VARCHAR(100),
    location JSONB,
    area_m2 DECIMAL(10,2),
    crop_type VARCHAR(50),
    timezone VARCHAR(50),
    is_active BOOLEAN,

    -- Metadata de staging
    batch_id UUID NOT NULL,
    submitted_at TIMESTAMPTZ DEFAULT NOW(),
    submitted_by VARCHAR(100),
    validation_status VARCHAR(20) DEFAULT 'PENDING',
    validation_errors TEXT,
    processed_at TIMESTAMPTZ,
    applied_to_production BOOLEAN DEFAULT FALSE,

    CONSTRAINT chk_greenhouse_operation CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    CONSTRAINT chk_greenhouse_validation CHECK (validation_status IN ('PENDING', 'VALID', 'INVALID', 'APPLIED'))
);

CREATE INDEX idx_staging_greenhouse_batch ON staging.greenhouse_updates(batch_id);
CREATE INDEX idx_staging_greenhouse_status ON staging.greenhouse_updates(validation_status);
CREATE INDEX idx_staging_greenhouse_processed ON staging.greenhouse_updates(applied_to_production);

COMMENT ON TABLE staging.greenhouse_updates IS 'Staging para actualizaciones masivas de greenhouses';

-- =====================================================
-- 3. STAGING TABLE - Sensor Calibrations
-- =====================================================
-- Tabla para calibraciones masivas de sensores

CREATE TABLE IF NOT EXISTS staging.sensor_calibrations (
    id BIGSERIAL PRIMARY KEY,
    sensor_id UUID NOT NULL,
    calibration_type VARCHAR(50) NOT NULL,  -- OFFSET, SCALE, RANGE_ADJUSTMENT

    -- Parámetros de calibración
    old_min_threshold DECIMAL(10,2),
    old_max_threshold DECIMAL(10,2),
    new_min_threshold DECIMAL(10,2),
    new_max_threshold DECIMAL(10,2),
    calibration_offset DECIMAL(10,4),
    calibration_scale DECIMAL(10,4),

    -- Metadata
    batch_id UUID NOT NULL,
    reason TEXT,
    submitted_at TIMESTAMPTZ DEFAULT NOW(),
    submitted_by VARCHAR(100),
    applied_at TIMESTAMPTZ,
    applied_to_production BOOLEAN DEFAULT FALSE,

    CONSTRAINT chk_calibration_type CHECK (calibration_type IN ('OFFSET', 'SCALE', 'RANGE_ADJUSTMENT', 'FULL_RECALIBRATION'))
);

CREATE INDEX idx_staging_calibration_batch ON staging.sensor_calibrations(batch_id);
CREATE INDEX idx_staging_calibration_sensor ON staging.sensor_calibrations(sensor_id);
CREATE INDEX idx_staging_calibration_applied ON staging.sensor_calibrations(applied_to_production);

COMMENT ON TABLE staging.sensor_calibrations IS 'Staging para calibraciones masivas de sensores';

-- =====================================================
-- 4. STAGING TABLE - Data Corrections
-- =====================================================
-- Tabla para correcciones masivas de datos históricos

CREATE TABLE IF NOT EXISTS staging.data_corrections (
    id BIGSERIAL PRIMARY KEY,
    correction_type VARCHAR(50) NOT NULL,  -- DELETE_RANGE, UPDATE_VALUES, RECALCULATE

    -- Filtros de selección
    target_table VARCHAR(100) NOT NULL,  -- sensor_readings, alerts, etc.
    tenant_id UUID,
    greenhouse_id UUID,
    sensor_id VARCHAR(50),
    time_from TIMESTAMPTZ,
    time_to TIMESTAMPTZ,

    -- Corrección a aplicar
    correction_sql TEXT,  -- SQL statement para la corrección
    correction_params JSONB,  -- Parámetros para la corrección
    estimated_affected_rows INT,

    -- Metadata
    batch_id UUID NOT NULL,
    reason TEXT NOT NULL,
    submitted_at TIMESTAMPTZ DEFAULT NOW(),
    submitted_by VARCHAR(100) NOT NULL,
    reviewed_by VARCHAR(100),
    reviewed_at TIMESTAMPTZ,
    approval_status VARCHAR(20) DEFAULT 'PENDING_REVIEW',
    applied_at TIMESTAMPTZ,
    actual_affected_rows INT,

    CONSTRAINT chk_correction_type CHECK (correction_type IN ('DELETE_RANGE', 'UPDATE_VALUES', 'RECALCULATE', 'BULK_DELETE')),
    CONSTRAINT chk_approval_status CHECK (approval_status IN ('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'APPLIED'))
);

CREATE INDEX idx_staging_correction_batch ON staging.data_corrections(batch_id);
CREATE INDEX idx_staging_correction_status ON staging.data_corrections(approval_status);
CREATE INDEX idx_staging_correction_target ON staging.data_corrections(target_table);

COMMENT ON TABLE staging.data_corrections IS 'Staging para correcciones masivas de datos con aprobación';

-- =====================================================
-- 5. OPERATION AUDIT LOG
-- =====================================================
-- Log completo de todas las operaciones de staging

CREATE TABLE IF NOT EXISTS staging.operation_audit_log (
    id BIGSERIAL PRIMARY KEY,
    operation_id UUID UNIQUE NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    target_schema VARCHAR(50),
    target_table VARCHAR(100),
    operation_description TEXT,

    -- Ejecución
    executed_by VARCHAR(100) NOT NULL,
    executed_at TIMESTAMPTZ DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    duration_ms BIGINT,

    -- Resultados
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    rows_affected INT,
    error_message TEXT,

    -- Rollback info
    rollback_available BOOLEAN DEFAULT FALSE,
    rollback_sql TEXT,
    rolled_back_at TIMESTAMPTZ,
    rolled_back_by VARCHAR(100),

    CONSTRAINT chk_operation_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'ROLLED_BACK'))
);

CREATE INDEX idx_audit_operation_id ON staging.operation_audit_log(operation_id);
CREATE INDEX idx_audit_executed_at ON staging.operation_audit_log(executed_at DESC);
CREATE INDEX idx_audit_status ON staging.operation_audit_log(status);
CREATE INDEX idx_audit_executed_by ON staging.operation_audit_log(executed_by);

COMMENT ON TABLE staging.operation_audit_log IS 'Auditoría completa de todas las operaciones de staging y producción';

-- =====================================================
-- 6. STORED PROCEDURE - Apply Greenhouse Updates
-- =====================================================

CREATE OR REPLACE FUNCTION staging.proc_apply_greenhouse_updates(
    p_batch_id UUID,
    p_dry_run BOOLEAN DEFAULT FALSE
)
RETURNS TABLE(
    operations_applied INT,
    inserts_count INT,
    updates_count INT,
    deletes_count INT,
    errors_count INT,
    status VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_operations_applied INT := 0;
    v_inserts INT := 0;
    v_updates INT := 0;
    v_deletes INT := 0;
    v_errors INT := 0;
    rec RECORD;
BEGIN
    RAISE NOTICE 'Aplicando actualizaciones de greenhouses para batch: %', p_batch_id;

    IF p_dry_run THEN
        RAISE NOTICE 'MODO DRY RUN - No se aplicarán cambios reales';
    END IF;

    -- Procesar cada operación pendiente validada
    FOR rec IN
        SELECT * FROM staging.greenhouse_updates
        WHERE batch_id = p_batch_id
          AND validation_status = 'VALID'
          AND applied_to_production = FALSE
        ORDER BY submitted_at
    LOOP
        BEGIN
            IF rec.operation = 'INSERT' THEN
                IF NOT p_dry_run THEN
                    INSERT INTO metadata.greenhouses (
                        id, tenant_id, name, greenhouse_code, mqtt_topic,
                        mqtt_publish_interval_seconds, external_id, location,
                        area_m2, crop_type, timezone, is_active,
                        created_at, updated_at
                    ) VALUES (
                        COALESCE(rec.greenhouse_id, gen_random_uuid()),
                        rec.tenant_id, rec.name, rec.greenhouse_code, rec.mqtt_topic,
                        rec.mqtt_publish_interval_seconds, rec.external_id, rec.location,
                        rec.area_m2, rec.crop_type, rec.timezone, COALESCE(rec.is_active, TRUE),
                        NOW(), NOW()
                    );
                END IF;
                v_inserts := v_inserts + 1;

            ELSIF rec.operation = 'UPDATE' THEN
                IF NOT p_dry_run THEN
                    UPDATE metadata.greenhouses SET
                        name = COALESCE(rec.name, name),
                        greenhouse_code = COALESCE(rec.greenhouse_code, greenhouse_code),
                        mqtt_topic = COALESCE(rec.mqtt_topic, mqtt_topic),
                        mqtt_publish_interval_seconds = COALESCE(rec.mqtt_publish_interval_seconds, mqtt_publish_interval_seconds),
                        external_id = COALESCE(rec.external_id, external_id),
                        location = COALESCE(rec.location, location),
                        area_m2 = COALESCE(rec.area_m2, area_m2),
                        crop_type = COALESCE(rec.crop_type, crop_type),
                        timezone = COALESCE(rec.timezone, timezone),
                        is_active = COALESCE(rec.is_active, is_active),
                        updated_at = NOW()
                    WHERE id = rec.greenhouse_id;
                END IF;
                v_updates := v_updates + 1;

            ELSIF rec.operation = 'DELETE' THEN
                IF NOT p_dry_run THEN
                    -- Soft delete: marcar como inactivo
                    UPDATE metadata.greenhouses
                    SET is_active = FALSE, updated_at = NOW()
                    WHERE id = rec.greenhouse_id;
                END IF;
                v_deletes := v_deletes + 1;
            END IF;

            v_operations_applied := v_operations_applied + 1;

            -- Marcar como aplicado
            IF NOT p_dry_run THEN
                UPDATE staging.greenhouse_updates
                SET applied_to_production = TRUE,
                    processed_at = NOW(),
                    validation_status = 'APPLIED'
                WHERE id = rec.id;
            END IF;

        EXCEPTION WHEN OTHERS THEN
            v_errors := v_errors + 1;
            RAISE WARNING 'Error aplicando operación ID %: %', rec.id, SQLERRM;

            IF NOT p_dry_run THEN
                UPDATE staging.greenhouse_updates
                SET validation_status = 'INVALID',
                    validation_errors = SQLERRM
                WHERE id = rec.id;
            END IF;
        END;
    END LOOP;

    RAISE NOTICE 'Operaciones completadas: total=%, inserts=%, updates=%, deletes=%, errors=%',
        v_operations_applied, v_inserts, v_updates, v_deletes, v_errors;

    RETURN QUERY SELECT
        v_operations_applied,
        v_inserts,
        v_updates,
        v_deletes,
        v_errors,
        CASE WHEN p_dry_run THEN 'DRY_RUN_COMPLETED' ELSE 'COMPLETED' END::VARCHAR;
END;
$$;

COMMENT ON FUNCTION staging.proc_apply_greenhouse_updates IS
'Aplica actualizaciones masivas de greenhouses desde staging a producción con dry-run option';

-- =====================================================
-- 7. STORED PROCEDURE - Apply Sensor Calibrations
-- =====================================================

CREATE OR REPLACE FUNCTION staging.proc_apply_sensor_calibrations(
    p_batch_id UUID
)
RETURNS TABLE(
    calibrations_applied INT,
    sensors_updated INT,
    status VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_calibrations_applied INT := 0;
    v_sensors_updated INT := 0;
    rec RECORD;
BEGIN
    RAISE NOTICE 'Aplicando calibraciones de sensores para batch: %', p_batch_id;

    FOR rec IN
        SELECT * FROM staging.sensor_calibrations
        WHERE batch_id = p_batch_id
          AND applied_to_production = FALSE
        ORDER BY submitted_at
    LOOP
        -- Aplicar calibración según tipo
        IF rec.calibration_type = 'RANGE_ADJUSTMENT' THEN
            UPDATE metadata.sensors
            SET min_threshold = rec.new_min_threshold,
                max_threshold = rec.new_max_threshold,
                updated_at = NOW()
            WHERE id = rec.sensor_id;

        ELSIF rec.calibration_type = 'OFFSET' THEN
            -- Actualizar offset en metadata del sensor
            UPDATE metadata.sensors
            SET metadata = COALESCE(metadata, '{}'::JSONB) ||
                jsonb_build_object('calibration_offset', rec.calibration_offset),
                updated_at = NOW()
            WHERE id = rec.sensor_id;

        ELSIF rec.calibration_type = 'SCALE' THEN
            UPDATE metadata.sensors
            SET metadata = COALESCE(metadata, '{}'::JSONB) ||
                jsonb_build_object('calibration_scale', rec.calibration_scale),
                updated_at = NOW()
            WHERE id = rec.sensor_id;
        END IF;

        -- Marcar calibración como aplicada
        UPDATE staging.sensor_calibrations
        SET applied_to_production = TRUE,
            applied_at = NOW()
        WHERE id = rec.id;

        v_calibrations_applied := v_calibrations_applied + 1;
        v_sensors_updated := v_sensors_updated + 1;
    END LOOP;

    RAISE NOTICE 'Calibraciones aplicadas: %, sensores actualizados: %',
        v_calibrations_applied, v_sensors_updated;

    RETURN QUERY SELECT v_calibrations_applied, v_sensors_updated, 'COMPLETED'::VARCHAR;
END;
$$;

COMMENT ON FUNCTION staging.proc_apply_sensor_calibrations IS
'Aplica calibraciones masivas de sensores desde staging a metadata';

-- =====================================================
-- 8. STORED PROCEDURE - Rollback Operation
-- =====================================================

CREATE OR REPLACE FUNCTION staging.proc_rollback_operation(
    p_operation_id UUID
)
RETURNS TABLE(
    operation_id UUID,
    rollback_status VARCHAR,
    message TEXT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_audit_rec RECORD;
    v_rollback_result TEXT;
BEGIN
    -- Buscar operación en audit log
    SELECT * INTO v_audit_rec
    FROM staging.operation_audit_log oal
    WHERE oal.operation_id = p_operation_id;

    IF NOT FOUND THEN
        RETURN QUERY SELECT
            p_operation_id,
            'NOT_FOUND'::VARCHAR,
            'Operación no encontrada en audit log'::TEXT;
        RETURN;
    END IF;

    IF v_audit_rec.status = 'ROLLED_BACK' THEN
        RETURN QUERY SELECT
            p_operation_id,
            'ALREADY_ROLLED_BACK'::VARCHAR,
            'Operación ya fue revertida anteriormente'::TEXT;
        RETURN;
    END IF;

    IF NOT v_audit_rec.rollback_available OR v_audit_rec.rollback_sql IS NULL THEN
        RETURN QUERY SELECT
            p_operation_id,
            'NO_ROLLBACK_AVAILABLE'::VARCHAR,
            'Esta operación no tiene rollback disponible'::TEXT;
        RETURN;
    END IF;

    -- Ejecutar rollback
    BEGIN
        EXECUTE v_audit_rec.rollback_sql;

        -- Actualizar audit log
        UPDATE staging.operation_audit_log oal
        SET status = 'ROLLED_BACK',
            rolled_back_at = NOW(),
            rolled_back_by = current_user
        WHERE oal.operation_id = p_operation_id;

        v_rollback_result := 'Rollback ejecutado exitosamente';

        RETURN QUERY SELECT
            p_operation_id,
            'SUCCESS'::VARCHAR,
            v_rollback_result;

    EXCEPTION WHEN OTHERS THEN
        RETURN QUERY SELECT
            p_operation_id,
            'FAILED'::VARCHAR,
            ('Error en rollback: ' || SQLERRM)::TEXT;
    END;
END;
$$;

COMMENT ON FUNCTION staging.proc_rollback_operation IS
'Revierte una operación usando el SQL de rollback almacenado en audit log';

-- =====================================================
-- 9. VIEW - Staging Operations Summary
-- =====================================================

-- Vista para PostgreSQL - solo tablas de metadata staging
CREATE OR REPLACE VIEW staging.v_operations_summary AS
SELECT
    'greenhouse_updates' as staging_table,
    COUNT(*) as total_records,
    COUNT(*) FILTER (WHERE validation_status = 'PENDING') as pending,
    COUNT(*) FILTER (WHERE validation_status = 'VALID') as valid,
    COUNT(*) FILTER (WHERE validation_status = 'INVALID') as invalid,
    COUNT(DISTINCT batch_id) as unique_batches,
    MIN(submitted_at) as oldest_record,
    MAX(submitted_at) as newest_record
FROM staging.greenhouse_updates

UNION ALL

SELECT
    'sensor_calibrations',
    COUNT(*),
    COUNT(*) FILTER (WHERE applied_to_production = FALSE),
    COUNT(*) FILTER (WHERE applied_to_production = TRUE),
    0,
    COUNT(DISTINCT batch_id),
    MIN(submitted_at),
    MAX(submitted_at)
FROM staging.sensor_calibrations

UNION ALL

SELECT
    'data_corrections',
    COUNT(*),
    COUNT(*) FILTER (WHERE approval_status = 'PENDING_REVIEW'),
    COUNT(*) FILTER (WHERE approval_status = 'APPROVED'),
    COUNT(*) FILTER (WHERE approval_status = 'REJECTED'),
    COUNT(DISTINCT batch_id),
    MIN(submitted_at),
    MAX(submitted_at)
FROM staging.data_corrections;

COMMENT ON VIEW staging.v_operations_summary IS
'Vista resumen del estado de todas las operaciones en staging';

-- =====================================================
-- 10. GRANT PERMISSIONS (optional, adjust as needed)
-- =====================================================

-- Grant usage on staging schema to application role
-- GRANT USAGE ON SCHEMA staging TO app_role;
-- GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA staging TO app_role;
-- GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA staging TO app_role;

-- =====================================================
-- VERIFICATION
-- =====================================================

DO $$
DECLARE
    v_tables_count INT;
    v_functions_count INT;
BEGIN
    -- Contar tablas creadas
    SELECT COUNT(*) INTO v_tables_count
    FROM information_schema.tables
    WHERE table_schema = 'staging';

    -- Contar funciones creadas
    SELECT COUNT(*) INTO v_functions_count
    FROM information_schema.routines
    WHERE routine_schema = 'staging' AND routine_type = 'FUNCTION';

    RAISE NOTICE '================================================================';
    RAISE NOTICE 'STAGING INFRASTRUCTURE (PostgreSQL) CREATED SUCCESSFULLY';
    RAISE NOTICE '================================================================';
    RAISE NOTICE 'Tablas staging creadas: %', v_tables_count;
    RAISE NOTICE '  - staging.greenhouse_updates (actualizaciones masivas)';
    RAISE NOTICE '  - staging.sensor_calibrations (calibraciones)';
    RAISE NOTICE '  - staging.data_corrections (correcciones con aprobación)';
    RAISE NOTICE '  - staging.operation_audit_log (auditoría completa)';
    RAISE NOTICE '';
    RAISE NOTICE 'Procedimientos almacenados: %', v_functions_count;
    RAISE NOTICE '  - proc_apply_greenhouse_updates() - Aplicar actualizaciones';
    RAISE NOTICE '  - proc_apply_sensor_calibrations() - Aplicar calibraciones';
    RAISE NOTICE '  - proc_rollback_operation() - Revertir operación';
    RAISE NOTICE '';
    RAISE NOTICE 'Vistas creadas:';
    RAISE NOTICE '  - v_operations_summary - Resumen de operaciones staging';
    RAISE NOTICE '================================================================';
END $$;
