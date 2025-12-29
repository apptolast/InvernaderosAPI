-- ============================================================================
-- V20: Crear tabla unificada de dispositivos (sensors + actuators)
-- Fecha: 2025-12-29
-- Descripcion: Tabla unica que consolida sensores y actuadores
-- Referencia: https://www.postgresql.org/docs/current/ddl-inherit.html
-- ============================================================================

-- Verificar que el ENUM device_category existe (creado en V18)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'device_category') THEN
        CREATE TYPE metadata.device_category AS ENUM ('SENSOR', 'ACTUATOR');
    END IF;
END $$;

-- Crear tabla unificada de dispositivos
CREATE TABLE IF NOT EXISTS metadata.devices (
    -- Identificadores primarios
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES metadata.tenants(id) ON DELETE CASCADE,
    greenhouse_id UUID NOT NULL REFERENCES metadata.greenhouses(id) ON DELETE CASCADE,
    sector_id UUID REFERENCES metadata.sectors(id) ON DELETE SET NULL,

    -- Identificacion del dispositivo
    device_code VARCHAR(50) NOT NULL,
    device_id VARCHAR(50) NOT NULL,

    -- Discriminador de categoria (SENSOR o ACTUATOR)
    device_category metadata.device_category NOT NULL,

    -- Tipo normalizado (FK a device_types)
    device_type_id SMALLINT NOT NULL REFERENCES metadata.device_types(id),

    -- Unidad normalizada (FK a units)
    unit_id SMALLINT REFERENCES metadata.units(id),

    -- Configuracion MQTT
    mqtt_topic VARCHAR(150),
    mqtt_command_topic VARCHAR(150),
    mqtt_field_name VARCHAR(100),

    -- Valores y estado
    current_value DOUBLE PRECISION,
    current_state VARCHAR(20),
    state_id SMALLINT REFERENCES metadata.actuator_states(id),

    -- Umbrales (principalmente para sensores)
    min_threshold NUMERIC(10,2),
    max_threshold NUMERIC(10,2),

    -- Formato de datos
    data_format VARCHAR(20) DEFAULT 'NUMERIC',

    -- Metadata fisica
    location_in_greenhouse VARCHAR(150),
    calibration_data JSONB,

    -- Estado operacional
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Timestamps de actividad
    last_seen TIMESTAMPTZ,
    last_command_at TIMESTAMPTZ,
    last_status_update TIMESTAMPTZ,

    -- Auditoria
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uq_device_code_per_greenhouse UNIQUE (greenhouse_id, device_code),
    CONSTRAINT uq_device_id_per_greenhouse UNIQUE (greenhouse_id, device_id),
    CONSTRAINT chk_actuator_fields CHECK (
        device_category = 'ACTUATOR' OR
        (current_state IS NULL AND mqtt_command_topic IS NULL AND state_id IS NULL AND last_command_at IS NULL)
    ),
    CONSTRAINT chk_sensor_thresholds CHECK (
        device_category = 'SENSOR' OR
        (min_threshold IS NULL AND max_threshold IS NULL)
    ),
    CONSTRAINT chk_data_format CHECK (
        data_format IN ('NUMERIC', 'BOOLEAN', 'STRING', 'JSON')
    ),
    CONSTRAINT chk_threshold_range CHECK (
        min_threshold IS NULL OR max_threshold IS NULL OR min_threshold <= max_threshold
    )
);

-- Indices principales
CREATE INDEX IF NOT EXISTS idx_devices_tenant_id ON metadata.devices(tenant_id);
CREATE INDEX IF NOT EXISTS idx_devices_greenhouse_id ON metadata.devices(greenhouse_id);
CREATE INDEX IF NOT EXISTS idx_devices_sector_id ON metadata.devices(sector_id) WHERE sector_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_devices_category ON metadata.devices(device_category);
CREATE INDEX IF NOT EXISTS idx_devices_type_id ON metadata.devices(device_type_id);
CREATE INDEX IF NOT EXISTS idx_devices_code ON metadata.devices(device_code);
CREATE INDEX IF NOT EXISTS idx_devices_hardware_id ON metadata.devices(device_id);

-- Indices compuestos para queries comunes
CREATE INDEX IF NOT EXISTS idx_devices_category_active ON metadata.devices(device_category, is_active);
CREATE INDEX IF NOT EXISTS idx_devices_greenhouse_category ON metadata.devices(greenhouse_id, device_category);
CREATE INDEX IF NOT EXISTS idx_devices_tenant_greenhouse ON metadata.devices(tenant_id, greenhouse_id);

-- Indices parciales para actuadores activos (optimiza queries de control)
CREATE INDEX IF NOT EXISTS idx_devices_actuators_active
    ON metadata.devices(greenhouse_id, device_type_id)
    WHERE device_category = 'ACTUATOR' AND is_active = TRUE;

-- Indices parciales para sensores activos (optimiza queries de telemetria)
CREATE INDEX IF NOT EXISTS idx_devices_sensors_active
    ON metadata.devices(greenhouse_id, device_type_id)
    WHERE device_category = 'SENSOR' AND is_active = TRUE;

-- Indice para detectar dispositivos sin comunicacion reciente
CREATE INDEX IF NOT EXISTS idx_devices_last_seen
    ON metadata.devices(last_seen)
    WHERE is_active = TRUE;

-- Indice para mqtt_field_name (usado en procesamiento MQTT)
CREATE INDEX IF NOT EXISTS idx_devices_mqtt_field
    ON metadata.devices(greenhouse_id, mqtt_field_name)
    WHERE mqtt_field_name IS NOT NULL;

-- Comentarios
COMMENT ON TABLE metadata.devices IS 'Tabla unificada para sensores y actuadores IoT';
COMMENT ON COLUMN metadata.devices.device_category IS 'Discriminador: SENSOR para lecturas, ACTUATOR para control';
COMMENT ON COLUMN metadata.devices.device_code IS 'Codigo unico del dispositivo dentro del greenhouse (ej: TEMP_01, FAN_02)';
COMMENT ON COLUMN metadata.devices.device_id IS 'ID del hardware fisico (ej: DHT22-SARA01-TEMP)';
COMMENT ON COLUMN metadata.devices.mqtt_command_topic IS 'Solo actuators: topic para enviar comandos de control';
COMMENT ON COLUMN metadata.devices.current_state IS 'Solo actuators: estado operacional (ON, OFF, AUTO, MANUAL, ERROR)';
COMMENT ON COLUMN metadata.devices.min_threshold IS 'Solo sensors: umbral minimo para alertas';
COMMENT ON COLUMN metadata.devices.max_threshold IS 'Solo sensors: umbral maximo para alertas';
