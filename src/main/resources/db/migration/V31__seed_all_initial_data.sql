-- =============================================================================
-- V31: Seed system and catalog data required for any fresh deployment
-- =============================================================================
-- Seeds ONLY data that the system needs to boot and function correctly:
--   1. Catalog tables (units, device types, actuator states, etc.)
--   2. MQTT system accounts required for EMQX authentication
--
-- Business data (tenants, greenhouses, users) is NOT seeded here.
-- It is created at runtime via the API.
--
-- All INSERTs use ON CONFLICT DO NOTHING — safe to run multiple times.
--
-- Generated from: greenhouse_metadata_dev
-- Generated on:   2026-02-23
-- =============================================================================


-- metadata.actuator_states
INSERT INTO metadata.actuator_states (id, name, description, is_operational, display_order, color, created_at) VALUES (1, 'OFF', 'Apagado', false, 1, '#808080', '2025-12-30 00:01:19.766726+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.actuator_states (id, name, description, is_operational, display_order, color, created_at) VALUES (2, 'ON', 'Encendido', true, 2, '#00FF00', '2025-12-30 00:01:19.766726+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.actuator_states (id, name, description, is_operational, display_order, color, created_at) VALUES (3, 'AUTO', 'Modo automatico', true, 3, '#0066FF', '2025-12-30 00:01:19.766726+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.actuator_states (id, name, description, is_operational, display_order, color, created_at) VALUES (4, 'MANUAL', 'Control manual', true, 4, '#FFA500', '2025-12-30 00:01:19.766726+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.actuator_states (id, name, description, is_operational, display_order, color, created_at) VALUES (5, 'ERROR', 'Error operacional', false, 5, '#FF0000', '2025-12-30 00:01:19.766726+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.actuator_states (id, name, description, is_operational, display_order, color, created_at) VALUES (6, 'MAINTENANCE', 'En mantenimiento', false, 6, '#FFFF00', '2025-12-30 00:01:19.766726+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.actuator_states (id, name, description, is_operational, display_order, color, created_at) VALUES (7, 'STANDBY', 'En espera', false, 7, '#ADD8E6', '2025-12-30 00:01:19.766726+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.actuator_states (id, name, description, is_operational, display_order, color, created_at) VALUES (8, 'CALIBRATING', 'Calibrando', false, 8, '#9932CC', '2025-12-30 00:01:19.766726+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.actuator_states (id, name, description, is_operational, display_order, color, created_at) VALUES (9, 'OFFLINE', 'Sin conexion', false, 9, '#000000', '2025-12-30 00:01:19.766726+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.actuator_states (id, name, description, is_operational, display_order, color, created_at) VALUES (10, 'UNKNOWN', 'Estado desconocido', false, 10, '#C0C0C0', '2025-12-30 00:01:19.766726+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.actuator_states (id, name, description, is_operational, display_order, color, created_at) VALUES (14, 'DOWN', 'Bajar la temperatura', false, 0, '#FFFFFF', '2026-01-15 12:25:41.610751+00') ON CONFLICT DO NOTHING;

-- metadata.alert_types
INSERT INTO metadata.alert_types (id, name, description) VALUES (1, 'THRESHOLD EXCEEDED', NULL) ON CONFLICT DO NOTHING;
INSERT INTO metadata.alert_types (id, name, description) VALUES (2, 'SENSOR OFFLINE', NULL) ON CONFLICT DO NOTHING;
INSERT INTO metadata.alert_types (id, name, description) VALUES (3, 'ACTUATOR FAILURE', NULL) ON CONFLICT DO NOTHING;
INSERT INTO metadata.alert_types (id, name, description) VALUES (4, 'SYSTEM ERROR', NULL) ON CONFLICT DO NOTHING;
INSERT INTO metadata.alert_types (id, name, description) VALUES (5, 'WARNING', NULL) ON CONFLICT DO NOTHING;
INSERT INTO metadata.alert_types (id, name, description) VALUES (6, 'INFO', NULL) ON CONFLICT DO NOTHING;

-- metadata.alert_severities
INSERT INTO metadata.alert_severities (id, name, level, description, color, requires_action, notification_delay_minutes, created_at) VALUES (1, 'INFO', 1, NULL, '#0066FF', false, 0, '2025-12-31 16:35:00.740048+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.alert_severities (id, name, level, description, color, requires_action, notification_delay_minutes, created_at) VALUES (2, 'WARNING', 2, NULL, '#FFA500', false, 0, '2025-12-31 16:35:00.740048+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.alert_severities (id, name, level, description, color, requires_action, notification_delay_minutes, created_at) VALUES (3, 'ERROR', 3, NULL, '#FF7722', false, 0, '2025-12-31 16:35:00.740048+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.alert_severities (id, name, level, description, color, requires_action, notification_delay_minutes, created_at) VALUES (4, 'CRITICAL', 4, NULL, '#FF0000', false, 0, '2025-12-31 16:35:00.740048+00') ON CONFLICT DO NOTHING;

-- metadata.data_types
INSERT INTO metadata.data_types (id, name, description, validation_regex, example_value, display_order, is_active, created_at) VALUES (1, 'INTEGER', 'Numero entero (32 bits)', '^-?\d+$', '25', 1, true, '2026-01-15 21:46:23.12214+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.data_types (id, name, description, validation_regex, example_value, display_order, is_active, created_at) VALUES (2, 'LONG', 'Numero entero grande (64 bits)', '^-?\d+$', '9223372036854775807', 2, true, '2026-01-15 21:46:23.12214+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.data_types (id, name, description, validation_regex, example_value, display_order, is_active, created_at) VALUES (3, 'DOUBLE', 'Numero decimal de precision doble', '^-?\d+(\.\d+)?$', '25.5', 3, true, '2026-01-15 21:46:23.12214+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.data_types (id, name, description, validation_regex, example_value, display_order, is_active, created_at) VALUES (4, 'BOOLEAN', 'Valor verdadero o falso', '^(true|false|TRUE|FALSE|1|0)$', 'true', 4, true, '2026-01-15 21:46:23.12214+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.data_types (id, name, description, validation_regex, example_value, display_order, is_active, created_at) VALUES (5, 'STRING', 'Cadena de texto', '.*', 'Invernadero Norte', 5, true, '2026-01-15 21:46:23.12214+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.data_types (id, name, description, validation_regex, example_value, display_order, is_active, created_at) VALUES (6, 'DATE', 'Fecha en formato ISO 8601', '^\d{4}-\d{2}-\d{2}$', '2026-01-15', 6, true, '2026-01-15 21:46:23.12214+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.data_types (id, name, description, validation_regex, example_value, display_order, is_active, created_at) VALUES (7, 'TIME', 'Hora en formato HH:mm:ss', '^\d{2}:\d{2}(:\d{2})?$', '14:30:00', 7, true, '2026-01-15 21:46:23.12214+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.data_types (id, name, description, validation_regex, example_value, display_order, is_active, created_at) VALUES (8, 'DATETIME', 'Fecha y hora en formato ISO 8601', '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?', '2026-01-15T14:30:00', 8, true, '2026-01-15 21:46:23.12214+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.data_types (id, name, description, validation_regex, example_value, display_order, is_active, created_at) VALUES (9, 'JSON', 'Objeto JSON valido', '^\{.*\}$|^\[.*\]$', '{"key": "value"}', 9, true, '2026-01-15 21:46:23.12214+00') ON CONFLICT DO NOTHING;

-- metadata.device_categories
INSERT INTO metadata.device_categories (id, name) VALUES (1, 'SENSOR') ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_categories (id, name) VALUES (2, 'ACTUATOR') ON CONFLICT DO NOTHING;

-- metadata.units
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (1, '°C', 'Grados Celsius', 'Temperatura en grados Celsius', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (2, '°F', 'Grados Fahrenheit', 'Temperatura en grados Fahrenheit', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (3, '%', 'Porcentaje', 'Porcentaje (humedad, apertura, velocidad)', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (4, 'hPa', 'Hectopascales', 'Presion atmosferica', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (5, 'ppm', 'Partes por millon', 'Concentracion de CO2 u otros gases', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (6, 'lux', 'lx', 'Intensidad luminica', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (7, 'W/m²', 'Vatios por metro cuadrado', 'Radiacion solar', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (8, 'm/s', 'Metros por segundo', 'Velocidad del viento', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (9, 'km/h', 'Kilometros por hora', 'Velocidad del viento', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (10, 'mm', 'Milimetros', 'Precipitacion', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (11, 'W', 'Vatios', 'Potencia electrica', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (12, 'kW', 'Kilovatios', 'Potencia electrica', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (13, 'L/h', 'Litros por hora', 'Caudal de riego', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (14, 'L/min', 'Litros por minuto', 'Caudal de riego', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (15, 'm³/h', 'Metros cubicos por hora', 'Caudal de aire', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (16, 'RPM', 'Revoluciones por minuto', 'Velocidad de motor', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (17, 'pH', 'pH', 'Acidez/alcalinidad', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (18, 'mS/cm', 'Milisiemens por centimetro', 'Conductividad electrica', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (19, 'dS/m', 'Decisiemens por metro', 'Conductividad electrica', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (20, 'bar', 'Bar', 'Presion', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.units (id, symbol, name, description, is_active, created_at) VALUES (21, '°', 'Grados', 'Angulo de apertura', true, '2025-12-30 00:01:19.562745+00') ON CONFLICT DO NOTHING;

-- metadata.periods
INSERT INTO metadata.periods (id, name) VALUES (1, 'DAY') ON CONFLICT DO NOTHING;
INSERT INTO metadata.periods (id, name) VALUES (2, 'NIGHT') ON CONFLICT DO NOTHING;
INSERT INTO metadata.periods (id, name) VALUES (3, 'ALL') ON CONFLICT DO NOTHING;

-- metadata.device_types (depends on: units, device_categories)
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (1, 'TEMPERATURE', 'Sensor de temperatura ambiente', 1, 'DECIMAL', -50.00, 100.00, NULL, true, '2025-12-30 00:01:20.060541+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (2, 'HUMIDITY', 'Sensor de humedad relativa', 3, 'DECIMAL', 0.00, 100.00, NULL, true, '2025-12-30 00:01:20.060541+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (3, 'SOIL MOISTURE', 'Sensor de humedad del suelo', 3, 'DECIMAL', 0.00, 100.00, NULL, true, '2025-12-30 00:01:20.060541+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (4, 'LIGHT INTENSITY', 'Sensor de intensidad luminica', 6, 'DECIMAL', 0.00, 200000.00, NULL, true, '2025-12-30 00:01:20.060541+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (5, 'CO2 LEVEL', 'Sensor de dioxido de carbono', 5, 'DECIMAL', 0.00, 5000.00, NULL, true, '2025-12-30 00:01:20.060541+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (6, 'ATMOSPHERIC PRESSURE', 'Sensor de presion atmosferica', 4, 'DECIMAL', 800.00, 1100.00, NULL, true, '2025-12-30 00:01:20.060541+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (7, 'WIND SPEED', 'Sensor de velocidad del viento', 8, 'DECIMAL', 0.00, 200.00, NULL, true, '2025-12-30 00:01:20.060541+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (8, 'WIND DIRECTION', 'Sensor de direccion del viento', 21, 'DECIMAL', 0.00, 360.00, NULL, true, '2025-12-30 00:01:20.060541+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (9, 'RAINFALL', 'Pluviometro', 10, 'DECIMAL', 0.00, 500.00, NULL, true, '2025-12-30 00:01:20.060541+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (10, 'SOLAR RADIATION', 'Sensor de radiacion solar', 7, 'DECIMAL', 0.00, 1500.00, NULL, true, '2025-12-30 00:01:20.060541+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (11, 'PH', 'Sensor de pH', 17, 'DECIMAL', 0.00, 14.00, NULL, true, '2025-12-30 00:01:20.060541+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (12, 'EC', 'Sensor de conductividad electrica', 18, 'DECIMAL', 0.00, 20.00, NULL, true, '2025-12-30 00:01:20.060541+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (13, 'UV INDEX', 'Sensor de indice UV', NULL, 'DECIMAL', 0.00, 15.00, NULL, true, '2025-12-30 00:01:20.060541+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (14, 'VENTILATOR', 'Ventilador de invernadero', 3, 'DECIMAL', NULL, NULL, 'CONTINUOUS', true, '2025-12-30 00:01:20.060541+00', 2) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (15, 'FAN', 'Ventilador auxiliar', 3, 'DECIMAL', NULL, NULL, 'CONTINUOUS', true, '2025-12-30 00:01:20.060541+00', 2) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (16, 'HEATER', 'Calefactor', 11, 'DECIMAL', NULL, NULL, 'CONTINUOUS', true, '2025-12-30 00:01:20.060541+00', 2) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (17, 'COOLER', 'Sistema de enfriamiento', 11, 'DECIMAL', NULL, NULL, 'CONTINUOUS', true, '2025-12-30 00:01:20.060541+00', 2) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (18, 'IRRIGATOR', 'Sistema de riego', 13, 'DECIMAL', NULL, NULL, 'CONTINUOUS', true, '2025-12-30 00:01:20.060541+00', 2) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (19, 'LIGHTING', 'Iluminacion suplementaria', 11, 'DECIMAL', NULL, NULL, 'BINARY', true, '2025-12-30 00:01:20.060541+00', 2) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (20, 'CURTAIN', 'Cortina/Sombra', 3, 'DECIMAL', NULL, NULL, 'CONTINUOUS', true, '2025-12-30 00:01:20.060541+00', 2) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (21, 'WINDOW', 'Ventana motorizada', 3, 'DECIMAL', NULL, NULL, 'CONTINUOUS', true, '2025-12-30 00:01:20.060541+00', 2) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (22, 'VALVE', 'Valvula de control', NULL, 'DECIMAL', NULL, NULL, 'BINARY', true, '2025-12-30 00:01:20.060541+00', 2) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (23, 'PUMP', 'Bomba', 13, 'DECIMAL', NULL, NULL, 'BINARY', true, '2025-12-30 00:01:20.060541+00', 2) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (24, 'EXTRACTOR', 'Extractor de aire', 3, 'DECIMAL', NULL, NULL, 'CONTINUOUS', true, '2025-12-30 00:01:20.060541+00', 2) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (25, 'MISTING', 'Sistema de nebulizacion', NULL, 'DECIMAL', NULL, NULL, 'BINARY', true, '2025-12-30 00:01:20.060541+00', 2) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (26, 'DEHUMIDIFIER', 'Deshumidificador', 3, 'DECIMAL', NULL, NULL, 'CONTINUOUS', true, '2025-12-30 00:01:20.060541+00', 2) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (27, 'CO2 INJECTOR', 'Inyector de CO2', 5, 'DECIMAL', NULL, NULL, 'CONTINUOUS', true, '2025-12-30 00:01:20.060541+00', 2) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (28, 'TEST', 'borrar, es solo un test', 3, 'DECIMAL', NULL, NULL, NULL, false, '2026-01-06 01:13:17.773172+00', 2) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (30, 'WIND GUST', 'Racha de viento', 8, 'DECIMAL', NULL, NULL, NULL, true, '2026-01-09 16:46:26.916817+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (31, 'DEWPOINT', 'Punto de rocio', 1, 'DECIMAL', NULL, NULL, NULL, true, '2026-01-09 16:48:58.443728+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (32, 'ILLUMINANCE', 'Iluminancia', 6, 'DECIMAL', NULL, NULL, NULL, true, '2026-01-09 16:50:00.73753+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (33, 'LIFE BEAT', 'Pulso de vida', NULL, 'DECIMAL', NULL, NULL, NULL, true, '2026-01-09 16:59:53.907011+00', 1) ON CONFLICT DO NOTHING;
INSERT INTO metadata.device_types (id, name, description, default_unit_id, data_type, min_expected_value, max_expected_value, control_type, is_active, created_at, category_id) VALUES (34, 'TEMPERATURA DE DIA', 'Tempratura de dia', 1, 'DECIMAL', NULL, NULL, NULL, true, '2026-01-15 12:26:31.008056+00', 1) ON CONFLICT DO NOTHING;

-- =============================================================================
-- MQTT system accounts — required for EMQX authentication to work
-- These are service accounts, not user accounts
-- =============================================================================

-- metadata.mqtt_users
INSERT INTO metadata.mqtt_users (id, username, password_hash, salt, device_type, is_active, created_at, updated_at, last_connected_at, greenhouse_id, tenant_id) VALUES ('11cd0457-a5f4-4f81-913a-b3f51ece80b0', 'admin', 'b5ecf4e693f7b046e54badcd91040bfeeb74cd49947fba78a863e4577fb5f250', 'apptolast', 'API', true, '2025-11-03 18:21:52.046038+00', '2025-11-03 18:21:52.046038+00', NULL, NULL, NULL) ON CONFLICT DO NOTHING;
INSERT INTO metadata.mqtt_users (id, username, password_hash, salt, device_type, is_active, created_at, updated_at, last_connected_at, greenhouse_id, tenant_id) VALUES ('8bb9f453-eaef-491f-aa31-028c156c596f', 'api_spring_boot', 'f12ee30aa778b68f51438466fd13ee34a6ec35fc4dfc7d965f766dd78329da53', 'apptolast', 'API', true, '2025-11-08 16:57:35.048156+00', '2025-11-08 16:57:35.048156+00', NULL, NULL, NULL) ON CONFLICT DO NOTHING;

-- metadata.mqtt_acl
INSERT INTO metadata.mqtt_acl (id, username, permission, action, topic, qos, created_at) VALUES ('f239ad78-254d-48ce-a8e1-cf2aa51f2e2a', 'admin', 'allow', 'pubsub', '#', 2, '2025-11-03 18:21:52.046038+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.mqtt_acl (id, username, permission, action, topic, qos, created_at) VALUES ('1f74ee21-1973-4f77-b6c5-103eb6adf73a', 'api_spring_boot', 'allow', 'publish', '#', 2, '2025-11-08 16:57:51.131111+00') ON CONFLICT DO NOTHING;
INSERT INTO metadata.mqtt_acl (id, username, permission, action, topic, qos, created_at) VALUES ('acf98402-0435-4b0f-9fe1-e17d2e2fca96', 'api_spring_boot', 'allow', 'subscribe', '#', 2, '2025-11-08 16:57:51.131111+00') ON CONFLICT DO NOTHING;

-- =============================================================================
-- Reset sequences to avoid PK conflicts when inserting new records via API
-- =============================================================================
SELECT setval('metadata.actuator_states_id_seq',    (SELECT COALESCE(MAX(id), 1) FROM metadata.actuator_states), true);
SELECT setval('metadata.alert_severities_id_seq',   (SELECT COALESCE(MAX(id), 1) FROM metadata.alert_severities), true);
SELECT setval('metadata.alert_types_id_seq',        (SELECT COALESCE(MAX(id), 1) FROM metadata.alert_types), true);
SELECT setval('metadata.data_types_id_seq',         (SELECT COALESCE(MAX(id), 1) FROM metadata.data_types), true);
SELECT setval('metadata.device_categories_id_seq',  (SELECT COALESCE(MAX(id), 1) FROM metadata.device_categories), true);
SELECT setval('metadata.device_types_id_seq',       (SELECT COALESCE(MAX(id), 1) FROM metadata.device_types), true);
SELECT setval('metadata.units_id_seq',              (SELECT COALESCE(MAX(id), 1) FROM metadata.units), true);
SELECT setval('metadata.periods_id_seq',            (SELECT COALESCE(MAX(id), 1) FROM metadata.periods), true);
