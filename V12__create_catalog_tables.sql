-- =====================================================
-- V12: CREATE CATALOG/LOOKUP TABLES (PostgreSQL)
-- =====================================================
-- Purpose: Create normalized lookup tables to replace VARCHAR columns with SMALLINT
-- Impact: Reduces storage by ~70% for repeated values (sensor_type, unit, etc.)
-- Target: PostgreSQL metadata database
-- Estimated execution time: <5 seconds
-- =====================================================

-- =====================================================
-- 1. UNITS CATALOG
-- =====================================================
-- Replaces: sensors.unit VARCHAR(20), actuators.unit VARCHAR(20)
-- Benefit: 25 bytes → 2 bytes per record

CREATE TABLE IF NOT EXISTS metadata.units (
    id SMALLSERIAL PRIMARY KEY,
    symbol VARCHAR(10) UNIQUE NOT NULL,
    name VARCHAR(30) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE metadata.units IS
'Catálogo de unidades de medida (°C, %, lux, etc.). Normaliza campos unit en sensors/actuators.';

COMMENT ON COLUMN metadata.units.symbol IS 'Símbolo de la unidad (ej: °C, %, lux)';
COMMENT ON COLUMN metadata.units.name IS 'Nombre completo de la unidad';

-- Índice para búsquedas por símbolo
CREATE INDEX idx_units_symbol ON metadata.units(symbol) WHERE is_active = TRUE;

-- Insertar unidades estándar
INSERT INTO metadata.units (symbol, name, description) VALUES
    ('°C', 'Celsius', 'Temperature in degrees Celsius'),
    ('°F', 'Fahrenheit', 'Temperature in degrees Fahrenheit'),
    ('%', 'Percentage', 'Percentage value (0-100)'),
    ('lux', 'Lux', 'Light intensity in lux'),
    ('hPa', 'Hectopascal', 'Atmospheric pressure in hectopascals'),
    ('ppm', 'Parts per million', 'Concentration in parts per million (CO2, etc.)'),
    ('W/m²', 'Watts per square meter', 'Solar radiation'),
    ('m/s', 'Meters per second', 'Wind speed'),
    ('mm', 'Millimeters', 'Precipitation'),
    ('unit', 'Generic unit', 'Dimensionless unit'),
    ('value', 'Value', 'Generic value without specific unit')
ON CONFLICT (symbol) DO NOTHING;

-- =====================================================
-- 2. SENSOR TYPES CATALOG
-- =====================================================
-- Replaces: sensors.sensor_type VARCHAR(30)
-- Benefit: 30+ bytes → 2 bytes per record
-- With 10M sensor_readings: ~280 MB saved

CREATE TABLE IF NOT EXISTS metadata.sensor_types (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(30) UNIQUE NOT NULL,
    description TEXT,
    default_unit_id SMALLINT REFERENCES metadata.units(id),
    data_type VARCHAR(20) DEFAULT 'NUMERIC' CHECK (data_type IN ('NUMERIC', 'BOOLEAN', 'STRING', 'JSON')),
    min_value DECIMAL(10,2),
    max_value DECIMAL(10,2),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE metadata.sensor_types IS
'Catálogo de tipos de sensores. Normaliza sensors.sensor_type y sensor_readings.sensor_type.';

COMMENT ON COLUMN metadata.sensor_types.data_type IS
'Tipo de dato del valor: NUMERIC (temperatura, humedad), BOOLEAN (estado), STRING (texto), JSON (complejo)';

COMMENT ON COLUMN metadata.sensor_types.min_value IS
'Valor mínimo esperado para validación (opcional)';

-- Índices
CREATE INDEX idx_sensor_types_name ON metadata.sensor_types(name) WHERE is_active = TRUE;
CREATE INDEX idx_sensor_types_unit ON metadata.sensor_types(default_unit_id);

-- Insertar tipos de sensores estándar
INSERT INTO metadata.sensor_types (name, description, default_unit_id, min_value, max_value) VALUES
    ('TEMPERATURE', 'Temperature sensor', (SELECT id FROM metadata.units WHERE symbol = '°C'), -50.00, 100.00),
    ('HUMIDITY', 'Relative humidity sensor', (SELECT id FROM metadata.units WHERE symbol = '%'), 0.00, 100.00),
    ('LIGHT', 'Light intensity sensor (PAR or illuminance)', (SELECT id FROM metadata.units WHERE symbol = 'lux'), 0.00, 100000.00),
    ('SOIL_MOISTURE', 'Soil moisture sensor', (SELECT id FROM metadata.units WHERE symbol = '%'), 0.00, 100.00),
    ('CO2', 'CO2 concentration sensor', (SELECT id FROM metadata.units WHERE symbol = 'ppm'), 0.00, 5000.00),
    ('PRESSURE', 'Atmospheric pressure sensor', (SELECT id FROM metadata.units WHERE symbol = 'hPa'), 900.00, 1100.00),
    ('WIND_SPEED', 'Wind speed sensor', (SELECT id FROM metadata.units WHERE symbol = 'm/s'), 0.00, 50.00),
    ('PRECIPITATION', 'Rain gauge', (SELECT id FROM metadata.units WHERE symbol = 'mm'), 0.00, 500.00),
    ('SOLAR_RADIATION', 'Solar radiation sensor', (SELECT id FROM metadata.units WHERE symbol = 'W/m²'), 0.00, 1500.00),
    ('SETPOINT', 'Setpoint value (target value for automation)', (SELECT id FROM metadata.units WHERE symbol = 'value'), NULL, NULL),
    ('SENSOR', 'Generic sensor type', (SELECT id FROM metadata.units WHERE symbol = 'unit'), NULL, NULL)
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- 3. ACTUATOR TYPES CATALOG
-- =====================================================
-- Replaces: actuators.actuator_type VARCHAR(30)
-- Benefit: Similar reduction as sensor_types

CREATE TABLE IF NOT EXISTS metadata.actuator_types (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(30) UNIQUE NOT NULL,
    description TEXT,
    default_unit_id SMALLINT REFERENCES metadata.units(id),
    control_type VARCHAR(20) DEFAULT 'BINARY' CHECK (control_type IN ('BINARY', 'CONTINUOUS', 'MULTI_STATE')),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE metadata.actuator_types IS
'Catálogo de tipos de actuadores (ventiladores, calefacción, riego, etc.)';

COMMENT ON COLUMN metadata.actuator_types.control_type IS
'BINARY (ON/OFF), CONTINUOUS (0-100%), MULTI_STATE (LOW/MEDIUM/HIGH)';

-- Índices
CREATE INDEX idx_actuator_types_name ON metadata.actuator_types(name) WHERE is_active = TRUE;

-- Insertar tipos de actuadores estándar
INSERT INTO metadata.actuator_types (name, description, default_unit_id, control_type) VALUES
    ('VENTILATOR', 'Ventilation fan (exhaust or circulation)', (SELECT id FROM metadata.units WHERE symbol = '%'), 'CONTINUOUS'),
    ('HEATER', 'Heating system', (SELECT id FROM metadata.units WHERE symbol = '°C'), 'CONTINUOUS'),
    ('COOLER', 'Cooling system', (SELECT id FROM metadata.units WHERE symbol = '°C'), 'CONTINUOUS'),
    ('IRRIGATOR', 'Irrigation system / water pump', (SELECT id FROM metadata.units WHERE symbol = '%'), 'CONTINUOUS'),
    ('LIGHT', 'Artificial lighting (grow lights)', (SELECT id FROM metadata.units WHERE symbol = '%'), 'CONTINUOUS'),
    ('WINDOW', 'Automatic window opener', (SELECT id FROM metadata.units WHERE symbol = '%'), 'CONTINUOUS'),
    ('PUMP', 'Water pump (generic)', (SELECT id FROM metadata.units WHERE symbol = '%'), 'CONTINUOUS'),
    ('VALVE', 'Control valve (water, gas, etc.)', (SELECT id FROM metadata.units WHERE symbol = '%'), 'CONTINUOUS'),
    ('SHADE', 'Shade screen / curtain', (SELECT id FROM metadata.units WHERE symbol = '%'), 'CONTINUOUS'),
    ('HUMIDIFIER', 'Humidification system', (SELECT id FROM metadata.units WHERE symbol = '%'), 'CONTINUOUS'),
    ('DEHUMIDIFIER', 'Dehumidification system', (SELECT id FROM metadata.units WHERE symbol = '%'), 'CONTINUOUS'),
    ('CO2_INJECTOR', 'CO2 injection system', (SELECT id FROM metadata.units WHERE symbol = 'ppm'), 'CONTINUOUS'),
    ('FAN', 'Generic fan', (SELECT id FROM metadata.units WHERE symbol = '%'), 'CONTINUOUS'),
    ('RELAY', 'Generic relay (ON/OFF)', (SELECT id FROM metadata.units WHERE symbol = 'unit'), 'BINARY')
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- 4. ACTUATOR STATES CATALOG
-- =====================================================
-- Replaces: actuators.current_state VARCHAR(20)
-- Benefit: Standardizes possible states + reduces storage

CREATE TABLE IF NOT EXISTS metadata.actuator_states (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(20) UNIQUE NOT NULL,
    description TEXT,
    is_operational BOOLEAN DEFAULT TRUE,
    display_order SMALLINT,
    color VARCHAR(7),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE metadata.actuator_states IS
'Catálogo de estados posibles para actuadores';

COMMENT ON COLUMN metadata.actuator_states.is_operational IS
'TRUE si el estado es operativo (ON, OFF, AUTO), FALSE si es error/mantenimiento';

-- Insertar estados estándar
INSERT INTO metadata.actuator_states (name, description, is_operational, display_order, color) VALUES
    ('OFF', 'Actuator is off / inactive', TRUE, 1, '#6b7280'),
    ('ON', 'Actuator is on / active', TRUE, 2, '#10b981'),
    ('AUTO', 'Automatic mode (controlled by automation rules)', TRUE, 3, '#3b82f6'),
    ('MANUAL', 'Manual mode (controlled by user)', TRUE, 4, '#f59e0b'),
    ('ERROR', 'Error state (malfunction detected)', FALSE, 5, '#ef4444'),
    ('MAINTENANCE', 'Maintenance mode (disabled for maintenance)', FALSE, 6, '#8b5cf6'),
    ('STANDBY', 'Standby mode (ready but inactive)', TRUE, 7, '#6366f1'),
    ('CALIBRATING', 'Calibration in progress', FALSE, 8, '#ec4899'),
    ('OFFLINE', 'Device offline / disconnected', FALSE, 9, '#dc2626')
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- 5. ALERT SEVERITIES CATALOG
-- =====================================================
-- Replaces: alerts.severity VARCHAR(20)
-- Benefit: Standardizes severity levels + enables sorting by level

CREATE TABLE IF NOT EXISTS metadata.alert_severities (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(20) UNIQUE NOT NULL,
    level SMALLINT NOT NULL UNIQUE,
    description TEXT,
    color VARCHAR(7),
    requires_action BOOLEAN DEFAULT FALSE,
    notification_delay_minutes INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE metadata.alert_severities IS
'Catálogo de niveles de severidad de alertas (INFO < WARNING < ERROR < CRITICAL)';

COMMENT ON COLUMN metadata.alert_severities.level IS
'Nivel numérico para ordenamiento (1=INFO, 2=WARNING, 3=ERROR, 4=CRITICAL)';

COMMENT ON COLUMN metadata.alert_severities.notification_delay_minutes IS
'Minutos de espera antes de enviar notificación (evitar spam en INFO)';

-- Insertar niveles de severidad estándar
INSERT INTO metadata.alert_severities (name, level, description, color, requires_action, notification_delay_minutes) VALUES
    ('INFO', 1, 'Informational alert - no action required', '#0ea5e9', FALSE, 60),
    ('WARNING', 2, 'Warning - attention recommended', '#f59e0b', FALSE, 15),
    ('ERROR', 3, 'Error - action required', '#ef4444', TRUE, 5),
    ('CRITICAL', 4, 'Critical - immediate action required', '#dc2626', TRUE, 0)
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- 6. ALERT TYPES CATALOG
-- =====================================================
-- Replaces: alerts.alert_type VARCHAR(50)
-- Benefit: Standardizes alert types + default severity

CREATE TABLE IF NOT EXISTS metadata.alert_types (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    default_severity_id SMALLINT REFERENCES metadata.alert_severities(id),
    category VARCHAR(30),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE metadata.alert_types IS
'Catálogo de tipos de alertas posibles';

COMMENT ON COLUMN metadata.alert_types.category IS
'Categoría: SENSOR, ACTUATOR, SYSTEM, CONNECTIVITY, THRESHOLD';

-- Índices
CREATE INDEX idx_alert_types_category ON metadata.alert_types(category) WHERE is_active = TRUE;

-- Insertar tipos de alertas estándar
INSERT INTO metadata.alert_types (name, description, default_severity_id, category) VALUES
    ('SENSOR_OFFLINE', 'Sensor not sending data', (SELECT id FROM metadata.alert_severities WHERE name = 'ERROR'), 'CONNECTIVITY'),
    ('ACTUATOR_OFFLINE', 'Actuator not responding', (SELECT id FROM metadata.alert_severities WHERE name = 'ERROR'), 'CONNECTIVITY'),
    ('THRESHOLD_EXCEEDED_HIGH', 'Sensor value above maximum threshold', (SELECT id FROM metadata.alert_severities WHERE name = 'WARNING'), 'THRESHOLD'),
    ('THRESHOLD_EXCEEDED_LOW', 'Sensor value below minimum threshold', (SELECT id FROM metadata.alert_severities WHERE name = 'WARNING'), 'THRESHOLD'),
    ('CRITICAL_THRESHOLD_HIGH', 'Sensor value critically high', (SELECT id FROM metadata.alert_severities WHERE name = 'CRITICAL'), 'THRESHOLD'),
    ('CRITICAL_THRESHOLD_LOW', 'Sensor value critically low', (SELECT id FROM metadata.alert_severities WHERE name = 'CRITICAL'), 'THRESHOLD'),
    ('ACTUATOR_ERROR', 'Actuator malfunction detected', (SELECT id FROM metadata.alert_severities WHERE name = 'ERROR'), 'ACTUATOR'),
    ('SENSOR_ERROR', 'Sensor malfunction detected', (SELECT id FROM metadata.alert_severities WHERE name = 'ERROR'), 'SENSOR'),
    ('CONNECTIVITY_LOST', 'Greenhouse connectivity lost', (SELECT id FROM metadata.alert_severities WHERE name = 'CRITICAL'), 'CONNECTIVITY'),
    ('BATTERY_LOW', 'Device battery low', (SELECT id FROM metadata.alert_severities WHERE name = 'WARNING'), 'SYSTEM'),
    ('MAINTENANCE_DUE', 'Maintenance is due', (SELECT id FROM metadata.alert_severities WHERE name = 'INFO'), 'SYSTEM'),
    ('CALIBRATION_REQUIRED', 'Sensor calibration required', (SELECT id FROM metadata.alert_severities WHERE name = 'WARNING'), 'SENSOR'),
    ('DATA_ANOMALY', 'Unusual data pattern detected', (SELECT id FROM metadata.alert_severities WHERE name = 'WARNING'), 'SENSOR')
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- 7. GRANT PERMISSIONS (adjust as needed)
-- =====================================================

-- Grant read access to all users
GRANT SELECT ON ALL TABLES IN SCHEMA metadata TO PUBLIC;

-- Grant write access to application role (if exists)
-- GRANT INSERT, UPDATE ON metadata.units TO app_role;
-- GRANT INSERT, UPDATE ON metadata.sensor_types TO app_role;
-- etc.

-- =====================================================
-- 8. VERIFICATION QUERIES
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '================================================================';
    RAISE NOTICE 'V12: CATALOG TABLES CREATED SUCCESSFULLY';
    RAISE NOTICE '================================================================';
    RAISE NOTICE 'Tables created:';
    RAISE NOTICE '  - metadata.units (% rows)', (SELECT COUNT(*) FROM metadata.units);
    RAISE NOTICE '  - metadata.sensor_types (% rows)', (SELECT COUNT(*) FROM metadata.sensor_types);
    RAISE NOTICE '  - metadata.actuator_types (% rows)', (SELECT COUNT(*) FROM metadata.actuator_types);
    RAISE NOTICE '  - metadata.actuator_states (% rows)', (SELECT COUNT(*) FROM metadata.actuator_states);
    RAISE NOTICE '  - metadata.alert_severities (% rows)', (SELECT COUNT(*) FROM metadata.alert_severities);
    RAISE NOTICE '  - metadata.alert_types (% rows)', (SELECT COUNT(*) FROM metadata.alert_types);
    RAISE NOTICE '';
    RAISE NOTICE 'Next steps:';
    RAISE NOTICE '  1. Execute V13__normalize_existing_tables.sql';
    RAISE NOTICE '  2. Migrate data from VARCHAR columns to *_id columns';
    RAISE NOTICE '  3. Update application queries to use new catalog tables';
    RAISE NOTICE '================================================================';
END $$;
