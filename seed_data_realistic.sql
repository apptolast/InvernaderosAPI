-- =====================================================
-- SEED DATA - Datos Ficticios Super Realistas
-- =====================================================
-- Description: Datos de prueba para sistema multi-tenant
--              de gestión de invernaderos en España
-- Author: Claude Code
-- Date: 2025-11-16
-- =====================================================

-- Connect to PostgreSQL metadata database
\c postgres

-- Start transaction
BEGIN;

-- =====================================================
-- 1. TENANTS (Empresas Agrícolas Españolas)
-- =====================================================

-- Tenant 1: Agrícola Sara (Almería)
INSERT INTO metadata.tenants (
    id, name, email, company_name, tax_id,
    address, city, postal_code, province, country,
    phone, contact_person, contact_phone, contact_email,
    mqtt_topic_prefix, coordinates, notes, is_active,
    created_at, updated_at
) VALUES (
    '550e8400-e29b-41d4-a716-446655440001'::UUID,
    'Agrícola Sara',
    'info@agricolasara.es',
    'Agrícola Sara S.L.',
    'B04123456',
    'Carretera de Níjar, Km 12.5, Polígono La Redonda',
    'El Ejido',
    '04700',
    'Almería',
    'España',
    '+34 950 123 456',
    'Sara Martínez López',
    '+34 678 901 234',
    'sara.martinez@agricolasara.es',
    'SARA',
    '{"lat": 36.7756, "lon": -2.8149}'::JSONB,
    'Cliente desde 2020. Especializado en tomates cherry y pimientos',
    TRUE,
    NOW() - INTERVAL '3 years',
    NOW()
);

-- Tenant 2: Hortalizas Mediterráneo (Murcia)
INSERT INTO metadata.tenants (
    id, name, email, company_name, tax_id,
    address, city, postal_code, province, country,
    phone, contact_person, contact_phone, contact_email,
    mqtt_topic_prefix, coordinates, notes, is_active,
    created_at, updated_at
) VALUES (
    '550e8400-e29b-41d4-a716-446655440002'::UUID,
    'Hortalizas Mediterráneo',
    'contacto@hortamed.com',
    'Hortalizas Mediterráneo S.A.',
    'A30987654',
    'Camino Viejo de Alicante, 178',
    'Torre Pacheco',
    '30700',
    'Murcia',
    'España',
    '+34 968 456 789',
    'José García Fernández',
    '+34 689 123 456',
    'jose.garcia@hortamed.com',
    'HORTAMED',
    '{"lat": 37.7431, "lon": -0.9531}'::JSONB,
    'Cliente Premium desde 2018. Exportación a Francia y Alemania',
    TRUE,
    NOW() - INTERVAL '5 years',
    NOW()
);

-- Tenant 3: Vivero El Prado (Valencia)
INSERT INTO metadata.tenants (
    id, name, email, company_name, tax_id,
    address, city, postal_code, province, country,
    phone, contact_person, contact_phone, contact_email,
    mqtt_topic_prefix, coordinates, notes, is_active,
    created_at, updated_at
) VALUES (
    '550e8400-e29b-41d4-a716-446655440003'::UUID,
    'Vivero El Prado',
    'admin@viveroelprado.es',
    'Vivero El Prado C.B.',
    'E46234567',
    'Partida El Pla, Parcela 142',
    'Sueca',
    '46410',
    'Valencia',
    'España',
    '+34 961 789 012',
    'María Carmen Ruiz Sánchez',
    '+34 612 345 678',
    'mcarmen.ruiz@viveroelprado.es',
    'ELPRADO',
    '{"lat": 39.2053, "lon": -0.3115}'::JSONB,
    'Especializado en plantas ornamentales y cítricos. Certificado ECO',
    TRUE,
    NOW() - INTERVAL '2 years',
    NOW()
);

-- =====================================================
-- 2. GREENHOUSES (Invernaderos)
-- =====================================================

-- Invernaderos de Agrícola Sara (Almería)
INSERT INTO metadata.greenhouses (
    id, tenant_id, name, greenhouse_code, mqtt_topic,
    mqtt_publish_interval_seconds, external_id,
    location, area_m2, crop_type, timezone, is_active,
    created_at, updated_at
) VALUES
-- SARA_01: Tomates Cherry
(
    '660e8400-e29b-41d4-a716-446655440001'::UUID,
    '550e8400-e29b-41d4-a716-446655440001'::UUID,
    'Invernadero Sara 01 - Tomates Cherry',
    'SARA_01',
    'GREENHOUSE/SARA/01',
    5,
    'SARA-TOMATE-001',
    '{"address": "Nave 1, Polígono La Redonda", "coordinates": {"lat": 36.7756, "lon": -2.8149}}'::JSONB,
    5000.00,
    'Tomate Cherry (Solanum lycopersicum var. cerasiforme)',
    'Europe/Madrid',
    TRUE,
    NOW() - INTERVAL '2 years',
    NOW()
),
-- SARA_02: Pimientos
(
    '660e8400-e29b-41d4-a716-446655440002'::UUID,
    '550e8400-e29b-41d4-a716-446655440001'::UUID,
    'Invernadero Sara 02 - Pimientos California',
    'SARA_02',
    'GREENHOUSE/SARA/02',
    5,
    'SARA-PIMIENTO-002',
    '{"address": "Nave 2, Polígono La Redonda", "coordinates": {"lat": 36.7758, "lon": -2.8150}}'::JSONB,
    4500.00,
    'Pimiento California Rojo (Capsicum annuum)',
    'Europe/Madrid',
    TRUE,
    NOW() - INTERVAL '1 year 6 months',
    NOW()
),
-- SARA_03: Pepinos
(
    '660e8400-e29b-41d4-a716-446655440003'::UUID,
    '550e8400-e29b-41d4-a716-446655440001'::UUID,
    'Invernadero Sara 03 - Pepinos',
    'SARA_03',
    'GREENHOUSE/SARA/03',
    5,
    'SARA-PEPINO-003',
    '{"address": "Nave 3, Polígono La Redonda", "coordinates": {"lat": 36.7760, "lon": -2.8148}}'::JSONB,
    3800.00,
    'Pepino Holandés (Cucumis sativus)',
    'Europe/Madrid',
    TRUE,
    NOW() - INTERVAL '1 year',
    NOW()
);

-- Invernaderos de Hortalizas Mediterráneo (Murcia)
INSERT INTO metadata.greenhouses (
    id, tenant_id, name, greenhouse_code, mqtt_topic,
    mqtt_publish_interval_seconds, external_id,
    location, area_m2, crop_type, timezone, is_active,
    created_at, updated_at
) VALUES
-- HORTAMED_A1: Lechugas
(
    '660e8400-e29b-41d4-a716-446655440004'::UUID,
    '550e8400-e29b-41d4-a716-446655440002'::UUID,
    'Invernadero Hortamed A1 - Lechugas Iceberg',
    'HORTAMED_A1',
    'GREENHOUSE/HORTAMED/A1',
    3,
    'HM-LECHUGA-A1',
    '{"address": "Sector A, Parcela 1", "coordinates": {"lat": 37.7431, "lon": -0.9531}}'::JSONB,
    6200.00,
    'Lechuga Iceberg (Lactuca sativa)',
    'Europe/Madrid',
    TRUE,
    NOW() - INTERVAL '4 years',
    NOW()
),
-- HORTAMED_A2: Brócoli
(
    '660e8400-e29b-41d4-a716-446655440005'::UUID,
    '550e8400-e29b-41d4-a716-446655440002'::UUID,
    'Invernadero Hortamed A2 - Brócoli',
    'HORTAMED_A2',
    'GREENHOUSE/HORTAMED/A2',
    3,
    'HM-BROCOLI-A2',
    '{"address": "Sector A, Parcela 2", "coordinates": {"lat": 37.7433, "lon": -0.9529}}'::JSONB,
    5800.00,
    'Brócoli (Brassica oleracea var. italica)',
    'Europe/Madrid',
    TRUE,
    NOW() - INTERVAL '3 years',
    NOW()
);

-- Invernaderos de Vivero El Prado (Valencia)
INSERT INTO metadata.greenhouses (
    id, tenant_id, name, greenhouse_code, mqtt_topic,
    mqtt_publish_interval_seconds, external_id,
    location, area_m2, crop_type, timezone, is_active,
    created_at, updated_at
) VALUES
-- ELPRADO_V1: Plantas Ornamentales
(
    '660e8400-e29b-41d4-a716-446655440006'::UUID,
    '550e8400-e29b-41d4-a716-446655440003'::UUID,
    'Vivero El Prado V1 - Plantas Ornamentales',
    'ELPRADO_V1',
    'GREENHOUSE/ELPRADO/V1',
    10,
    'EP-ORNAMENTAL-V1',
    '{"address": "Zona Viveros, Módulo 1", "coordinates": {"lat": 39.2053, "lon": -0.3115}}'::JSONB,
    2500.00,
    'Plantas Ornamentales Mixtas',
    'Europe/Madrid',
    TRUE,
    NOW() - INTERVAL '1 year 8 months',
    NOW()
),
-- ELPRADO_C1: Cítricos
(
    '660e8400-e29b-41d4-a716-446655440007'::UUID,
    '550e8400-e29b-41d4-a716-446655440003'::UUID,
    'Vivero El Prado C1 - Naranjos',
    'ELPRADO_C1',
    'GREENHOUSE/ELPRADO/C1',
    10,
    'EP-CITRICOS-C1',
    '{"address": "Zona Cítricos, Módulo 1", "coordinates": {"lat": 39.2055, "lon": -0.3117}}'::JSONB,
    3200.00,
    'Naranjos (Citrus × sinensis) - Variedad Navelina',
    'Europe/Madrid',
    TRUE,
    NOW() - INTERVAL '10 months',
    NOW()
);

-- =====================================================
-- 3. USERS (Usuarios del Sistema)
-- =====================================================

INSERT INTO metadata.users (
    id, tenant_id, username, email, password_hash, role,
    is_active, last_login, created_at, updated_at
) VALUES
-- Usuarios Agrícola Sara
(
    '770e8400-e29b-41d4-a716-446655440001'::UUID,
    '550e8400-e29b-41d4-a716-446655440001'::UUID,
    'sara.martinez',
    'sara.martinez@agricolasara.es',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIvApYrZ0C',  -- password: Sara2024!
    'ADMIN',
    TRUE,
    NOW() - INTERVAL '2 hours',
    NOW() - INTERVAL '3 years',
    NOW()
),
(
    '770e8400-e29b-41d4-a716-446655440002'::UUID,
    '550e8400-e29b-41d4-a716-446655440001'::UUID,
    'antonio.lopez',
    'antonio.lopez@agricolasara.es',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIvApYrZ0C',
    'OPERATOR',
    TRUE,
    NOW() - INTERVAL '5 hours',
    NOW() - INTERVAL '2 years',
    NOW()
),
-- Usuarios Hortalizas Mediterráneo
(
    '770e8400-e29b-41d4-a716-446655440003'::UUID,
    '550e8400-e29b-41d4-a716-446655440002'::UUID,
    'jose.garcia',
    'jose.garcia@hortamed.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIvApYrZ0C',
    'ADMIN',
    TRUE,
    NOW() - INTERVAL '1 hour',
    NOW() - INTERVAL '5 years',
    NOW()
),
(
    '770e8400-e29b-41d4-a716-446655440004'::UUID,
    '550e8400-e29b-41d4-a716-446655440002'::UUID,
    'maria.torres',
    'maria.torres@hortamed.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIvApYrZ0C',
    'OPERATOR',
    TRUE,
    NOW() - INTERVAL '3 hours',
    NOW() - INTERVAL '3 years',
    NOW()
),
-- Usuarios Vivero El Prado
(
    '770e8400-e29b-41d4-a716-446655440005'::UUID,
    '550e8400-e29b-41d4-a716-446655440003'::UUID,
    'mcarmen.ruiz',
    'mcarmen.ruiz@viveroelprado.es',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIvApYrZ0C',
    'ADMIN',
    TRUE,
    NOW() - INTERVAL '30 minutes',
    NOW() - INTERVAL '2 years',
    NOW()
);

-- =====================================================
-- 4. SENSORS (Sensores por Invernadero)
-- =====================================================

-- Sensores para SARA_01 (Tomates Cherry)
INSERT INTO metadata.sensors (
    id, greenhouse_id, tenant_id, sensor_code, device_id,
    sensor_type, mqtt_field_name, data_format, unit,
    min_threshold, max_threshold, location_in_greenhouse,
    is_active, last_seen, created_at, updated_at
) VALUES
(
    '880e8400-e29b-41d4-a716-446655440001'::UUID,
    '660e8400-e29b-41d4-a716-446655440001'::UUID,
    '550e8400-e29b-41d4-a716-446655440001'::UUID,
    'TEMP_01',
    'DHT22-001',
    'TEMPERATURE',
    'SARA_TEMP01_valor',
    'NUMERIC',
    '°C',
    15.00,
    35.00,
    'Zona Central, Altura 1.5m',
    TRUE,
    NOW() - INTERVAL '5 minutes',
    NOW() - INTERVAL '2 years',
    NOW()
),
(
    '880e8400-e29b-41d4-a716-446655440002'::UUID,
    '660e8400-e29b-41d4-a716-446655440001'::UUID,
    '550e8400-e29b-41d4-a716-446655440001'::UUID,
    'HUM_01',
    'DHT22-001',
    'HUMIDITY',
    'SARA_HUM01_valor',
    'NUMERIC',
    '%',
    40.00,
    85.00,
    'Zona Central, Altura 1.5m',
    TRUE,
    NOW() - INTERVAL '5 minutes',
    NOW() - INTERVAL '2 years',
    NOW()
),
(
    '880e8400-e29b-41d4-a716-446655440003'::UUID,
    '660e8400-e29b-41d4-a716-446655440001'::UUID,
    '550e8400-e29b-41d4-a716-446655440001'::UUID,
    'SOIL_MOIST_01',
    'CAPACITIVE-001',
    'SOIL_MOISTURE',
    'SARA_SOIL01_valor',
    'NUMERIC',
    '%',
    30.00,
    70.00,
    'Zona Riego 1',
    TRUE,
    NOW() - INTERVAL '5 minutes',
    NOW() - INTERVAL '1 year 6 months',
    NOW()
),
(
    '880e8400-e29b-41d4-a716-446655440004'::UUID,
    '660e8400-e29b-41d4-a716-446655440001'::UUID,
    '550e8400-e29b-41d4-a716-446655440001'::UUID,
    'LIGHT_01',
    'BH1750-001',
    'LIGHT_INTENSITY',
    'SARA_LIGHT01_valor',
    'NUMERIC',
    'lux',
    10000.00,
    50000.00,
    'Techo Central',
    TRUE,
    NOW() - INTERVAL '5 minutes',
    NOW() - INTERVAL '1 year',
    NOW()
);

-- Sensores para HORTAMED_A1 (Lechugas)
INSERT INTO metadata.sensors (
    id, greenhouse_id, tenant_id, sensor_code, device_id,
    sensor_type, mqtt_field_name, data_format, unit,
    min_threshold, max_threshold, location_in_greenhouse,
    is_active, last_seen, created_at, updated_at
) VALUES
(
    '880e8400-e29b-41d4-a716-446655440005'::UUID,
    '660e8400-e29b-41d4-a716-446655440004'::UUID,
    '550e8400-e29b-41d4-a716-446655440002'::UUID,
    'TEMP_A1_01',
    'DHT22-HM001',
    'TEMPERATURE',
    'HORTAMED_TEMP_A1_01',
    'NUMERIC',
    '°C',
    10.00,
    28.00,
    'Zona Norte',
    TRUE,
    NOW() - INTERVAL '3 minutes',
    NOW() - INTERVAL '4 years',
    NOW()
),
(
    '880e8400-e29b-41d4-a716-446655440006'::UUID,
    '660e8400-e29b-41d4-a716-446655440004'::UUID,
    '550e8400-e29b-41d4-a716-446655440002'::UUID,
    'HUM_A1_01',
    'DHT22-HM001',
    'HUMIDITY',
    'HORTAMED_HUM_A1_01',
    'NUMERIC',
    '%',
    50.00,
    90.00,
    'Zona Norte',
    TRUE,
    NOW() - INTERVAL '3 minutes',
    NOW() - INTERVAL '4 years',
    NOW()
);

-- =====================================================
-- 5. ACTUATORS (Actuadores por Invernadero)
-- =====================================================

-- Actuadores para SARA_01
INSERT INTO metadata.actuators (
    id, tenant_id, greenhouse_id, actuator_code, device_id,
    actuator_type, current_state, current_value, unit,
    mqtt_command_topic, mqtt_status_topic, location_in_greenhouse,
    is_active, last_command_at, last_status_update, created_at, updated_at
) VALUES
(
    '990e8400-e29b-41d4-a716-446655440001'::UUID,
    '550e8400-e29b-41d4-a716-446655440001'::UUID,
    '660e8400-e29b-41d4-a716-446655440001'::UUID,
    'FAN_01',
    'VENTILADOR-001',
    'VENTILADOR',
    'ON',
    75.00,
    '%',
    'GREENHOUSE/SARA/01/actuator/FAN_01/command',
    'GREENHOUSE/SARA/01/actuator/FAN_01/status',
    'Pared Norte, Altura 3m',
    TRUE,
    NOW() - INTERVAL '10 minutes',
    NOW() - INTERVAL '5 minutes',
    NOW() - INTERVAL '2 years',
    NOW()
),
(
    '990e8400-e29b-41d4-a716-446655440002'::UUID,
    '550e8400-e29b-41d4-a716-446655440001'::UUID,
    '660e8400-e29b-41d4-a716-446655440001'::UUID,
    'RIEGO_01',
    'VALVULA-RIEGO-001',
    'RIEGO',
    'AUTO',
    12.50,
    'L/min',
    'GREENHOUSE/SARA/01/actuator/RIEGO_01/command',
    'GREENHOUSE/SARA/01/actuator/RIEGO_01/status',
    'Sistema Riego Zona 1',
    TRUE,
    NOW() - INTERVAL '1 hour',
    NOW() - INTERVAL '30 minutes',
    NOW() - INTERVAL '1 year 8 months',
    NOW()
),
(
    '990e8400-e29b-41d4-a716-446655440003'::UUID,
    '550e8400-e29b-41d4-a716-446655440001'::UUID,
    '660e8400-e29b-41d4-a716-446655440001'::UUID,
    'CORTINA_01',
    'MOTOR-CORTINA-001',
    'CORTINA',
    'MANUAL',
    45.00,
    '%',
    'GREENHOUSE/SARA/01/actuator/CORTINA_01/command',
    'GREENHOUSE/SARA/01/actuator/CORTINA_01/status',
    'Techo Móvil',
    TRUE,
    NOW() - INTERVAL '2 hours',
    NOW() - INTERVAL '1 hour 30 minutes',
    NOW() - INTERVAL '1 year',
    NOW()
);

-- =====================================================
-- 6. ALERTS (Alertas del Sistema)
-- =====================================================

INSERT INTO metadata.alerts (
    id, greenhouse_id, sensor_id, tenant_id,
    alert_type, severity, message, alert_data,
    is_resolved, resolved_at, resolved_by,
    created_at, updated_at
) VALUES
-- Alerta CRÍTICA: Temperatura alta en SARA_01
(
    'aa0e8400-e29b-41d4-a716-446655440001'::UUID,
    '660e8400-e29b-41d4-a716-446655440001'::UUID,
    '880e8400-e29b-41d4-a716-446655440001'::UUID,
    '550e8400-e29b-41d4-a716-446655440001'::UUID,
    'THRESHOLD_EXCEEDED',
    'CRITICAL',
    'Temperatura supera umbral máximo en Invernadero Sara 01',
    '{"threshold": 35, "current_value": 38.5, "sensor_type": "TEMPERATURE", "duration_minutes": 15}'::JSONB,
    FALSE,
    NULL,
    NULL,
    NOW() - INTERVAL '15 minutes',
    NOW() - INTERVAL '15 minutes'
),
-- Alerta WARNING: Humedad baja en SARA_01 (RESUELTA)
(
    'aa0e8400-e29b-41d4-a716-446655440002'::UUID,
    '660e8400-e29b-41d4-a716-446655440001'::UUID,
    '880e8400-e29b-41d4-a716-446655440002'::UUID,
    '550e8400-e29b-41d4-a716-446655440001'::UUID,
    'THRESHOLD_EXCEEDED',
    'WARNING',
    'Humedad por debajo del umbral mínimo',
    '{"threshold": 40, "current_value": 35.2, "sensor_type": "HUMIDITY"}'::JSONB,
    TRUE,
    NOW() - INTERVAL '2 hours',
    '770e8400-e29b-41d4-a716-446655440002'::UUID,
    NOW() - INTERVAL '3 hours',
    NOW() - INTERVAL '2 hours'
),
-- Alerta ERROR: Sensor offline en HORTAMED_A1
(
    'aa0e8400-e29b-41d4-a716-446655440003'::UUID,
    '660e8400-e29b-41d4-a716-446655440004'::UUID,
    '880e8400-e29b-41d4-a716-446655440005'::UUID,
    '550e8400-e29b-41d4-a716-446655440002'::UUID,
    'SENSOR_OFFLINE',
    'ERROR',
    'Sensor de temperatura TEMP_A1_01 sin respuesta',
    '{"sensor_code": "TEMP_A1_01", "last_seen": "2025-11-16T10:30:00Z", "offline_duration_minutes": 120}'::JSONB,
    FALSE,
    NULL,
    NULL,
    NOW() - INTERVAL '2 hours',
    NOW() - INTERVAL '2 hours'
);

-- =====================================================
-- 7. SAMPLE DATA - TimescaleDB sensor_readings
-- =====================================================

-- Connect to TimescaleDB
\c postgres -h 138.199.157.58 -p 30432

-- Insert sample sensor readings for last 24 hours
-- (Simplified - en producción se generarían con script Python/script)

INSERT INTO public.sensor_readings (time, sensor_id, greenhouse_id, tenant_id, sensor_type, value, unit)
VALUES
-- Lecturas de SARA_01 (últimas 2 horas)
(NOW() - INTERVAL '5 minutes', 'SARA_TEMP01_valor', '660e8400-e29b-41d4-a716-446655440001'::UUID, '550e8400-e29b-41d4-a716-446655440001'::UUID, 'TEMPERATURE', 24.5, '°C'),
(NOW() - INTERVAL '10 minutes', 'SARA_TEMP01_valor', '660e8400-e29b-41d4-a716-446655440001'::UUID, '550e8400-e29b-41d4-a716-446655440001'::UUID, 'TEMPERATURE', 24.3, '°C'),
(NOW() - INTERVAL '15 minutes', 'SARA_TEMP01_valor', '660e8400-e29b-41d4-a716-446655440001'::UUID, '550e8400-e29b-41d4-a716-446655440001'::UUID, 'TEMPERATURE', 24.7, '°C'),
(NOW() - INTERVAL '5 minutes', 'SARA_HUM01_valor', '660e8400-e29b-41d4-a716-446655440001'::UUID, '550e8400-e29b-41d4-a716-446655440001'::UUID, 'HUMIDITY', 62.3, '%'),
(NOW() - INTERVAL '10 minutes', 'SARA_HUM01_valor', '660e8400-e29b-41d4-a716-446655440001'::UUID, '550e8400-e29b-41d4-a716-446655440001'::UUID, 'HUMIDITY', 61.8, '%'),
(NOW() - INTERVAL '15 minutes', 'SARA_HUM01_valor', '660e8400-e29b-41d4-a716-446655440001'::UUID, '550e8400-e29b-41d4-a716-446655440001'::UUID, 'HUMIDITY', 63.1, '%');

COMMIT;

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

-- Count records by table
SELECT 'tenants' as table_name, COUNT(*) FROM metadata.tenants
UNION ALL
SELECT 'greenhouses', COUNT(*) FROM metadata.greenhouses
UNION ALL
SELECT 'users', COUNT(*) FROM metadata.users
UNION ALL
SELECT 'sensors', COUNT(*) FROM metadata.sensors
UNION ALL
SELECT 'actuators', COUNT(*) FROM metadata.actuators
UNION ALL
SELECT 'alerts', COUNT(*) FROM metadata.alerts;

-- Show tenant hierarchy
SELECT
    t.name as tenant,
    t.mqtt_topic_prefix,
    COUNT(DISTINCT g.id) as num_greenhouses,
    COUNT(DISTINCT s.id) as num_sensors,
    COUNT(DISTINCT a.id) as num_actuators
FROM metadata.tenants t
LEFT JOIN metadata.greenhouses g ON g.tenant_id = t.id
LEFT JOIN metadata.sensors s ON s.tenant_id = t.id
LEFT JOIN metadata.actuators a ON a.tenant_id = t.id
GROUP BY t.id, t.name, t.mqtt_topic_prefix
ORDER BY t.name;
