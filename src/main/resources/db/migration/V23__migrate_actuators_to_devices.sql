-- ============================================================================
-- V23: Migrar datos de actuators a devices
-- Fecha: 2025-12-29
-- Descripcion: Copia datos de la tabla actuators a la nueva tabla devices
-- NOTA: Esta migracion NO elimina la tabla actuators (se hace en una fase posterior)
-- ============================================================================

-- Migrar actuadores existentes a la tabla devices
INSERT INTO metadata.devices (
    id,
    tenant_id,
    greenhouse_id,
    sector_id,
    device_code,
    device_id,
    device_category,
    device_type_id,
    unit_id,
    mqtt_topic,
    mqtt_command_topic,
    current_value,
    current_state,
    state_id,
    location_in_greenhouse,
    is_active,
    last_seen,
    last_command_at,
    last_status_update,
    created_at,
    updated_at
)
SELECT
    a.id,
    a.tenant_id,
    a.greenhouse_id,
    NULL AS sector_id,  -- Se puede asignar posteriormente
    a.actuator_code AS device_code,
    a.device_id,
    'ACTUATOR'::metadata.device_category,
    -- Mapear tipo de actuador
    COALESCE(
        a.actuator_type_id::SMALLINT,
        (SELECT dt.id FROM metadata.device_types dt
         WHERE UPPER(dt.name) = UPPER(a.actuator_type) AND dt.category = 'ACTUATOR' LIMIT 1),
        (SELECT dt.id FROM metadata.device_types dt WHERE dt.name = 'VENTILATOR' LIMIT 1)
    ) AS device_type_id,
    -- Mapear unidad
    COALESCE(
        a.unit_id::SMALLINT,
        (SELECT u.id FROM metadata.units u WHERE u.symbol = a.unit LIMIT 1)
    ) AS unit_id,
    a.mqtt_status_topic AS mqtt_topic,
    a.mqtt_command_topic,
    a.current_value,
    a.current_state,
    -- Mapear estado
    COALESCE(
        a.state_id::SMALLINT,
        (SELECT ast.id FROM metadata.actuator_states ast
         WHERE UPPER(ast.name) = UPPER(a.current_state) LIMIT 1)
    ) AS state_id,
    a.location_in_greenhouse,
    a.is_active,
    a.last_status_update AS last_seen,
    a.last_command_at,
    a.last_status_update,
    a.created_at,
    a.updated_at
FROM metadata.actuators a
WHERE NOT EXISTS (
    -- Evitar duplicados si se ejecuta mas de una vez
    SELECT 1 FROM metadata.devices d WHERE d.id = a.id
);

-- Log de migracion
DO $$
DECLARE
    migrated_count INTEGER;
    total_actuators INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_actuators FROM metadata.actuators;
    SELECT COUNT(*) INTO migrated_count
    FROM metadata.devices WHERE device_category = 'ACTUATOR';

    RAISE NOTICE 'Migracion de actuadores completada:';
    RAISE NOTICE '  - Actuadores en tabla original: %', total_actuators;
    RAISE NOTICE '  - Actuadores migrados a devices: %', migrated_count;

    IF migrated_count < total_actuators THEN
        RAISE WARNING '  - ATENCION: Algunos actuadores no se migraron. Revisar conflictos.';
    END IF;
END $$;

-- Verificar integridad
DO $$
DECLARE
    orphan_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO orphan_count
    FROM metadata.devices d
    WHERE d.device_category = 'ACTUATOR'
    AND NOT EXISTS (
        SELECT 1 FROM metadata.greenhouses g WHERE g.id = d.greenhouse_id
    );

    IF orphan_count > 0 THEN
        RAISE WARNING 'Hay % dispositivos ACTUATOR sin greenhouse valido', orphan_count;
    END IF;
END $$;

-- Resumen final
DO $$
DECLARE
    sensor_count INTEGER;
    actuator_count INTEGER;
    total_devices INTEGER;
BEGIN
    SELECT COUNT(*) INTO sensor_count FROM metadata.devices WHERE device_category = 'SENSOR';
    SELECT COUNT(*) INTO actuator_count FROM metadata.devices WHERE device_category = 'ACTUATOR';
    total_devices := sensor_count + actuator_count;

    RAISE NOTICE '';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'RESUMEN DE MIGRACION A DEVICES';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Total dispositivos: %', total_devices;
    RAISE NOTICE '  - Sensores: %', sensor_count;
    RAISE NOTICE '  - Actuadores: %', actuator_count;
    RAISE NOTICE '========================================';
END $$;

-- Crear vistas de compatibilidad (para codigo legacy que aun use sensors/actuators)
CREATE OR REPLACE VIEW metadata.sensors_view AS
SELECT
    d.id,
    d.greenhouse_id,
    d.tenant_id,
    d.device_code AS sensor_code,
    d.device_id,
    dt.name AS sensor_type,
    d.device_type_id AS sensor_type_id,
    u.symbol AS unit,
    d.unit_id,
    d.mqtt_field_name,
    d.data_format,
    d.min_threshold,
    d.max_threshold,
    d.location_in_greenhouse,
    d.calibration_data,
    d.is_active,
    d.last_seen,
    d.created_at,
    d.updated_at
FROM metadata.devices d
JOIN metadata.device_types dt ON d.device_type_id = dt.id
LEFT JOIN metadata.units u ON d.unit_id = u.id
WHERE d.device_category = 'SENSOR';

COMMENT ON VIEW metadata.sensors_view IS 'Vista de compatibilidad para codigo que aun use la estructura de sensors';

CREATE OR REPLACE VIEW metadata.actuators_view AS
SELECT
    d.id,
    d.tenant_id,
    d.greenhouse_id,
    d.device_code AS actuator_code,
    d.device_id,
    dt.name AS actuator_type,
    d.device_type_id AS actuator_type_id,
    d.current_state,
    d.state_id,
    d.current_value,
    u.symbol AS unit,
    d.unit_id,
    d.mqtt_command_topic,
    d.mqtt_topic AS mqtt_status_topic,
    d.location_in_greenhouse,
    d.is_active,
    d.last_command_at,
    d.last_status_update,
    d.created_at,
    d.updated_at
FROM metadata.devices d
JOIN metadata.device_types dt ON d.device_type_id = dt.id
LEFT JOIN metadata.units u ON d.unit_id = u.id
WHERE d.device_category = 'ACTUATOR';

COMMENT ON VIEW metadata.actuators_view IS 'Vista de compatibilidad para codigo que aun use la estructura de actuators';
