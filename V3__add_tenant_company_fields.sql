-- Migración para expandir tabla tenants con campos de ficha de cliente/empresa
-- Añade información completa de empresa: dirección, contacto, MQTT topic, etc.
--
-- Esta migración prepara el sistema para multi-empresa donde cada tenant
-- representa un cliente (particular o empresa) con su propia información

-- Paso 1: Añadir campos de información de empresa
ALTER TABLE metadata.tenants
  ADD COLUMN IF NOT EXISTS company_name VARCHAR(200),
  ADD COLUMN IF NOT EXISTS tax_id VARCHAR(50),
  ADD COLUMN IF NOT EXISTS address TEXT,
  ADD COLUMN IF NOT EXISTS city VARCHAR(100),
  ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20),
  ADD COLUMN IF NOT EXISTS province VARCHAR(100),
  ADD COLUMN IF NOT EXISTS country VARCHAR(50) DEFAULT 'España',
  ADD COLUMN IF NOT EXISTS phone VARCHAR(50),
  ADD COLUMN IF NOT EXISTS contact_person VARCHAR(150),
  ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(50),
  ADD COLUMN IF NOT EXISTS contact_email VARCHAR(255),
  ADD COLUMN IF NOT EXISTS mqtt_topic_prefix VARCHAR(50),
  ADD COLUMN IF NOT EXISTS coordinates JSONB,
  ADD COLUMN IF NOT EXISTS notes TEXT;

-- Paso 2: Añadir constraints de unicidad
-- Tax ID debe ser único (CIF/NIF)
ALTER TABLE metadata.tenants
  ADD CONSTRAINT IF NOT EXISTS uq_tenants_tax_id
  UNIQUE (tax_id);

-- MQTT topic prefix debe ser único (usado para routing MQTT)
ALTER TABLE metadata.tenants
  ADD CONSTRAINT IF NOT EXISTS uq_tenants_mqtt_topic_prefix
  UNIQUE (mqtt_topic_prefix);

-- Paso 3: Crear índices para búsquedas comunes
CREATE INDEX IF NOT EXISTS idx_tenants_company_name
  ON metadata.tenants(company_name);

CREATE INDEX IF NOT EXISTS idx_tenants_mqtt_topic_prefix
  ON metadata.tenants(mqtt_topic_prefix);

CREATE INDEX IF NOT EXISTS idx_tenants_tax_id
  ON metadata.tenants(tax_id);

-- Paso 4: Añadir comentarios a la tabla
COMMENT ON COLUMN metadata.tenants.company_name IS 'Razón social o nombre de la empresa/cliente';
COMMENT ON COLUMN metadata.tenants.tax_id IS 'CIF/NIF de la empresa';
COMMENT ON COLUMN metadata.tenants.mqtt_topic_prefix IS 'Prefijo para topics MQTT (ej: GREENHOUSE/empresaID)';
COMMENT ON COLUMN metadata.tenants.coordinates IS 'Coordenadas geográficas: {lat: number, lon: number}';
COMMENT ON COLUMN metadata.tenants.contact_person IS 'Persona de contacto principal del cliente';

-- Verificación: Ver estructura actualizada
-- SELECT column_name, data_type, character_maximum_length, is_nullable
-- FROM information_schema.columns
-- WHERE table_schema = 'metadata' AND table_name = 'tenants'
-- ORDER BY ordinal_position;
