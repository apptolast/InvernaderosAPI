-- =====================================================
-- V6: Extend actuators table for multi-tenant support
-- =====================================================
-- Description: Add multi-tenant fields and MQTT support to existing actuators table
-- Author: Claude Code
-- Date: 2025-11-16
-- =====================================================
-- Note: Table already exists from Kubernetes init script with basic structure
-- This migration extends it with additional fields for multi-tenant architecture

-- Paso 1: Agregar tenant_id (permite NULL temporalmente)
ALTER TABLE metadata.actuators
    ADD COLUMN IF NOT EXISTS tenant_id UUID;

-- Paso 2: Agregar nuevos campos para multi-tenant y MQTT
ALTER TABLE metadata.actuators
    ADD COLUMN IF NOT EXISTS actuator_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS current_value DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS unit VARCHAR(20),
    ADD COLUMN IF NOT EXISTS mqtt_command_topic VARCHAR(100),
    ADD COLUMN IF NOT EXISTS mqtt_status_topic VARCHAR(100),
    ADD COLUMN IF NOT EXISTS location_in_greenhouse VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_status_update TIMESTAMPTZ;

-- Paso 3: Migrar current_state de JSONB a VARCHAR(20) para simplificar
-- Crear columna temporal
ALTER TABLE metadata.actuators
    ADD COLUMN IF NOT EXISTS state_simple VARCHAR(20);

-- Migrar datos: extraer state de JSONB si existe, sino poner 'UNKNOWN'
UPDATE metadata.actuators
SET state_simple = COALESCE(
    current_state->>'state',
    current_state->>'status',
    'UNKNOWN'
)
WHERE state_simple IS NULL AND current_state IS NOT NULL;

-- Para registros sin current_state, poner UNKNOWN
UPDATE metadata.actuators
SET state_simple = 'UNKNOWN'
WHERE state_simple IS NULL;

-- Renombrar columnas
ALTER TABLE metadata.actuators
    RENAME COLUMN current_state TO current_state_old;

ALTER TABLE metadata.actuators
    RENAME COLUMN state_simple TO current_state;

-- Paso 4: Actualizar tenant_id desde greenhouses (denormalización)
UPDATE metadata.actuators a
SET tenant_id = g.tenant_id
FROM metadata.greenhouses g
WHERE a.greenhouse_id = g.id
  AND a.tenant_id IS NULL;

-- Paso 5: Establecer tenant_id como NOT NULL
ALTER TABLE metadata.actuators
    ALTER COLUMN tenant_id SET NOT NULL;

-- Paso 6: Agregar Foreign Keys
ALTER TABLE metadata.actuators
    DROP CONSTRAINT IF EXISTS fk_actuator_tenant;

ALTER TABLE metadata.actuators
    ADD CONSTRAINT fk_actuator_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES metadata.tenants(id)
        ON DELETE CASCADE;

-- Ya existe FK para greenhouse_id desde el init script

-- Paso 7: Agregar constraints de unicidad
ALTER TABLE metadata.actuators
    DROP CONSTRAINT IF EXISTS uq_actuator_code_per_greenhouse;

ALTER TABLE metadata.actuators
    ADD CONSTRAINT uq_actuator_code_per_greenhouse
        UNIQUE (greenhouse_id, actuator_code);

-- device_id ya es UNIQUE desde el init script

-- Paso 8: Crear índices
CREATE INDEX IF NOT EXISTS idx_actuators_tenant
    ON metadata.actuators(tenant_id);

CREATE INDEX IF NOT EXISTS idx_actuators_code
    ON metadata.actuators(actuator_code)
    WHERE actuator_code IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_actuators_type
    ON metadata.actuators(actuator_type);

CREATE INDEX IF NOT EXISTS idx_actuators_active
    ON metadata.actuators(is_active)
    WHERE is_active = true;

CREATE INDEX IF NOT EXISTS idx_actuators_tenant_active
    ON metadata.actuators(tenant_id, is_active)
    WHERE is_active = true;

-- Paso 9: Añadir CHECK constraints
ALTER TABLE metadata.actuators
    DROP CONSTRAINT IF EXISTS chk_actuator_state;

ALTER TABLE metadata.actuators
    ADD CONSTRAINT chk_actuator_state
    CHECK (current_state IN ('ON', 'OFF', 'AUTO', 'MANUAL', 'ERROR', 'UNKNOWN'));

ALTER TABLE metadata.actuators
    DROP CONSTRAINT IF EXISTS chk_actuator_type;

ALTER TABLE metadata.actuators
    ADD CONSTRAINT chk_actuator_type
    CHECK (actuator_type IN (
        'VENTILADOR', 'FAN',
        'RIEGO', 'IRRIGATION',
        'CALEFACCION', 'HEATING',
        'ENFRIAMIENTO', 'COOLING',
        'ILUMINACION', 'LIGHTING',
        'CORTINA', 'CURTAIN',
        'VALVULA', 'VALVE',
        'MOTOR', 'EXTRACTOR',
        'OTHER'
    ));

-- Paso 10: Añadir comentarios
COMMENT ON TABLE metadata.actuators IS 'Actuadores (dispositivos de control) multi-tenant en invernaderos';
COMMENT ON COLUMN metadata.actuators.tenant_id IS 'ID del tenant (denormalizado para queries eficientes)';
COMMENT ON COLUMN metadata.actuators.actuator_code IS 'Código corto del actuador (ej: FAN01, RIEGO02)';
COMMENT ON COLUMN metadata.actuators.current_state IS 'Estado actual: ON, OFF, AUTO, MANUAL, ERROR, UNKNOWN';
COMMENT ON COLUMN metadata.actuators.current_value IS 'Valor actual (ej: velocidad ventilador 0-100%, caudal riego)';
COMMENT ON COLUMN metadata.actuators.unit IS 'Unidad de medida del valor (%, L/h, RPM, etc.)';
COMMENT ON COLUMN metadata.actuators.mqtt_command_topic IS 'Topic MQTT para enviar comandos';
COMMENT ON COLUMN metadata.actuators.mqtt_status_topic IS 'Topic MQTT donde publica su estado';
COMMENT ON COLUMN metadata.actuators.last_status_update IS 'Último update de estado recibido vía MQTT';

-- Paso 11: Eliminar columna antigua current_state_old (JSONB) después de migración
-- Descomentar solo si estás seguro de que la migración funcionó correctamente
-- ALTER TABLE metadata.actuators DROP COLUMN IF EXISTS current_state_old;

-- Paso 12: Eliminar columna config si no se usa
-- Mantenerla por ahora para compatibilidad, puede eliminarse en futuras migraciones
-- ALTER TABLE metadata.actuators DROP COLUMN IF EXISTS config;
