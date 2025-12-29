-- ============================================================================
-- V18: Crear tabla unificada de tipos de dispositivos
-- Fecha: 2025-12-29
-- Descripcion: Catalogo unificado para tipos de sensores y actuadores
-- Referencia: https://www.postgresql.org/docs/current/datatype-enum.html
-- ============================================================================

-- Crear ENUM para categoria de dispositivo
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'device_category') THEN
        CREATE TYPE metadata.device_category AS ENUM ('SENSOR', 'ACTUATOR');
    END IF;
END $$;

-- Crear tabla de tipos de dispositivos
CREATE TABLE IF NOT EXISTS metadata.device_types (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL,
    description TEXT,
    category metadata.device_category NOT NULL,
    default_unit_id SMALLINT REFERENCES metadata.units(id),

    -- Solo para SENSOR
    data_type VARCHAR(20) DEFAULT 'DECIMAL',
    min_expected_value NUMERIC(10,2),
    max_expected_value NUMERIC(10,2),

    -- Solo para ACTUATOR
    control_type VARCHAR(20),

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_device_types_name UNIQUE (name),
    CONSTRAINT chk_device_types_data_type CHECK (
        data_type IS NULL OR data_type IN ('DECIMAL', 'INTEGER', 'BOOLEAN', 'TEXT', 'JSON')
    ),
    CONSTRAINT chk_device_types_control_type CHECK (
        control_type IS NULL OR control_type IN ('BINARY', 'CONTINUOUS', 'MULTI_STATE')
    )
);

-- Indices
CREATE INDEX IF NOT EXISTS idx_device_types_category ON metadata.device_types(category);
CREATE INDEX IF NOT EXISTS idx_device_types_active ON metadata.device_types(is_active) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_device_types_name ON metadata.device_types(name);

-- Comentarios
COMMENT ON TABLE metadata.device_types IS 'Catalogo unificado de tipos de dispositivos (sensores y actuadores)';
COMMENT ON COLUMN metadata.device_types.category IS 'SENSOR para lecturas, ACTUATOR para control';
COMMENT ON COLUMN metadata.device_types.control_type IS 'BINARY (on/off), CONTINUOUS (0-100%), MULTI_STATE';
COMMENT ON COLUMN metadata.device_types.data_type IS 'Tipo de dato que genera el sensor';
COMMENT ON COLUMN metadata.device_types.min_expected_value IS 'Valor minimo fisicamente posible';
COMMENT ON COLUMN metadata.device_types.max_expected_value IS 'Valor maximo fisicamente posible';

-- Datos iniciales para SENSORES
INSERT INTO metadata.device_types (name, description, category, default_unit_id, data_type, min_expected_value, max_expected_value) VALUES
    ('TEMPERATURE', 'Sensor de temperatura ambiente', 'SENSOR',
        (SELECT id FROM metadata.units WHERE symbol = '°C'), 'DECIMAL', -50, 100),
    ('HUMIDITY', 'Sensor de humedad relativa', 'SENSOR',
        (SELECT id FROM metadata.units WHERE symbol = '%'), 'DECIMAL', 0, 100),
    ('SOIL_MOISTURE', 'Sensor de humedad del suelo', 'SENSOR',
        (SELECT id FROM metadata.units WHERE symbol = '%'), 'DECIMAL', 0, 100),
    ('LIGHT_INTENSITY', 'Sensor de intensidad luminica', 'SENSOR',
        (SELECT id FROM metadata.units WHERE symbol = 'lux'), 'DECIMAL', 0, 200000),
    ('CO2_LEVEL', 'Sensor de dioxido de carbono', 'SENSOR',
        (SELECT id FROM metadata.units WHERE symbol = 'ppm'), 'DECIMAL', 0, 5000),
    ('ATMOSPHERIC_PRESSURE', 'Sensor de presion atmosferica', 'SENSOR',
        (SELECT id FROM metadata.units WHERE symbol = 'hPa'), 'DECIMAL', 800, 1100),
    ('WIND_SPEED', 'Sensor de velocidad del viento', 'SENSOR',
        (SELECT id FROM metadata.units WHERE symbol = 'm/s'), 'DECIMAL', 0, 200),
    ('WIND_DIRECTION', 'Sensor de direccion del viento', 'SENSOR',
        (SELECT id FROM metadata.units WHERE symbol = '°'), 'DECIMAL', 0, 360),
    ('RAINFALL', 'Pluviometro', 'SENSOR',
        (SELECT id FROM metadata.units WHERE symbol = 'mm'), 'DECIMAL', 0, 500),
    ('SOLAR_RADIATION', 'Sensor de radiacion solar', 'SENSOR',
        (SELECT id FROM metadata.units WHERE symbol = 'W/m²'), 'DECIMAL', 0, 1500),
    ('PH', 'Sensor de pH', 'SENSOR',
        (SELECT id FROM metadata.units WHERE symbol = 'pH'), 'DECIMAL', 0, 14),
    ('EC', 'Sensor de conductividad electrica', 'SENSOR',
        (SELECT id FROM metadata.units WHERE symbol = 'mS/cm'), 'DECIMAL', 0, 20),
    ('UV_INDEX', 'Sensor de indice UV', 'SENSOR', NULL, 'DECIMAL', 0, 15)
ON CONFLICT (name) DO NOTHING;

-- Datos iniciales para ACTUADORES
INSERT INTO metadata.device_types (name, description, category, default_unit_id, control_type) VALUES
    ('VENTILATOR', 'Ventilador de invernadero', 'ACTUATOR',
        (SELECT id FROM metadata.units WHERE symbol = '%'), 'CONTINUOUS'),
    ('FAN', 'Ventilador auxiliar', 'ACTUATOR',
        (SELECT id FROM metadata.units WHERE symbol = '%'), 'CONTINUOUS'),
    ('HEATER', 'Calefactor', 'ACTUATOR',
        (SELECT id FROM metadata.units WHERE symbol = 'W'), 'CONTINUOUS'),
    ('COOLER', 'Sistema de enfriamiento', 'ACTUATOR',
        (SELECT id FROM metadata.units WHERE symbol = 'W'), 'CONTINUOUS'),
    ('IRRIGATOR', 'Sistema de riego', 'ACTUATOR',
        (SELECT id FROM metadata.units WHERE symbol = 'L/h'), 'CONTINUOUS'),
    ('LIGHTING', 'Iluminacion suplementaria', 'ACTUATOR',
        (SELECT id FROM metadata.units WHERE symbol = 'W'), 'BINARY'),
    ('CURTAIN', 'Cortina/Sombra', 'ACTUATOR',
        (SELECT id FROM metadata.units WHERE symbol = '%'), 'CONTINUOUS'),
    ('WINDOW', 'Ventana motorizada', 'ACTUATOR',
        (SELECT id FROM metadata.units WHERE symbol = '%'), 'CONTINUOUS'),
    ('VALVE', 'Valvula de control', 'ACTUATOR', NULL, 'BINARY'),
    ('PUMP', 'Bomba', 'ACTUATOR',
        (SELECT id FROM metadata.units WHERE symbol = 'L/h'), 'BINARY'),
    ('EXTRACTOR', 'Extractor de aire', 'ACTUATOR',
        (SELECT id FROM metadata.units WHERE symbol = '%'), 'CONTINUOUS'),
    ('MISTING', 'Sistema de nebulizacion', 'ACTUATOR', NULL, 'BINARY'),
    ('DEHUMIDIFIER', 'Deshumidificador', 'ACTUATOR',
        (SELECT id FROM metadata.units WHERE symbol = '%'), 'CONTINUOUS'),
    ('CO2_INJECTOR', 'Inyector de CO2', 'ACTUATOR',
        (SELECT id FROM metadata.units WHERE symbol = 'ppm'), 'CONTINUOUS')
ON CONFLICT (name) DO NOTHING;
