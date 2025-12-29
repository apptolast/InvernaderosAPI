-- ============================================================================
-- V22: Migrar datos de sensors a devices
-- Fecha: 2025-12-29
-- Descripcion: Copia datos de la tabla sensors a la nueva tabla devices
-- NOTA: Esta migracion NO elimina la tabla sensors (se hace en una fase posterior)
-- ============================================================================

-- Migrar sensores existentes a la tabla devices
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
    mqtt_field_name,
    current_value,
    min_threshold,
    max_threshold,
    data_format,
    location_in_greenhouse,
    calibration_data,
    is_active,
    last_seen,
    created_at,
    updated_at
)
SELECT
    s.id,
    s.tenant_id,
    s.greenhouse_id,
    NULL AS sector_id,  -- Se puede asignar posteriormente
    COALESCE(s.sensor_code, 'SENSOR_' || LEFT(s.id::TEXT, 8)) AS device_code,
    s.device_id,
    'SENSOR'::metadata.device_category,
    -- Mapear tipo de sensor
    COALESCE(
        s.sensor_type_id::SMALLINT,
        (SELECT dt.id FROM metadata.device_types dt
         WHERE UPPER(dt.name) = UPPER(s.sensor_type) AND dt.category = 'SENSOR' LIMIT 1),
        (SELECT dt.id FROM metadata.device_types dt WHERE dt.name = 'TEMPERATURE' LIMIT 1)
    ) AS device_type_id,
    -- Mapear unidad
    COALESCE(
        s.unit_id::SMALLINT,
        (SELECT u.id FROM metadata.units u WHERE u.symbol = s.unit LIMIT 1)
    ) AS unit_id,
    NULL AS mqtt_topic,
    s.mqtt_field_name,
    NULL AS current_value,
    s.min_threshold,
    s.max_threshold,
    COALESCE(s.data_format, 'NUMERIC'),
    s.location_in_greenhouse,
    s.calibration_data,
    s.is_active,
    s.last_seen,
    s.created_at,
    s.updated_at
FROM metadata.sensors s
WHERE NOT EXISTS (
    -- Evitar duplicados si se ejecuta mas de una vez
    SELECT 1 FROM metadata.devices d WHERE d.id = s.id
);

-- Log de migracion
DO $$
DECLARE
    migrated_count INTEGER;
    total_sensors INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_sensors FROM metadata.sensors;
    SELECT COUNT(*) INTO migrated_count
    FROM metadata.devices WHERE device_category = 'SENSOR';

    RAISE NOTICE 'Migracion de sensores completada:';
    RAISE NOTICE '  - Sensores en tabla original: %', total_sensors;
    RAISE NOTICE '  - Sensores migrados a devices: %', migrated_count;

    IF migrated_count < total_sensors THEN
        RAISE WARNING '  - ATENCION: Algunos sensores no se migraron. Revisar conflictos.';
    END IF;
END $$;

-- Verificar integridad
DO $$
DECLARE
    orphan_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO orphan_count
    FROM metadata.devices d
    WHERE d.device_category = 'SENSOR'
    AND NOT EXISTS (
        SELECT 1 FROM metadata.greenhouses g WHERE g.id = d.greenhouse_id
    );

    IF orphan_count > 0 THEN
        RAISE WARNING 'Hay % dispositivos SENSOR sin greenhouse valido', orphan_count;
    END IF;
END $$;
