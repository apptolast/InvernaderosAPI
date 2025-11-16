-- Migración para crear tabla actuators (NUEVA)
-- Esta tabla almacena información de actuadores (ventiladores, riego, calefacción, etc.)
-- en los invernaderos, permitiendo control y monitoreo de estado
--
-- Actualmente el sistema tiene ActuatorStatusListener pero sin persistencia
-- Esta migración implementa el modelo completo de datos para actuadores

-- Paso 1: Crear tabla actuators
CREATE TABLE IF NOT EXISTS metadata.actuators (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    greenhouse_id UUID NOT NULL,
    actuator_code VARCHAR(50) NOT NULL,
    device_id VARCHAR(50) NOT NULL,
    actuator_type VARCHAR(50) NOT NULL,
    current_state VARCHAR(20),
    current_value DOUBLE PRECISION,
    unit VARCHAR(20),
    mqtt_command_topic VARCHAR(100),
    mqtt_status_topic VARCHAR(100),
    location_in_greenhouse VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_command_at TIMESTAMPTZ,
    last_status_update TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- FKs
    CONSTRAINT fk_actuator_greenhouse
        FOREIGN KEY (greenhouse_id)
        REFERENCES metadata.greenhouses(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_actuator_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES metadata.tenants(id)
        ON DELETE CASCADE,

    -- Constraints de unicidad
    CONSTRAINT uq_actuator_code_per_greenhouse
        UNIQUE (greenhouse_id, actuator_code),

    CONSTRAINT uq_actuator_device_id_per_greenhouse
        UNIQUE (greenhouse_id, device_id)
);

-- Paso 2: Crear índices para búsquedas comunes
CREATE INDEX IF NOT EXISTS idx_actuators_greenhouse
    ON metadata.actuators(greenhouse_id);

CREATE INDEX IF NOT EXISTS idx_actuators_tenant
    ON metadata.actuators(tenant_id);

CREATE INDEX IF NOT EXISTS idx_actuators_code
    ON metadata.actuators(actuator_code);

CREATE INDEX IF NOT EXISTS idx_actuators_device_id
    ON metadata.actuators(device_id);

CREATE INDEX IF NOT EXISTS idx_actuators_type
    ON metadata.actuators(actuator_type);

CREATE INDEX IF NOT EXISTS idx_actuators_active
    ON metadata.actuators(is_active)
    WHERE is_active = true;

-- Paso 3: Añadir CHECK constraints para estados válidos
ALTER TABLE metadata.actuators
    ADD CONSTRAINT IF NOT EXISTS chk_actuator_state
    CHECK (current_state IN ('ON', 'OFF', 'AUTO', 'MANUAL', 'ERROR', 'UNKNOWN'));

ALTER TABLE metadata.actuators
    ADD CONSTRAINT IF NOT EXISTS chk_actuator_type
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

-- Paso 4: Añadir comentarios a la tabla y columnas
COMMENT ON TABLE metadata.actuators IS 'Actuadores (dispositivos de control) en invernaderos: ventiladores, riego, calefacción, etc.';
COMMENT ON COLUMN metadata.actuators.actuator_code IS 'Código corto del actuador (ej: FAN01, RIEGO02)';
COMMENT ON COLUMN metadata.actuators.device_id IS 'ID del dispositivo físico (hardware)';
COMMENT ON COLUMN metadata.actuators.actuator_type IS 'Tipo de actuador: VENTILADOR, RIEGO, CALEFACCION, etc.';
COMMENT ON COLUMN metadata.actuators.current_state IS 'Estado actual: ON, OFF, AUTO, MANUAL, ERROR, UNKNOWN';
COMMENT ON COLUMN metadata.actuators.current_value IS 'Valor actual (ej: velocidad ventilador 0-100%, caudal riego)';
COMMENT ON COLUMN metadata.actuators.unit IS 'Unidad de medida del valor (%, L/h, RPM, etc.)';
COMMENT ON COLUMN metadata.actuators.mqtt_command_topic IS 'Topic MQTT para enviar comandos al actuador';
COMMENT ON COLUMN metadata.actuators.mqtt_status_topic IS 'Topic MQTT donde el actuador publica su estado';
COMMENT ON COLUMN metadata.actuators.last_command_at IS 'Timestamp del último comando enviado';
COMMENT ON COLUMN metadata.actuators.last_status_update IS 'Timestamp de la última actualización de estado recibida';

-- Verificación: Ver estructura de la nueva tabla
-- SELECT column_name, data_type, character_maximum_length, is_nullable, column_default
-- FROM information_schema.columns
-- WHERE table_schema = 'metadata' AND table_name = 'actuators'
-- ORDER BY ordinal_position;
--
-- Verificar constraints:
-- SELECT constraint_name, constraint_type
-- FROM information_schema.table_constraints
-- WHERE table_schema = 'metadata' AND table_name = 'actuators';
