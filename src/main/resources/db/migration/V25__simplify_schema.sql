-- ============================================================================
-- V25: SIMPLIFICACION RADICAL DEL ESQUEMA
-- Fecha: 2025-12-29
-- Descripcion: Reduce de 27 tablas a 8 tablas principales
-- CAMBIOS:
--   - Elimina tablas de catalogo (usar CHECK constraints)
--   - Embebe sectores en greenhouses como JSONB
--   - MANTIENE mqtt_users y mqtt_acl (requeridas por EMQX)
--   - Elimina tablas de historico innecesarias
--   - Crea setpoints y command_history simplificados
-- ============================================================================

-- ==========================================================================
-- PASO 1: Agregar columna sectors JSONB a greenhouses
-- ==========================================================================
ALTER TABLE metadata.greenhouses
ADD COLUMN IF NOT EXISTS sectors JSONB DEFAULT '[]'::jsonb;

-- Migrar datos de sectors a JSONB en greenhouses
UPDATE metadata.greenhouses g
SET sectors = COALESCE((
    SELECT jsonb_agg(jsonb_build_object(
        'code', s.sector_code,
        'name', s.name,
        'area_m2', s.area_m2,
        'target_temp_min', s.target_temperature_min,
        'target_temp_max', s.target_temperature_max,
        'target_humidity_min', s.target_humidity_min,
        'target_humidity_max', s.target_humidity_max,
        'location_data', s.location_data
    ))
    FROM metadata.sectors s
    WHERE s.greenhouse_id = g.id AND s.is_active = TRUE
), '[]'::jsonb);

COMMENT ON COLUMN metadata.greenhouses.sectors IS 'Sectores embebidos como JSONB: [{"code": "S01", "name": "Sector Norte", ...}]';

-- ==========================================================================
-- PASO 2: Crear nueva tabla devices_simplified
-- ==========================================================================
CREATE TABLE metadata.devices_new (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES metadata.tenants(id) ON DELETE CASCADE,
    greenhouse_id UUID NOT NULL REFERENCES metadata.greenhouses(id) ON DELETE CASCADE,

    -- Identificacion
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100),
    hardware_id VARCHAR(50),

    -- Tipo (CHECK constraint, NO FK)
    category VARCHAR(20) NOT NULL,
    type VARCHAR(30) NOT NULL,
    unit VARCHAR(15),

    -- Sector (referencia al JSONB de greenhouses)
    sector VARCHAR(50),

    -- MQTT
    mqtt_topic VARCHAR(150),
    mqtt_command_topic VARCHAR(150),
    mqtt_field_name VARCHAR(100),

    -- Estado actual
    last_value DOUBLE PRECISION,
    state VARCHAR(20),
    last_seen TIMESTAMPTZ,

    -- Umbrales (solo sensores)
    min_threshold NUMERIC(10,2),
    max_threshold NUMERIC(10,2),

    -- Config adicional
    config JSONB DEFAULT '{}'::jsonb,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- CHECK constraints en lugar de FK
    CONSTRAINT chk_device_category CHECK (category IN ('SENSOR', 'ACTUATOR')),
    CONSTRAINT chk_device_type CHECK (type IN (
        -- Sensores
        'TEMPERATURE', 'HUMIDITY', 'CO2', 'CO2_LEVEL', 'LIGHT', 'LIGHT_INTENSITY',
        'SOIL_MOISTURE', 'PRESSURE', 'ATMOSPHERIC_PRESSURE', 'WIND_SPEED', 'WIND_DIRECTION',
        'UV', 'UV_INDEX', 'PH', 'EC', 'RAIN', 'RAINFALL', 'SOLAR_RADIATION',
        -- Actuadores
        'VENTILATOR', 'FAN', 'EXTRACTOR', 'HEATER', 'COOLER', 'IRRIGATOR', 'LIGHT_CONTROL',
        'LIGHTING', 'SHADE', 'CURTAIN', 'WINDOW', 'PUMP', 'VALVE', 'MOTOR', 'RELAY',
        'CO2_INJECTOR', 'DEHUMIDIFIER', 'MISTING',
        -- Otros
        'OTHER'
    )),
    CONSTRAINT chk_device_unit CHECK (unit IS NULL OR unit IN (
        '°C', '°F', '°', '%', 'ppm', 'lux', 'hPa', 'm/s', 'km/h', 'W', 'kW', 'W/m²',
        'L/h', 'L/min', 'm³/h', 'RPM', 'pH', 'mS/cm', 'dS/m', 'mm', 'kg', 'L', 'bar',
        'V', 'A', 'Hz', 'bool', 'unit', 'value'
    )),
    CONSTRAINT chk_device_state CHECK (state IS NULL OR state IN (
        'ON', 'OFF', 'AUTO', 'MANUAL', 'ERROR', 'OFFLINE', 'STANDBY', 'MAINTENANCE'
    )),
    CONSTRAINT chk_threshold_range CHECK (
        min_threshold IS NULL OR max_threshold IS NULL OR min_threshold <= max_threshold
    ),
    CONSTRAINT uq_device_code UNIQUE (greenhouse_id, code)
);

-- Migrar datos de devices actual a devices_new
INSERT INTO metadata.devices_new (
    id, tenant_id, greenhouse_id, code, name, hardware_id,
    category, type, unit, sector,
    mqtt_topic, mqtt_command_topic, mqtt_field_name,
    last_value, state, last_seen,
    min_threshold, max_threshold, config,
    is_active, created_at, updated_at
)
SELECT
    d.id,
    d.tenant_id,
    d.greenhouse_id,
    d.device_code AS code,
    NULL AS name,  -- No existia en la tabla anterior
    d.device_id AS hardware_id,
    d.device_category::TEXT AS category,
    COALESCE(dt.name, 'OTHER') AS type,
    COALESCE(u.symbol, NULL) AS unit,
    COALESCE(s.sector_code, NULL) AS sector,
    d.mqtt_topic,
    d.mqtt_command_topic,
    d.mqtt_field_name,
    d.current_value AS last_value,
    COALESCE(ast.name, d.current_state) AS state,
    d.last_seen,
    d.min_threshold,
    d.max_threshold,
    COALESCE(d.calibration_data, '{}'::jsonb) AS config,
    d.is_active,
    d.created_at,
    d.updated_at
FROM metadata.devices d
LEFT JOIN metadata.device_types dt ON d.device_type_id = dt.id
LEFT JOIN metadata.units u ON d.unit_id = u.id
LEFT JOIN metadata.sectors s ON d.sector_id = s.id
LEFT JOIN metadata.actuator_states ast ON d.state_id = ast.id;

-- Indices para devices_new
CREATE INDEX idx_devices_new_tenant ON metadata.devices_new(tenant_id);
CREATE INDEX idx_devices_new_greenhouse ON metadata.devices_new(greenhouse_id);
CREATE INDEX idx_devices_new_category ON metadata.devices_new(category);
CREATE INDEX idx_devices_new_type ON metadata.devices_new(type);
CREATE INDEX idx_devices_new_active ON metadata.devices_new(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_devices_new_last_seen ON metadata.devices_new(last_seen DESC) WHERE is_active = TRUE;
CREATE INDEX idx_devices_new_sector ON metadata.devices_new(greenhouse_id, sector) WHERE sector IS NOT NULL;

-- ==========================================================================
-- PASO 3: Crear tabla setpoints simplificada
-- ==========================================================================
CREATE TABLE metadata.setpoints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES metadata.tenants(id) ON DELETE CASCADE,
    greenhouse_id UUID NOT NULL REFERENCES metadata.greenhouses(id) ON DELETE CASCADE,

    -- Sector (NULL = todo el invernadero)
    sector VARCHAR(50),

    -- Parametro
    parameter VARCHAR(30) NOT NULL,

    -- Periodo
    period VARCHAR(20) NOT NULL,

    -- Valores
    min_value NUMERIC(10,2),
    max_value NUMERIC(10,2),
    target_value NUMERIC(10,2),

    -- Horarios (para DAY/NIGHT)
    times JSONB,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_setpoint_parameter CHECK (parameter IN (
        'TEMPERATURE', 'HUMIDITY', 'CO2', 'LIGHT', 'SOIL_MOISTURE', 'PRESSURE', 'PH', 'EC'
    )),
    CONSTRAINT chk_setpoint_period CHECK (period IN ('DAY', 'NIGHT', 'ALL')),
    CONSTRAINT chk_setpoint_values CHECK (
        min_value IS NULL OR max_value IS NULL OR min_value <= max_value
    ),
    CONSTRAINT uq_setpoint UNIQUE (greenhouse_id, sector, parameter, period)
);

-- Migrar datos de greenhouse_setpoints
INSERT INTO metadata.setpoints (
    id, tenant_id, greenhouse_id, sector, parameter, period,
    min_value, max_value, target_value, times,
    is_active, created_at, updated_at
)
SELECT
    gs.id,
    gs.tenant_id,
    gs.greenhouse_id,
    s.sector_code AS sector,
    gs.parameter_type AS parameter,
    CASE gs.day_period
        WHEN 'DIURNO' THEN 'DAY'
        WHEN 'NOCTURNO' THEN 'NIGHT'
        WHEN 'ALL_DAY' THEN 'ALL'
        ELSE 'ALL'
    END AS period,
    gs.min_value,
    gs.max_value,
    gs.target_value,
    CASE
        WHEN gs.start_time IS NOT NULL AND gs.end_time IS NOT NULL
        THEN jsonb_build_object('start', gs.start_time::TEXT, 'end', gs.end_time::TEXT)
        ELSE NULL
    END AS times,
    gs.is_active,
    gs.created_at,
    gs.updated_at
FROM metadata.greenhouse_setpoints gs
LEFT JOIN metadata.sectors s ON gs.sector_id = s.id;

-- Indices para setpoints
CREATE INDEX idx_setpoints_tenant ON metadata.setpoints(tenant_id);
CREATE INDEX idx_setpoints_greenhouse ON metadata.setpoints(greenhouse_id);
CREATE INDEX idx_setpoints_parameter ON metadata.setpoints(parameter);
CREATE INDEX idx_setpoints_active ON metadata.setpoints(greenhouse_id, is_active) WHERE is_active = TRUE;

-- ==========================================================================
-- PASO 4: Simplificar tabla alerts
-- ==========================================================================
-- Agregar columna device_id y actualizar CHECK constraints
ALTER TABLE metadata.alerts
ADD COLUMN IF NOT EXISTS device_id UUID;

-- Actualizar device_id basandose en sensor_id
UPDATE metadata.alerts a
SET device_id = d.id
FROM metadata.devices_new d
WHERE a.sensor_id IS NOT NULL
AND d.id = a.sensor_id;

-- Modificar CHECK constraint de severity para incluir solo los valores simples
ALTER TABLE metadata.alerts DROP CONSTRAINT IF EXISTS chk_alert_severity;
ALTER TABLE metadata.alerts ADD CONSTRAINT chk_alert_severity
    CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL', 'ERROR'));

-- Agregar CHECK constraint para alert_type si no existe
ALTER TABLE metadata.alerts DROP CONSTRAINT IF EXISTS chk_alert_type;
ALTER TABLE metadata.alerts ADD CONSTRAINT chk_alert_type
    CHECK (alert_type IN (
        'THRESHOLD', 'THRESHOLD_EXCEEDED', 'OFFLINE', 'SENSOR_OFFLINE',
        'ERROR', 'SYSTEM', 'WARNING', 'INFO', 'ACTUATOR_FAILURE'
    ));

-- ==========================================================================
-- PASO 5: MQTT - NO TOCAR mqtt_users ni mqtt_acl
-- ==========================================================================
-- IMPORTANTE: Las tablas mqtt_users y mqtt_acl son REQUERIDAS por EMQX
-- El broker las consulta directamente para autenticacion y autorizacion
-- Ver: https://docs.emqx.com/en/emqx/latest/access-control/authz/postgresql.html
--
-- Estructura esperada por EMQX:
--   mqtt_users: id, username, password_hash, salt, is_superuser, is_active, ...
--   mqtt_acl: id, allow, ipaddr, username, clientid, access, topic
--
-- NO ELIMINAR NI MODIFICAR ESTAS TABLAS

-- ==========================================================================
-- PASO 6: Crear tabla command_history
-- ==========================================================================
CREATE TABLE metadata.command_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID NOT NULL REFERENCES metadata.devices_new(id) ON DELETE CASCADE,

    command VARCHAR(50) NOT NULL,
    value DOUBLE PRECISION,

    -- Quien ejecuto
    source VARCHAR(30),
    user_id UUID REFERENCES metadata.users(id) ON DELETE SET NULL,

    -- Resultado
    success BOOLEAN,
    response JSONB,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_command_source CHECK (source IS NULL OR source IN (
        'USER', 'SYSTEM', 'SCHEDULE', 'ALERT', 'API', 'MQTT'
    ))
);

-- Migrar datos de actuator_command_history
INSERT INTO metadata.command_history (
    device_id, command, value, source, user_id, success, response, created_at
)
SELECT
    ach.actuator_id AS device_id,
    ach.command,
    ach.target_value AS value,
    CASE ach.triggered_by
        WHEN 'USER' THEN 'USER'
        WHEN 'AUTOMATION' THEN 'SYSTEM'
        WHEN 'SCHEDULE' THEN 'SCHEDULE'
        WHEN 'ALERT' THEN 'ALERT'
        WHEN 'API' THEN 'API'
        WHEN 'SYSTEM' THEN 'SYSTEM'
        ELSE 'SYSTEM'
    END AS source,
    ach.triggered_by_user_id AS user_id,
    CASE ach.execution_status
        WHEN 'EXECUTED' THEN TRUE
        WHEN 'FAILED' THEN FALSE
        ELSE NULL
    END AS success,
    ach.metadata AS response,
    ach.command_sent_at AS created_at
FROM metadata.actuator_command_history ach
WHERE EXISTS (SELECT 1 FROM metadata.devices_new d WHERE d.id = ach.actuator_id);

-- Indices para command_history
CREATE INDEX idx_command_history_device ON metadata.command_history(device_id);
CREATE INDEX idx_command_history_created ON metadata.command_history(created_at DESC);
CREATE INDEX idx_command_history_device_time ON metadata.command_history(device_id, created_at DESC);

-- ==========================================================================
-- PASO 7: Renombrar tablas (swap devices)
-- ==========================================================================
-- Primero eliminar las FK que apuntan a devices
ALTER TABLE metadata.alerts DROP CONSTRAINT IF EXISTS alerts_sensor_id_fkey;

-- Renombrar devices actual a devices_old
ALTER TABLE metadata.devices RENAME TO devices_old;

-- Renombrar devices_new a devices
ALTER TABLE metadata.devices_new RENAME TO devices;

-- Actualizar FK de alerts para apuntar a devices
ALTER TABLE metadata.alerts
ADD CONSTRAINT alerts_device_id_fkey
FOREIGN KEY (device_id) REFERENCES metadata.devices(id) ON DELETE SET NULL;

-- NOTA: mqtt_users y mqtt_acl se mantienen sin cambios (EMQX)

-- Actualizar FK de command_history
ALTER TABLE metadata.command_history DROP CONSTRAINT IF EXISTS command_history_device_id_fkey;
ALTER TABLE metadata.command_history
ADD CONSTRAINT command_history_device_id_fkey
FOREIGN KEY (device_id) REFERENCES metadata.devices(id) ON DELETE CASCADE;

-- ==========================================================================
-- PASO 8: DROP tablas innecesarias
-- ==========================================================================
-- Primero eliminar FK constraints que apuntan a estas tablas
ALTER TABLE metadata.devices_old DROP CONSTRAINT IF EXISTS devices_device_type_id_fkey;
ALTER TABLE metadata.devices_old DROP CONSTRAINT IF EXISTS devices_unit_id_fkey;
ALTER TABLE metadata.devices_old DROP CONSTRAINT IF EXISTS devices_state_id_fkey;
ALTER TABLE metadata.devices_old DROP CONSTRAINT IF EXISTS devices_sector_id_fkey;
ALTER TABLE metadata.greenhouse_setpoints DROP CONSTRAINT IF EXISTS greenhouse_setpoints_sector_id_fkey;
ALTER TABLE metadata.greenhouse_setpoints DROP CONSTRAINT IF EXISTS greenhouse_setpoints_alert_severity_id_fkey;
ALTER TABLE metadata.alerts DROP CONSTRAINT IF EXISTS fk_alerts_alert_type;
ALTER TABLE metadata.alerts DROP CONSTRAINT IF EXISTS fk_alerts_severity;

-- DROP tablas de catalogo
DROP TABLE IF EXISTS metadata.device_types CASCADE;
DROP TABLE IF EXISTS metadata.sensor_types CASCADE;
DROP TABLE IF EXISTS metadata.actuator_types CASCADE;
DROP TABLE IF EXISTS metadata.actuator_states CASCADE;
DROP TABLE IF EXISTS metadata.units CASCADE;
DROP TABLE IF EXISTS metadata.alert_types CASCADE;
DROP TABLE IF EXISTS metadata.alert_severities CASCADE;

-- DROP tabla sectors (ya migrada a JSONB)
DROP TABLE IF EXISTS metadata.sectors CASCADE;

-- DROP tablas de historico innecesarias
DROP TABLE IF EXISTS metadata.actuator_command_history CASCADE;
DROP TABLE IF EXISTS metadata.alert_resolution_history CASCADE;
DROP TABLE IF EXISTS metadata.sensor_configuration_history CASCADE;
DROP TABLE IF EXISTS metadata.audit_log CASCADE;
DROP TABLE IF EXISTS metadata.bulk_operation_log CASCADE;
DROP TABLE IF EXISTS metadata.data_quality_log CASCADE;
DROP TABLE IF EXISTS metadata.greenhouse_snapshot CASCADE;
DROP TABLE IF EXISTS metadata.migration_scripts CASCADE;

-- DROP tablas consolidadas
-- NOTA: mqtt_users y mqtt_acl se MANTIENEN (requeridas por EMQX)
DROP TABLE IF EXISTS metadata.greenhouse_setpoints CASCADE;

-- DROP tablas originales obsoletas
DROP TABLE IF EXISTS metadata.sensors CASCADE;
DROP TABLE IF EXISTS metadata.actuators CASCADE;
DROP TABLE IF EXISTS metadata.devices_old CASCADE;

-- DROP vistas obsoletas
DROP VIEW IF EXISTS metadata.sensors_view CASCADE;
DROP VIEW IF EXISTS metadata.actuators_view CASCADE;
DROP VIEW IF EXISTS metadata.v_active_setpoints CASCADE;

-- DROP ENUM obsoleto (device_category)
DROP TYPE IF EXISTS metadata.device_category CASCADE;

-- ==========================================================================
-- PASO 9: Comentarios y documentacion
-- ==========================================================================
COMMENT ON TABLE metadata.devices IS 'Dispositivos IoT unificados (sensores + actuadores) con CHECK constraints';
COMMENT ON TABLE metadata.setpoints IS 'Consignas/setpoints simplificadas para parametros por periodo';
COMMENT ON TABLE metadata.command_history IS 'Historico de comandos enviados a actuadores';
-- NOTA: mqtt_users y mqtt_acl mantienen sus comentarios originales (EMQX)

-- ==========================================================================
-- PASO 10: Resumen final
-- ==========================================================================
DO $$
DECLARE
    table_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO table_count
    FROM information_schema.tables
    WHERE table_schema = 'metadata' AND table_type = 'BASE TABLE';

    RAISE NOTICE '';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'SIMPLIFICACION COMPLETADA';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Tablas en metadata: %', table_count;
    RAISE NOTICE '';
    RAISE NOTICE 'Tablas principales:';
    RAISE NOTICE '  1. tenants';
    RAISE NOTICE '  2. greenhouses (con sectors JSONB)';
    RAISE NOTICE '  3. devices (CHECK constraints)';
    RAISE NOTICE '  4. setpoints';
    RAISE NOTICE '  5. alerts';
    RAISE NOTICE '  6. users';
    RAISE NOTICE '  7. command_history';
    RAISE NOTICE '  + mqtt_users (EMQX auth)';
    RAISE NOTICE '  + mqtt_acl (EMQX permisos)';
    RAISE NOTICE '  + flyway_schema_history (sistema)';
    RAISE NOTICE '========================================';
END $$;
