-- Migración para expandir tabla greenhouses con soporte MQTT multi-tenant
-- Añade: greenhouse_code, mqtt_topic, intervalo de publicación, external_id
-- y establece FK explícita con tenants
--
-- Esto permite que cada invernadero tenga su propio topic MQTT dinámico
-- del tipo: GREENHOUSE/empresaID donde empresaID viene del tenant

-- Paso 1: Añadir campos para soporte MQTT dinámico
ALTER TABLE metadata.greenhouses
  ADD COLUMN IF NOT EXISTS greenhouse_code VARCHAR(50),
  ADD COLUMN IF NOT EXISTS mqtt_topic VARCHAR(100),
  ADD COLUMN IF NOT EXISTS mqtt_publish_interval_seconds INT DEFAULT 5,
  ADD COLUMN IF NOT EXISTS external_id VARCHAR(100);

-- Paso 2: Añadir FK explícita a tenants (si no existe)
-- Esto asegura integridad referencial y permite CASCADE DELETE
ALTER TABLE metadata.greenhouses
  DROP CONSTRAINT IF EXISTS fk_greenhouse_tenant;

ALTER TABLE metadata.greenhouses
  ADD CONSTRAINT fk_greenhouse_tenant
  FOREIGN KEY (tenant_id)
  REFERENCES metadata.tenants(id)
  ON DELETE CASCADE;

-- Paso 3: Añadir constraints de unicidad
-- greenhouse_code debe ser único dentro de un tenant
ALTER TABLE metadata.greenhouses
  ADD CONSTRAINT IF NOT EXISTS uq_greenhouse_code_per_tenant
  UNIQUE (tenant_id, greenhouse_code);

-- mqtt_topic debe ser globalmente único
ALTER TABLE metadata.greenhouses
  ADD CONSTRAINT IF NOT EXISTS uq_greenhouse_mqtt_topic
  UNIQUE (mqtt_topic);

-- Paso 4: Crear índices para búsquedas comunes
CREATE INDEX IF NOT EXISTS idx_greenhouses_tenant
  ON metadata.greenhouses(tenant_id);

CREATE INDEX IF NOT EXISTS idx_greenhouses_code
  ON metadata.greenhouses(greenhouse_code);

CREATE INDEX IF NOT EXISTS idx_greenhouses_mqtt_topic
  ON metadata.greenhouses(mqtt_topic);

CREATE INDEX IF NOT EXISTS idx_greenhouses_external_id
  ON metadata.greenhouses(external_id);

-- Paso 5: Añadir comentarios
COMMENT ON COLUMN metadata.greenhouses.greenhouse_code IS 'Código corto del invernadero (ej: INV01, SARA_01)';
COMMENT ON COLUMN metadata.greenhouses.mqtt_topic IS 'Topic MQTT completo asignado (ej: GREENHOUSE/empresa001/inv01)';
COMMENT ON COLUMN metadata.greenhouses.mqtt_publish_interval_seconds IS 'Intervalo de publicación de datos en segundos';
COMMENT ON COLUMN metadata.greenhouses.external_id IS 'ID externo del sistema de sensores (si aplica)';

-- Verificación: Ver estructura actualizada y relaciones
-- SELECT column_name, data_type, character_maximum_length, is_nullable
-- FROM information_schema.columns
-- WHERE table_schema = 'metadata' AND table_name = 'greenhouses'
-- ORDER BY ordinal_position;
--
-- SELECT tc.constraint_name, tc.constraint_type, kcu.column_name, ccu.table_name AS foreign_table_name
-- FROM information_schema.table_constraints AS tc
-- JOIN information_schema.key_column_usage AS kcu ON tc.constraint_name = kcu.constraint_name
-- LEFT JOIN information_schema.constraint_column_usage AS ccu ON ccu.constraint_name = tc.constraint_name
-- WHERE tc.table_schema = 'metadata' AND tc.table_name = 'greenhouses';
