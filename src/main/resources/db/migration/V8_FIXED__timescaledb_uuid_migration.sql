-- ⚠️⚠️⚠️ MIGRACIÓN CRÍTICA - TIMESCALEDB CON COMPRESIÓN ⚠️⚠️⚠️
-- Cambiar tipo de greenhouse_id de VARCHAR(50) a UUID en sensor_readings
-- Añadir tenant_id para optimizar queries multi-tenant
--
-- ⚠️ SOLUCIÓN PARA HYPERTABLES CON COMPRESIÓN HABILITADA
-- ⚠️ IMPORTANTE: REQUIERE BACKUP COMPLETO ANTES DE EJECUTAR
-- ⚠️ IMPORTANTE: PUEDE REQUERIR DOWNTIME (depende del volumen de datos)
--
-- Este script maneja correctamente hypertables comprimidas:
-- 1. Descomprime todos los chunks
-- 2. Deshabilita compresión
-- 3. Hace la migración de tipos
-- 4. Re-habilita compresión

-- =============================================================================
-- CONFIGURACIÓN: Ajustar según el entorno
-- =============================================================================
-- Para DEV: schema=public, Para PROD: schema=iot
\set SCHEMA 'public'

-- =============================================================================
-- PASO 0: PRE-VALIDACIONES
-- =============================================================================

DO $$
DECLARE
    v_total_records BIGINT;
    v_null_greenhouse_ids INT;
    v_schema TEXT := :'SCHEMA';
    v_table_name TEXT := v_schema || '.sensor_readings';
BEGIN
    EXECUTE format('SELECT COUNT(*) FROM %I.sensor_readings', v_schema) INTO v_total_records;
    RAISE NOTICE 'Total de registros en sensor_readings: %', v_total_records;

    EXECUTE format('SELECT COUNT(*) FROM %I.sensor_readings WHERE greenhouse_id IS NULL', v_schema)
        INTO v_null_greenhouse_ids;

    IF v_null_greenhouse_ids > 0 THEN
        RAISE EXCEPTION 'CRÍTICO: Hay % registros con greenhouse_id NULL', v_null_greenhouse_ids;
    END IF;

    RAISE NOTICE 'OK: Validaciones pre-migración completadas';
END $$;

-- =============================================================================
-- PASO 1: DESCOMPRIMIR TODOS LOS CHUNKS
-- =============================================================================

DO $$
DECLARE
    v_chunk_name TEXT;
    v_decompressed_count INT := 0;
    v_schema TEXT := :'SCHEMA';
BEGIN
    RAISE NOTICE 'Iniciando descompresión de chunks...';

    FOR v_chunk_name IN
        SELECT chunk_name::TEXT
        FROM timescaledb_information.chunks
        WHERE hypertable_name = 'sensor_readings'
          AND hypertable_schema = v_schema
          AND is_compressed = true
    LOOP
        EXECUTE format('SELECT decompress_chunk(%L)', v_schema || '.' || v_chunk_name);
        v_decompressed_count := v_decompressed_count + 1;

        IF v_decompressed_count % 10 = 0 THEN
            RAISE NOTICE 'Descomprimidos % chunks...', v_decompressed_count;
        END IF;
    END LOOP;

    RAISE NOTICE 'Descompresión completada: % chunks procesados', v_decompressed_count;
END $$;

-- =============================================================================
-- PASO 2: DESHABILITAR COMPRESIÓN
-- =============================================================================

DO $$
DECLARE
    v_schema TEXT := :'SCHEMA';
BEGIN
    RAISE NOTICE 'Deshabilitando compresión en sensor_readings...';

    EXECUTE format('ALTER TABLE %I.sensor_readings SET (timescaledb.compress = false)', v_schema);

    -- Eliminar política de compresión si existe
    PERFORM remove_compression_policy(format('%I.sensor_readings', v_schema)::regclass, if_exists => true);

    RAISE NOTICE 'Compresión deshabilitada exitosamente';
END $$;

-- =============================================================================
-- PASO 3: AÑADIR COLUMNA tenant_id
-- =============================================================================

DO $$
DECLARE
    v_schema TEXT := :'SCHEMA';
BEGIN
    EXECUTE format('ALTER TABLE %I.sensor_readings ADD COLUMN IF NOT EXISTS tenant_id UUID', v_schema);
    RAISE NOTICE 'Columna tenant_id agregada';
END $$;

-- =============================================================================
-- PASO 4: CONVERSIÓN CRÍTICA - greenhouse_id VARCHAR → UUID
-- =============================================================================

DO $$
DECLARE
    v_start_time TIMESTAMP;
    v_end_time TIMESTAMP;
    v_duration INTERVAL;
    v_schema TEXT := :'SCHEMA';
BEGIN
    v_start_time := clock_timestamp();
    RAISE NOTICE 'Iniciando conversión de greenhouse_id VARCHAR → UUID...';
    RAISE NOTICE 'Hora de inicio: %', v_start_time;

    EXECUTE format('ALTER TABLE %I.sensor_readings ALTER COLUMN greenhouse_id TYPE UUID USING greenhouse_id::UUID', v_schema);

    v_end_time := clock_timestamp();
    v_duration := v_end_time - v_start_time;

    RAISE NOTICE 'Conversión completada exitosamente!';
    RAISE NOTICE 'Hora de fin: %', v_end_time;
    RAISE NOTICE 'Duración: %', v_duration;
END $$;

-- =============================================================================
-- PASO 5: HACER greenhouse_id NOT NULL
-- =============================================================================

DO $$
DECLARE
    v_schema TEXT := :'SCHEMA';
BEGIN
    EXECUTE format('ALTER TABLE %I.sensor_readings ALTER COLUMN greenhouse_id SET NOT NULL', v_schema);
    RAISE NOTICE 'greenhouse_id marcado como NOT NULL';
END $$;

-- =============================================================================
-- PASO 6: CREAR ÍNDICES OPTIMIZADOS
-- =============================================================================

DO $$
DECLARE
    v_schema TEXT := :'SCHEMA';
BEGIN
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_sensor_readings_tenant_time
        ON %I.sensor_readings(tenant_id, time DESC) WHERE tenant_id IS NOT NULL', v_schema);

    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_sensor_readings_greenhouse_sensor_time
        ON %I.sensor_readings(greenhouse_id, sensor_id, time DESC)', v_schema);

    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_sensor_readings_greenhouse_time
        ON %I.sensor_readings(greenhouse_id, time DESC)', v_schema);

    RAISE NOTICE 'Índices multi-tenant creados';
END $$;

-- =============================================================================
-- PASO 7: RE-HABILITAR COMPRESIÓN
-- =============================================================================

DO $$
DECLARE
    v_schema TEXT := :'SCHEMA';
BEGIN
    RAISE NOTICE 'Re-habilitando compresión...';

    EXECUTE format('ALTER TABLE %I.sensor_readings SET (
        timescaledb.compress,
        timescaledb.compress_segmentby = ''greenhouse_id, sensor_id'',
        timescaledb.compress_orderby = ''time DESC''
    )', v_schema);

    -- Re-crear política de compresión (comprimir datos > 7 días)
    PERFORM add_compression_policy(
        format('%I.sensor_readings', v_schema)::regclass,
        INTERVAL '7 days',
        if_not_exists => true
    );

    RAISE NOTICE 'Compresión re-habilitada con nuevas columnas';
END $$;

-- =============================================================================
-- PASO 8: ACTUALIZAR ESTADÍSTICAS
-- =============================================================================

DO $$
DECLARE
    v_schema TEXT := :'SCHEMA';
BEGIN
    EXECUTE format('ANALYZE %I.sensor_readings', v_schema);
    RAISE NOTICE 'Estadísticas actualizadas (ANALYZE completado)';
END $$;

-- =============================================================================
-- PASO 9: VERIFICACIÓN FINAL
-- =============================================================================

DO $$
DECLARE
    v_greenhouse_type TEXT;
    v_tenant_exists BOOLEAN;
    v_compression_enabled BOOLEAN;
    v_schema TEXT := :'SCHEMA';
BEGIN
    -- Verificar tipo de greenhouse_id
    SELECT data_type INTO v_greenhouse_type
    FROM information_schema.columns
    WHERE table_schema = v_schema
      AND table_name = 'sensor_readings'
      AND column_name = 'greenhouse_id';

    IF v_greenhouse_type != 'uuid' THEN
        RAISE EXCEPTION 'ERROR: greenhouse_id NO es UUID, es: %', v_greenhouse_type;
    END IF;

    -- Verificar que tenant_id existe
    SELECT EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = v_schema
          AND table_name = 'sensor_readings'
          AND column_name = 'tenant_id'
    ) INTO v_tenant_exists;

    IF NOT v_tenant_exists THEN
        RAISE EXCEPTION 'ERROR: Columna tenant_id NO fue creada';
    END IF;

    -- Verificar compresión re-habilitada
    SELECT compression_enabled INTO v_compression_enabled
    FROM timescaledb_information.hypertables
    WHERE hypertable_schema = v_schema
      AND hypertable_name = 'sensor_readings';

    IF NOT v_compression_enabled THEN
        RAISE WARNING 'ADVERTENCIA: Compresión NO fue re-habilitada correctamente';
    END IF;

    RAISE NOTICE '==================================================================';
    RAISE NOTICE 'MIGRACIÓN TIMESCALEDB COMPLETADA EXITOSAMENTE';
    RAISE NOTICE '==================================================================';
    RAISE NOTICE 'Cambios aplicados:';
    RAISE NOTICE '  1. greenhouse_id: VARCHAR(50) → UUID ✓';
    RAISE NOTICE '  2. greenhouse_id: NULL → NOT NULL ✓';
    RAISE NOTICE '  3. tenant_id: Columna añadida (UUID nullable) ✓';
    RAISE NOTICE '  4. Índices multi-tenant creados ✓';
    RAISE NOTICE '  5. Compresión re-habilitada ✓';
    RAISE NOTICE '';
    RAISE NOTICE 'IMPORTANTE: Pasos pendientes POST-migración:';
    RAISE NOTICE '  1. Popular tenant_id mediante script de aplicación';
    RAISE NOTICE '  2. Verificar queries de aplicación funcionan correctamente';
    RAISE NOTICE '  3. Monitorear performance con nuevos índices';
    RAISE NOTICE '==================================================================';
END $$;
