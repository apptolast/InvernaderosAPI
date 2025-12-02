-- Índices optimizados para sistema multi-tenant
-- Añade índices adicionales en todas las tablas para mejorar performance
-- de queries comunes en arquitectura multi-tenant
--
-- Este script complementa los índices básicos creados en migraciones anteriores
-- con índices compuestos y parciales para casos de uso específicos

-- =============================================================================
-- ÍNDICES EN TABLA TENANTS (metadata.tenants)
-- =============================================================================

-- Búsqueda por estado activo (muy común en dashboards)
CREATE INDEX IF NOT EXISTS idx_tenants_active
    ON metadata.tenants(is_active)
    WHERE is_active = true;

-- Búsqueda por nombre (autocomplete, búsquedas)
CREATE INDEX IF NOT EXISTS idx_tenants_name_trgm
    ON metadata.tenants USING gin(name gin_trgm_ops);

-- Búsqueda por email (login, validación)
CREATE INDEX IF NOT EXISTS idx_tenants_email_lower
    ON metadata.tenants(LOWER(email));

-- =============================================================================
-- ÍNDICES EN TABLA GREENHOUSES (metadata.greenhouses)
-- =============================================================================

-- Query común: greenhouses activos de un tenant
CREATE INDEX IF NOT EXISTS idx_greenhouses_tenant_active
    ON metadata.greenhouses(tenant_id, is_active)
    WHERE is_active = true;

-- Query común: greenhouses por tipo de cultivo
CREATE INDEX IF NOT EXISTS idx_greenhouses_crop_type
    ON metadata.greenhouses(crop_type)
    WHERE crop_type IS NOT NULL;

-- Búsqueda geográfica (si se usa coordinates JSONB)
CREATE INDEX IF NOT EXISTS idx_greenhouses_location_gin
    ON metadata.greenhouses USING gin(location jsonb_path_ops);

-- =============================================================================
-- ÍNDICES EN TABLA SENSORS (metadata.sensors)
-- =============================================================================

-- Query común: sensores activos de un tenant
CREATE INDEX IF NOT EXISTS idx_sensors_tenant_active
    ON metadata.sensors(tenant_id, is_active)
    WHERE is_active = true;

-- Query común: sensores activos de un greenhouse
CREATE INDEX IF NOT EXISTS idx_sensors_greenhouse_active
    ON metadata.sensors(greenhouse_id, is_active)
    WHERE is_active = true;

-- Query común: sensores por tipo
CREATE INDEX IF NOT EXISTS idx_sensors_type_active
    ON metadata.sensors(sensor_type, is_active)
    WHERE is_active = true;

-- Query para validar umbrales (alertas)
CREATE INDEX IF NOT EXISTS idx_sensors_thresholds
    ON metadata.sensors(greenhouse_id, min_threshold, max_threshold)
    WHERE min_threshold IS NOT NULL OR max_threshold IS NOT NULL;

-- Última vez visto (monitoreo de conectividad)
CREATE INDEX IF NOT EXISTS idx_sensors_last_seen
    ON metadata.sensors(last_seen DESC NULLS LAST)
    WHERE is_active = true;

-- =============================================================================
-- ÍNDICES EN TABLA ACTUATORS (metadata.actuators)
-- =============================================================================

-- Query común: actuadores activos de un tenant
CREATE INDEX IF NOT EXISTS idx_actuators_tenant_active
    ON metadata.actuators(tenant_id, is_active)
    WHERE is_active = true;

-- Query común: actuadores activos de un greenhouse
CREATE INDEX IF NOT EXISTS idx_actuators_greenhouse_active
    ON metadata.actuators(greenhouse_id, is_active)
    WHERE is_active = true;

-- Query común: actuadores por estado actual
CREATE INDEX IF NOT EXISTS idx_actuators_state
    ON metadata.actuators(current_state)
    WHERE is_active = true;

-- Última actualización de estado (monitoreo)
CREATE INDEX IF NOT EXISTS idx_actuators_last_update
    ON metadata.actuators(last_status_update DESC NULLS LAST)
    WHERE is_active = true;

-- =============================================================================
-- ÍNDICES EN TABLA USERS (metadata.users)
-- =============================================================================

-- Query común: usuarios activos de un tenant
CREATE INDEX IF NOT EXISTS idx_users_tenant_active
    ON metadata.users(tenant_id, is_active)
    WHERE is_active = true;

-- Login por username (case-insensitive)
CREATE INDEX IF NOT EXISTS idx_users_username_lower
    ON metadata.users(LOWER(username));

-- Login por email (case-insensitive)
CREATE INDEX IF NOT EXISTS idx_users_email_lower
    ON metadata.users(LOWER(email));

-- Query común: usuarios por rol
CREATE INDEX IF NOT EXISTS idx_users_tenant_role
    ON metadata.users(tenant_id, role)
    WHERE is_active = true;

-- Último login (auditoría, seguridad)
CREATE INDEX IF NOT EXISTS idx_users_last_login
    ON metadata.users(last_login DESC NULLS LAST)
    WHERE is_active = true;

-- =============================================================================
-- ÍNDICES EN TABLA MQTT_USERS (metadata.mqtt_users)
-- =============================================================================

-- Query común: mqtt users activos de un tenant
CREATE INDEX IF NOT EXISTS idx_mqtt_users_tenant_active
    ON metadata.mqtt_users(tenant_id, is_active)
    WHERE is_active = true;

-- Última conexión (monitoreo de dispositivos)
CREATE INDEX IF NOT EXISTS idx_mqtt_users_last_connected
    ON metadata.mqtt_users(last_connected_at DESC)
    WHERE is_active = true;

-- Por tipo de dispositivo
CREATE INDEX IF NOT EXISTS idx_mqtt_users_device_type
    ON metadata.mqtt_users(device_type)
    WHERE is_active = true;

-- =============================================================================
-- OPTIMIZACIONES FINALES
-- =============================================================================

-- Actualizar estadísticas de todas las tablas para el query planner
ANALYZE metadata.tenants;
ANALYZE metadata.greenhouses;
ANALYZE metadata.sensors;
ANALYZE metadata.actuators;
ANALYZE metadata.users;
ANALYZE metadata.mqtt_users;

-- Habilitar extension pg_trgm si no está habilitada (para búsqueda de texto)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Habilitar extension pg_stat_statements si no está habilitada (para monitoreo)
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- =============================================================================
-- RESUMEN Y VERIFICACIÓN
-- =============================================================================

DO $$
DECLARE
    v_index_count INT;
    v_table_stats RECORD;
BEGIN
    -- Contar índices totales en schema metadata
    SELECT COUNT(*) INTO v_index_count
    FROM pg_indexes
    WHERE schemaname = 'metadata';

    RAISE NOTICE '==================================================================';
    RAISE NOTICE 'ÍNDICES MULTI-TENANT CREADOS EXITOSAMENTE';
    RAISE NOTICE '==================================================================';
    RAISE NOTICE 'Total de índices en schema metadata: %', v_index_count;
    RAISE NOTICE '';
    RAISE NOTICE 'Estadísticas de tablas:';

    FOR v_table_stats IN
        SELECT
            schemaname,
            tablename,
            pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
            pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as table_size,
            pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) as indexes_size,
            (SELECT COUNT(*) FROM pg_indexes WHERE schemaname = t.schemaname AND tablename = t.tablename) as index_count
        FROM pg_tables t
        WHERE schemaname IN ('metadata', 'public')
          AND tablename IN ('tenants', 'greenhouses', 'sensors', 'actuators', 'users', 'mqtt_users')
        ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
    LOOP
        RAISE NOTICE '  %.%: Total=%, Tabla=%, Índices=%, Count=%',
            v_table_stats.schemaname,
            v_table_stats.tablename,
            v_table_stats.total_size,
            v_table_stats.table_size,
            v_table_stats.indexes_size,
            v_table_stats.index_count;
    END LOOP;

    RAISE NOTICE '';
    RAISE NOTICE 'Extensiones habilitadas:';
    RAISE NOTICE '  - pg_trgm: Búsqueda de texto fuzzy ✓';
    RAISE NOTICE '  - pg_stat_statements: Monitoreo de queries ✓';
    RAISE NOTICE '==================================================================';
    RAISE NOTICE 'IMPORTANTE: Monitorear performance de queries con nuevos índices';
    RAISE NOTICE 'IMPORTANTE: Considerar VACUUM ANALYZE periódico en producción';
    RAISE NOTICE '==================================================================';
END $$;

-- Queries útiles para monitoreo post-migración:
--
-- Ver todos los índices de una tabla:
-- SELECT indexname, indexdef FROM pg_indexes WHERE tablename = 'sensors' ORDER BY indexname;
--
-- Ver tamaño de índices:
-- SELECT indexname, pg_size_pretty(pg_relation_size(indexrelid)) as size
-- FROM pg_stat_user_indexes WHERE schemaname = 'metadata' ORDER BY pg_relation_size(indexrelid) DESC;
--
-- Ver queries lentas (requiere pg_stat_statements):
-- SELECT query, calls, mean_exec_time, stddev_exec_time
-- FROM pg_stat_statements
-- ORDER BY mean_exec_time DESC
-- LIMIT 20;
