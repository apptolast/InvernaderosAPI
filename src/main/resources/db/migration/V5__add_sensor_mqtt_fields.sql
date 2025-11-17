-- Migración para expandir tabla sensors con soporte MQTT multi-tenant
-- Añade: sensor_code (ID corto), tenant_id (denormalización), mqtt_field_name, data_format
-- y establece FKs explícitas con greenhouses y tenants
--
-- Esto permite identificar sensores en mensajes MQTT con formato empresaID_sensorID
-- y optimiza queries multi-tenant evitando JOINs innecesarios

-- Paso 1: Añadir campos para soporte MQTT
ALTER TABLE metadata.sensors
  ADD COLUMN IF NOT EXISTS sensor_code VARCHAR(50),
  ADD COLUMN IF NOT EXISTS tenant_id UUID,
  ADD COLUMN IF NOT EXISTS mqtt_field_name VARCHAR(100),
  ADD COLUMN IF NOT EXISTS data_format VARCHAR(20) DEFAULT 'NUMERIC';

-- Paso 2: Popular tenant_id basado en greenhouse_id (denormalización)
UPDATE metadata.sensors s
SET tenant_id = (
    SELECT g.tenant_id
    FROM metadata.greenhouses g
    WHERE g.id = s.greenhouse_id
)
WHERE s.tenant_id IS NULL;

-- Paso 3: Añadir FK explícita a greenhouses
ALTER TABLE metadata.sensors
  DROP CONSTRAINT IF EXISTS fk_sensor_greenhouse;

ALTER TABLE metadata.sensors
  ADD CONSTRAINT fk_sensor_greenhouse
  FOREIGN KEY (greenhouse_id)
  REFERENCES metadata.greenhouses(id)
  ON DELETE CASCADE;

-- Paso 4: Añadir FK explícita a tenants (para queries directos por tenant)
ALTER TABLE metadata.sensors
  DROP CONSTRAINT IF EXISTS fk_sensor_tenant;

ALTER TABLE metadata.sensors
  ADD CONSTRAINT fk_sensor_tenant
  FOREIGN KEY (tenant_id)
  REFERENCES metadata.tenants(id)
  ON DELETE CASCADE;

-- Paso 5: Añadir constraints de unicidad
-- sensor_code debe ser único dentro de un greenhouse
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_sensor_code_per_greenhouse') THEN
        ALTER TABLE metadata.sensors ADD CONSTRAINT uq_sensor_code_per_greenhouse UNIQUE (greenhouse_id, sensor_code);
    END IF;
END$$;

-- mqtt_field_name debe ser único dentro de un greenhouse (mapeo JSON fields)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_mqtt_field_per_greenhouse') THEN
        ALTER TABLE metadata.sensors ADD CONSTRAINT uq_mqtt_field_per_greenhouse UNIQUE (greenhouse_id, mqtt_field_name);
    END IF;
END$$;

-- Paso 6: Crear índices para búsquedas comunes
CREATE INDEX IF NOT EXISTS idx_sensors_greenhouse
  ON metadata.sensors(greenhouse_id);

CREATE INDEX IF NOT EXISTS idx_sensors_tenant
  ON metadata.sensors(tenant_id);

CREATE INDEX IF NOT EXISTS idx_sensors_code
  ON metadata.sensors(sensor_code);

CREATE INDEX IF NOT EXISTS idx_sensors_mqtt_field
  ON metadata.sensors(mqtt_field_name);

CREATE INDEX IF NOT EXISTS idx_sensors_device_id
  ON metadata.sensors(device_id);

-- Paso 7: Añadir CHECK constraint para data_format
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_sensor_data_format') THEN
        ALTER TABLE metadata.sensors ADD CONSTRAINT chk_sensor_data_format CHECK (data_format IN ('NUMERIC', 'STRING', 'JSON', 'BOOLEAN'));
    END IF;
END$$;

-- Paso 8: Añadir comentarios
COMMENT ON COLUMN metadata.sensors.sensor_code IS 'Código corto del sensor para MQTT (ej: TEMP01, HUM02)';
COMMENT ON COLUMN metadata.sensors.tenant_id IS 'ID del tenant (denormalizado para performance en queries multi-tenant)';
COMMENT ON COLUMN metadata.sensors.mqtt_field_name IS 'Nombre del campo en payload JSON MQTT (ej: empresaID_sensorID, TEMPERATURA_INVERNADERO_01)';
COMMENT ON COLUMN metadata.sensors.data_format IS 'Formato de datos: NUMERIC, STRING, JSON, BOOLEAN';

-- Verificación: Ver estructura actualizada
-- SELECT column_name, data_type, character_maximum_length, is_nullable
-- FROM information_schema.columns
-- WHERE table_schema = 'metadata' AND table_name = 'sensors'
-- ORDER BY ordinal_position;
--
-- Verificar FKs:
-- SELECT tc.constraint_name, tc.constraint_type, kcu.column_name, ccu.table_name AS foreign_table_name
-- FROM information_schema.table_constraints AS tc
-- JOIN information_schema.key_column_usage AS kcu ON tc.constraint_name = kcu.constraint_name
-- LEFT JOIN information_schema.constraint_column_usage AS ccu ON ccu.constraint_name = tc.constraint_name
-- WHERE tc.table_schema = 'metadata' AND tc.table_name = 'sensors';
