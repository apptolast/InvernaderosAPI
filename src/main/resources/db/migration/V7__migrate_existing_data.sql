-- Migración de datos existentes al sistema multi-tenant
-- Crea un tenant por defecto y asigna todos los datos existentes a él
-- IMPORTANTE: Este script preserva TODOS los datos históricos
--
-- Los datos actuales (greenhouses, sensors, sensor_readings) se asignarán
-- al tenant "DEFAULT" para mantener compatibilidad con topic MQTT "GREENHOUSE"

-- =============================================================================
-- PASO 1: Crear tenant por defecto para migración
-- =============================================================================

DO $$
DECLARE
    v_default_tenant_id UUID;
BEGIN
    -- Intentar insertar tenant default, ignorar si ya existe
    INSERT INTO metadata.tenants (
        id,
        name,
        email,
        company_name,
        tax_id,
        mqtt_topic_prefix,
        address,
        city,
        country,
        phone,
        contact_person,
        is_active,
        created_at,
        updated_at
    ) VALUES (
        gen_random_uuid(),
        'Cliente Migración',
        'migracion@invernaderos.local',
        'Datos Sistema Anterior',
        'MIGRATION-2025',
        'DEFAULT',
        'Sin especificar',
        'Sin especificar',
        'España',
        'N/A',
        'Administrador Sistema',
        true,
        NOW(),
        NOW()
    )
    ON CONFLICT (mqtt_topic_prefix) DO NOTHING;

    -- Obtener ID del tenant default
    SELECT id INTO v_default_tenant_id
    FROM metadata.tenants
    WHERE mqtt_topic_prefix = 'DEFAULT';

    RAISE NOTICE 'Tenant DEFAULT creado/encontrado con ID: %', v_default_tenant_id;
END $$;

-- =============================================================================
-- PASO 2: Migrar greenhouses existentes al tenant DEFAULT
-- =============================================================================

-- Asignar greenhouses sin tenant_id al tenant DEFAULT
UPDATE metadata.greenhouses
SET tenant_id = (
    SELECT id FROM metadata.tenants WHERE mqtt_topic_prefix = 'DEFAULT'
)
WHERE tenant_id IS NULL;

-- Asignar códigos de greenhouse a los existentes (si no tienen)
UPDATE metadata.greenhouses g
SET
    greenhouse_code = COALESCE(g.greenhouse_code, 'GH-' || LPAD(ROW_NUMBER() OVER (ORDER BY g.created_at)::TEXT, 3, '0')),
    mqtt_topic = COALESCE(g.mqtt_topic, 'GREENHOUSE/DEFAULT/inv' || LPAD(ROW_NUMBER() OVER (ORDER BY g.created_at)::TEXT, 2, '0'))
WHERE greenhouse_code IS NULL OR mqtt_topic IS NULL;

-- Verificación
SELECT
    COUNT(*) as total_greenhouses,
    COUNT(CASE WHEN tenant_id IS NOT NULL THEN 1 END) as with_tenant,
    COUNT(CASE WHEN greenhouse_code IS NOT NULL THEN 1 END) as with_code,
    COUNT(CASE WHEN mqtt_topic IS NOT NULL THEN 1 END) as with_mqtt_topic
FROM metadata.greenhouses;

-- =============================================================================
-- PASO 3: Propagar tenant_id a sensors
-- =============================================================================

-- Actualizar tenant_id en sensors basado en su greenhouse
UPDATE metadata.sensors s
SET tenant_id = (
    SELECT g.tenant_id
    FROM metadata.greenhouses g
    WHERE g.id = s.greenhouse_id
)
WHERE s.tenant_id IS NULL;

-- Asignar códigos de sensor a los existentes (si no tienen)
WITH numbered_sensors AS (
    SELECT
        id,
        greenhouse_id,
        sensor_type,
        ROW_NUMBER() OVER (PARTITION BY greenhouse_id, sensor_type ORDER BY created_at) as sensor_number
    FROM metadata.sensors
    WHERE sensor_code IS NULL
)
UPDATE metadata.sensors s
SET
    sensor_code = UPPER(LEFT(ns.sensor_type, 4)) || LPAD(ns.sensor_number::TEXT, 2, '0'),
    mqtt_field_name = COALESCE(s.device_id, s.sensor_type || '_' || s.id::TEXT)
FROM numbered_sensors ns
WHERE s.id = ns.id AND s.sensor_code IS NULL;

-- Verificación
SELECT
    COUNT(*) as total_sensors,
    COUNT(CASE WHEN tenant_id IS NOT NULL THEN 1 END) as with_tenant,
    COUNT(CASE WHEN sensor_code IS NOT NULL THEN 1 END) as with_code,
    COUNT(CASE WHEN mqtt_field_name IS NOT NULL THEN 1 END) as with_mqtt_field
FROM metadata.sensors;

-- =============================================================================
-- PASO 4: Validar integridad de datos migrados
-- =============================================================================

-- Verificar que no hay greenhouses sin tenant
DO $$
DECLARE
    v_orphan_greenhouses INT;
BEGIN
    SELECT COUNT(*) INTO v_orphan_greenhouses
    FROM metadata.greenhouses
    WHERE tenant_id IS NULL;

    IF v_orphan_greenhouses > 0 THEN
        RAISE WARNING 'Hay % greenhouses sin tenant_id. Revisar!', v_orphan_greenhouses;
    ELSE
        RAISE NOTICE 'OK: Todos los greenhouses tienen tenant_id asignado';
    END IF;
END $$;

-- Verificar que no hay sensors sin tenant
DO $$
DECLARE
    v_orphan_sensors INT;
BEGIN
    SELECT COUNT(*) INTO v_orphan_sensors
    FROM metadata.sensors
    WHERE tenant_id IS NULL;

    IF v_orphan_sensors > 0 THEN
        RAISE WARNING 'Hay % sensors sin tenant_id. Revisar!', v_orphan_sensors;
    ELSE
        RAISE NOTICE 'OK: Todos los sensors tienen tenant_id asignado';
    END IF;
END $$;

-- =============================================================================
-- PASO 5: Extender mqtt_users con tenant_id
-- =============================================================================

-- Agregar columna tenant_id a mqtt_users si no existe
ALTER TABLE metadata.mqtt_users
  ADD COLUMN IF NOT EXISTS tenant_id UUID;

-- Actualizar tenant_id desde greenhouse_id
UPDATE metadata.mqtt_users mu
SET tenant_id = g.tenant_id
FROM metadata.greenhouses g
WHERE mu.greenhouse_id = g.id
  AND mu.tenant_id IS NULL;

-- Para mqtt_users sin greenhouse_id, asignar a DEFAULT
UPDATE metadata.mqtt_users
SET tenant_id = (
    SELECT id FROM metadata.tenants WHERE mqtt_topic_prefix = 'DEFAULT'
)
WHERE tenant_id IS NULL;

-- Agregar FK a tenants
ALTER TABLE metadata.mqtt_users
  DROP CONSTRAINT IF EXISTS fk_mqtt_users_tenant;

ALTER TABLE metadata.mqtt_users
  ADD CONSTRAINT fk_mqtt_users_tenant
  FOREIGN KEY (tenant_id)
  REFERENCES metadata.tenants(id)
  ON DELETE CASCADE;

-- =============================================================================
-- PASO 6: Resumen de migración
-- =============================================================================

DO $$
DECLARE
    v_tenant_count INT;
    v_greenhouse_count INT;
    v_sensor_count INT;
    v_mqtt_user_count INT;
BEGIN
    SELECT COUNT(*) INTO v_tenant_count FROM metadata.tenants;
    SELECT COUNT(*) INTO v_greenhouse_count FROM metadata.greenhouses WHERE tenant_id IS NOT NULL;
    SELECT COUNT(*) INTO v_sensor_count FROM metadata.sensors WHERE tenant_id IS NOT NULL;
    SELECT COUNT(*) INTO v_mqtt_user_count FROM metadata.mqtt_users WHERE tenant_id IS NOT NULL;

    RAISE NOTICE '=============================================================';
    RAISE NOTICE 'RESUMEN DE MIGRACIÓN A SISTEMA MULTI-TENANT';
    RAISE NOTICE '=============================================================';
    RAISE NOTICE 'Tenants creados: %', v_tenant_count;
    RAISE NOTICE 'Greenhouses migrados: %', v_greenhouse_count;
    RAISE NOTICE 'Sensors migrados: %', v_sensor_count;
    RAISE NOTICE 'MQTT users migrados: %', v_mqtt_user_count;
    RAISE NOTICE '=============================================================';
    RAISE NOTICE 'Tenant DEFAULT configurado con mqtt_topic_prefix="DEFAULT"';
    RAISE NOTICE 'Compatible con topic MQTT actual: "GREENHOUSE"';
    RAISE NOTICE '=============================================================';
END $$;

-- Verificación final: Mostrar estructura de datos migrados
-- SELECT t.name as tenant, t.mqtt_topic_prefix, COUNT(g.id) as greenhouses
-- FROM metadata.tenants t
-- LEFT JOIN metadata.greenhouses g ON g.tenant_id = t.id
-- GROUP BY t.id, t.name, t.mqtt_topic_prefix
-- ORDER BY t.created_at;
