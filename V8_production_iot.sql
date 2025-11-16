-- ⚠️⚠️⚠️ MIGRACIÓN CRÍTICA - TIMESCALEDB ⚠️⚠️⚠️
-- Cambiar tipo de greenhouse_id de VARCHAR(50) a UUID en sensor_readings
-- Añadir tenant_id para optimizar queries multi-tenant
--
-- ⚠️ IMPORTANTE: REQUIERE BACKUP COMPLETO ANTES DE EJECUTAR
-- ⚠️ IMPORTANTE: PUEDE REQUERIR DOWNTIME (depende del volumen de datos)
-- ⚠️ IMPORTANTE: VERIFICAR EN STAGING PRIMERO
--
-- Este script modifica la tabla sensor_readings que puede tener MILLONES de registros
-- En producción, considerar hacer la migración en ventana de mantenimiento

-- =============================================================================
-- PASO 0: PRE-VALIDACIONES OBLIGATORIAS
-- =============================================================================

DO $$
DECLARE
    v_total_records BIGINT;
    v_null_greenhouse_ids INT;
    v_invalid_uuids INT;
BEGIN
    -- Contar registros totales
    SELECT COUNT(*) INTO v_total_records FROM iot.sensor_readings;
    RAISE NOTICE 'Total de registros en sensor_readings: %', v_total_records;

    -- Verificar registros sin greenhouse_id
    SELECT COUNT(*) INTO v_null_greenhouse_ids
    FROM iot.sensor_readings
    WHERE greenhouse_id IS NULL;

    IF v_null_greenhouse_ids > 0 THEN
        RAISE EXCEPTION 'CRÍTICO: Hay % registros con greenhouse_id NULL. Debe corregirse antes de migrar.', v_null_greenhouse_ids;
    END IF;

    -- Verificar que todos los greenhouse_id son UUIDs válidos
    SELECT COUNT(*) INTO v_invalid_uuids
    FROM iot.sensor_readings
    WHERE greenhouse_id::TEXT !~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';

    IF v_invalid_uuids > 0 THEN
        RAISE WARNING 'ADVERTENCIA: Hay % registros con greenhouse_id que no son UUIDs válidos', v_invalid_uuids;
        RAISE NOTICE 'Se intentará convertir. Si falla, revisar datos manualmente.';
    ELSE
        RAISE NOTICE 'OK: Todos los greenhouse_id parecen ser UUIDs válidos';
    END IF;
END $$;

-- =============================================================================
-- PASO 1: Añadir columna temporal para tenant_id
-- =============================================================================

-- Añadir tenant_id como nullable primero
ALTER TABLE iot.sensor_readings
ADD COLUMN IF NOT EXISTS tenant_id UUID;

-- Columna tenant_id añadida a sensor_readings

-- =============================================================================
-- PASO 2: Popular tenant_id basado en greenhouse_id
-- =============================================================================

-- Esto requiere conexión a metadata DB mediante dblink o hacerlo en la aplicación
-- Por ahora, crearemos un UUID por defecto para el tenant DEFAULT

DO $$
DECLARE
    v_default_tenant_id UUID;
    v_updated_records BIGINT;
BEGIN
    -- IMPORTANTE: Este tenant_id debe coincidir con el tenant DEFAULT de metadata DB
    -- Por ahora usamos NULL, se actualizará después desde la aplicación
    -- o mediante script Python/Kotlin que conecte ambas DBs

    RAISE NOTICE 'tenant_id se dejará NULL temporalmente';
    RAISE NOTICE 'Debe actualizarse posteriormente mediante script de aplicación';
END $$;

-- =============================================================================
-- PASO 3: CAMBIO CRÍTICO - Convertir greenhouse_id de VARCHAR a UUID
-- =============================================================================

-- ⚠️ ESTA ES LA OPERACIÓN MÁS CRÍTICA
-- Puede tardar varios minutos/horas dependiendo del volumen de datos

DO $$
DECLARE
    v_start_time TIMESTAMP;
    v_end_time TIMESTAMP;
    v_duration INTERVAL;
BEGIN
    v_start_time := clock_timestamp();
    RAISE NOTICE 'Iniciando conversión de greenhouse_id VARCHAR → UUID...';
    RAISE NOTICE 'Hora de inicio: %', v_start_time;

    -- Cambiar tipo de columna usando USING para conversión explícita
    ALTER TABLE iot.sensor_readings
        ALTER COLUMN greenhouse_id TYPE UUID USING greenhouse_id::UUID;

    v_end_time := clock_timestamp();
    v_duration := v_end_time - v_start_time;

    RAISE NOTICE 'Conversión completada exitosamente!';
    RAISE NOTICE 'Hora de fin: %', v_end_time;
    RAISE NOTICE 'Duración: %', v_duration;
END $$;

-- =============================================================================
-- PASO 4: Hacer greenhouse_id NOT NULL
-- =============================================================================

ALTER TABLE iot.sensor_readings
    ALTER COLUMN greenhouse_id SET NOT NULL;

-- greenhouse_id marcado como NOT NULL

-- =============================================================================
-- PASO 5: Crear índices optimizados para queries multi-tenant
-- =============================================================================

-- Índice para queries por tenant y tiempo (muy común en dashboards)
CREATE INDEX IF NOT EXISTS idx_sensor_readings_tenant_time
    ON iot.sensor_readings(tenant_id, time DESC)
    WHERE tenant_id IS NOT NULL;

-- Índice compuesto para queries por greenhouse, sensor y tiempo
CREATE INDEX IF NOT EXISTS idx_sensor_readings_greenhouse_sensor_time
    ON iot.sensor_readings(greenhouse_id, sensor_id, time DESC);

-- Índice para queries solo por greenhouse
CREATE INDEX IF NOT EXISTS idx_sensor_readings_greenhouse_time
    ON iot.sensor_readings(greenhouse_id, time DESC);

-- Índices multi-tenant creados

-- =============================================================================
-- PASO 6: Actualizar estadísticas de la tabla para el query planner
-- =============================================================================

ANALYZE iot.sensor_readings;

RAISE NOTICE 'Estadísticas de tabla actualizadas (ANALYZE)';

-- =============================================================================
-- PASO 7: Verificaciones post-migración
-- =============================================================================

DO $$
DECLARE
    v_total_records BIGINT;
    v_greenhouse_id_type TEXT;
    v_tenant_id_count BIGINT;
BEGIN
    -- Verificar tipo de columna
    SELECT data_type INTO v_greenhouse_id_type
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'sensor_readings'
      AND column_name = 'greenhouse_id';

    IF v_greenhouse_id_type = 'uuid' THEN
        RAISE NOTICE 'OK: greenhouse_id es tipo UUID';
    ELSE
        RAISE EXCEPTION 'ERROR: greenhouse_id NO es UUID, es: %', v_greenhouse_id_type;
    END IF;

    -- Contar registros totales
    SELECT COUNT(*) INTO v_total_records FROM iot.sensor_readings;
    RAISE NOTICE 'Total de registros después de migración: %', v_total_records;

    -- Verificar si hay tenant_id poblados
    SELECT COUNT(*) INTO v_tenant_id_count
    FROM iot.sensor_readings
    WHERE tenant_id IS NOT NULL;

    RAISE NOTICE 'Registros con tenant_id: % de %', v_tenant_id_count, v_total_records;
END $$;

-- =============================================================================
-- PASO 8: Resumen de migración
-- =============================================================================

DO $$
BEGIN
    RAISE NOTICE '==================================================================';
    RAISE NOTICE 'MIGRACIÓN TIMESCALEDB COMPLETADA EXITOSAMENTE';
    RAISE NOTICE '==================================================================';
    RAISE NOTICE 'Cambios aplicados:';
    RAISE NOTICE '  1. greenhouse_id: VARCHAR(50) → UUID ✓';
    RAISE NOTICE '  2. greenhouse_id: NULL → NOT NULL ✓';
    RAISE NOTICE '  3. tenant_id: Columna añadida (UUID nullable) ✓';
    RAISE NOTICE '  4. Índices multi-tenant creados ✓';
    RAISE NOTICE '';
    RAISE NOTICE 'IMPORTANTE: Pasos pendientes POST-migración:';
    RAISE NOTICE '  1. Popular tenant_id mediante script de aplicación';
    RAISE NOTICE '  2. Verificar queries de aplicación funcionan correctamente';
    RAISE NOTICE '  3. Monitorear performance de queries con nuevos índices';
    RAISE NOTICE '==================================================================';
END $$;

-- Queries útiles para verificación manual:
--
-- Ver estructura actual de la tabla:
-- SELECT column_name, data_type, character_maximum_length, is_nullable
-- FROM information_schema.columns
-- WHERE table_schema = 'public' AND table_name = 'sensor_readings'
-- ORDER BY ordinal_position;
--
-- Ver índices de la tabla:
-- SELECT indexname, indexdef
-- FROM pg_indexes
-- WHERE schemaname = 'public' AND tablename = 'sensor_readings';
--
-- Ver tamaño de la tabla:
-- SELECT
--     pg_size_pretty(pg_total_relation_size('iot.sensor_readings')) as total_size,
--     pg_size_pretty(pg_relation_size('iot.sensor_readings')) as table_size,
--     pg_size_pretty(pg_total_relation_size('iot.sensor_readings') - pg_relation_size('iot.sensor_readings')) as indexes_size;
