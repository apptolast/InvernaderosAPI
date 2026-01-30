-- =============================================================================
-- V25: Migrate Existing IDs to TSID for 100% Consistency
-- Fecha: 2026-01-11
--
-- Esta migracion convierte TODOS los IDs existentes (1, 2, 3...) a valores TSID
-- para lograr 100% de consistencia en el sistema.
--
-- Orden de migracion (respetando FKs):
-- 1. tenants (raiz, sin dependencias)
-- 2. users (depende de tenants)
-- 3. greenhouses (depende de tenants)
-- 4. sectors (depende de greenhouses)
-- 5. devices (depende de greenhouses, tenants)
-- 6. alerts (depende de greenhouses, tenants, users)
-- 7. settings (depende de greenhouses, tenants)
-- 8. command_history (depende de devices, users)
-- 9. mqtt_users (solo FKs, id UUID se mantiene)
--
-- IMPORTANTE:
-- - Se generan TSIDs unicos para cada registro
-- - Se actualizan TODAS las FKs en cascada
-- - Los codes se regeneran basados en secuencias
-- - mqtt_users.id (UUID) NO se toca
-- =============================================================================

DO $$ BEGIN RAISE NOTICE '=== V25 MIGRATION START: Migrate existing IDs to TSID ==='; END $$;
DO $$ BEGIN RAISE NOTICE 'Timestamp: %', NOW(); END $$;

-- =============================================================================
-- FASE 0: Crear tablas de mapeo para cada entidad
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 0: Creating mapping tables...'; END $$;

CREATE TABLE IF NOT EXISTS metadata.tsid_mapping_tenants (
    old_id BIGINT PRIMARY KEY,
    new_id BIGINT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS metadata.tsid_mapping_users (
    old_id BIGINT PRIMARY KEY,
    new_id BIGINT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS metadata.tsid_mapping_greenhouses (
    old_id BIGINT PRIMARY KEY,
    new_id BIGINT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS metadata.tsid_mapping_sectors (
    old_id BIGINT PRIMARY KEY,
    new_id BIGINT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS metadata.tsid_mapping_devices (
    old_id BIGINT PRIMARY KEY,
    new_id BIGINT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS metadata.tsid_mapping_alerts (
    old_id BIGINT PRIMARY KEY,
    new_id BIGINT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS metadata.tsid_mapping_settings (
    old_id BIGINT PRIMARY KEY,
    new_id BIGINT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS metadata.tsid_mapping_command_history (
    old_id BIGINT PRIMARY KEY,
    new_id BIGINT NOT NULL UNIQUE
);

-- =============================================================================
-- FASE 1: Generar TSIDs y poblar tablas de mapeo
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 1: Generating TSIDs for all existing records...'; END $$;

-- TENANTS
INSERT INTO metadata.tsid_mapping_tenants (old_id, new_id)
SELECT id, metadata.generate_tsid()
FROM metadata.tenants
ORDER BY id;
DO $$ BEGIN RAISE NOTICE '  - tenants: % records mapped', (SELECT COUNT(*) FROM metadata.tsid_mapping_tenants); END $$;

-- Pequeña pausa para asegurar TSIDs unicos (diferente timestamp)
SELECT pg_sleep(0.01);

-- USERS
INSERT INTO metadata.tsid_mapping_users (old_id, new_id)
SELECT id, metadata.generate_tsid()
FROM metadata.users
ORDER BY id;
DO $$ BEGIN RAISE NOTICE '  - users: % records mapped', (SELECT COUNT(*) FROM metadata.tsid_mapping_users); END $$;

SELECT pg_sleep(0.01);

-- GREENHOUSES
INSERT INTO metadata.tsid_mapping_greenhouses (old_id, new_id)
SELECT id, metadata.generate_tsid()
FROM metadata.greenhouses
ORDER BY id;
DO $$ BEGIN RAISE NOTICE '  - greenhouses: % records mapped', (SELECT COUNT(*) FROM metadata.tsid_mapping_greenhouses); END $$;

SELECT pg_sleep(0.01);

-- SECTORS
INSERT INTO metadata.tsid_mapping_sectors (old_id, new_id)
SELECT id, metadata.generate_tsid()
FROM metadata.sectors
ORDER BY id;
DO $$ BEGIN RAISE NOTICE '  - sectors: % records mapped', (SELECT COUNT(*) FROM metadata.tsid_mapping_sectors); END $$;

SELECT pg_sleep(0.01);

-- DEVICES
INSERT INTO metadata.tsid_mapping_devices (old_id, new_id)
SELECT id, metadata.generate_tsid()
FROM metadata.devices
ORDER BY id;
DO $$ BEGIN RAISE NOTICE '  - devices: % records mapped', (SELECT COUNT(*) FROM metadata.tsid_mapping_devices); END $$;

SELECT pg_sleep(0.01);

-- ALERTS
INSERT INTO metadata.tsid_mapping_alerts (old_id, new_id)
SELECT id, metadata.generate_tsid()
FROM metadata.alerts
ORDER BY id;
DO $$ BEGIN RAISE NOTICE '  - alerts: % records mapped', (SELECT COUNT(*) FROM metadata.tsid_mapping_alerts); END $$;

SELECT pg_sleep(0.01);

-- SETTINGS
INSERT INTO metadata.tsid_mapping_settings (old_id, new_id)
SELECT id, metadata.generate_tsid()
FROM metadata.settings
ORDER BY id;
DO $$ BEGIN RAISE NOTICE '  - settings: % records mapped', (SELECT COUNT(*) FROM metadata.tsid_mapping_settings); END $$;

SELECT pg_sleep(0.01);

-- COMMAND_HISTORY
INSERT INTO metadata.tsid_mapping_command_history (old_id, new_id)
SELECT id, metadata.generate_tsid()
FROM metadata.command_history
ORDER BY id;
DO $$ BEGIN RAISE NOTICE '  - command_history: % records mapped', (SELECT COUNT(*) FROM metadata.tsid_mapping_command_history); END $$;

-- =============================================================================
-- FASE 2: Deshabilitar temporalmente las FKs para permitir la migracion
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 2: Temporarily disabling foreign key constraints...'; END $$;

-- Deshabilitar triggers de FK temporalmente
SET session_replication_role = 'replica';

-- =============================================================================
-- FASE 3: Actualizar IDs y FKs en orden
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 3: Updating IDs and FKs in order...'; END $$;

-- 3.1 TENANTS (tabla raiz)
DO $$ BEGIN RAISE NOTICE '  3.1 Updating tenants...'; END $$;
UPDATE metadata.tenants t
SET id = m.new_id
FROM metadata.tsid_mapping_tenants m
WHERE t.id = m.old_id;

-- 3.2 USERS (depende de tenants)
DO $$ BEGIN RAISE NOTICE '  3.2 Updating users...'; END $$;
-- Primero actualizar FK tenant_id
UPDATE metadata.users u
SET tenant_id = m.new_id
FROM metadata.tsid_mapping_tenants m
WHERE u.tenant_id = m.old_id;
-- Luego actualizar id
UPDATE metadata.users u
SET id = m.new_id
FROM metadata.tsid_mapping_users m
WHERE u.id = m.old_id;

-- 3.3 GREENHOUSES (depende de tenants)
DO $$ BEGIN RAISE NOTICE '  3.3 Updating greenhouses...'; END $$;
-- Primero actualizar FK tenant_id
UPDATE metadata.greenhouses g
SET tenant_id = m.new_id
FROM metadata.tsid_mapping_tenants m
WHERE g.tenant_id = m.old_id;
-- Luego actualizar id
UPDATE metadata.greenhouses g
SET id = m.new_id
FROM metadata.tsid_mapping_greenhouses m
WHERE g.id = m.old_id;

-- 3.4 SECTORS (depende de greenhouses)
DO $$ BEGIN RAISE NOTICE '  3.4 Updating sectors...'; END $$;
-- Primero actualizar FK greenhouse_id
UPDATE metadata.sectors s
SET greenhouse_id = m.new_id
FROM metadata.tsid_mapping_greenhouses m
WHERE s.greenhouse_id = m.old_id;
-- Luego actualizar id
UPDATE metadata.sectors s
SET id = m.new_id
FROM metadata.tsid_mapping_sectors m
WHERE s.id = m.old_id;

-- 3.5 DEVICES (depende de greenhouses, tenants)
DO $$ BEGIN RAISE NOTICE '  3.5 Updating devices...'; END $$;
-- Actualizar FK greenhouse_id
UPDATE metadata.devices d
SET greenhouse_id = m.new_id
FROM metadata.tsid_mapping_greenhouses m
WHERE d.greenhouse_id = m.old_id;
-- Actualizar FK tenant_id
UPDATE metadata.devices d
SET tenant_id = m.new_id
FROM metadata.tsid_mapping_tenants m
WHERE d.tenant_id = m.old_id;
-- Luego actualizar id
UPDATE metadata.devices d
SET id = m.new_id
FROM metadata.tsid_mapping_devices m
WHERE d.id = m.old_id;

-- 3.6 ALERTS (depende de greenhouses, tenants, users)
DO $$ BEGIN RAISE NOTICE '  3.6 Updating alerts...'; END $$;
-- Actualizar FK greenhouse_id
UPDATE metadata.alerts a
SET greenhouse_id = m.new_id
FROM metadata.tsid_mapping_greenhouses m
WHERE a.greenhouse_id = m.old_id;
-- Actualizar FK tenant_id
UPDATE metadata.alerts a
SET tenant_id = m.new_id
FROM metadata.tsid_mapping_tenants m
WHERE a.tenant_id = m.old_id;
-- Actualizar FK resolved_by_user_id
UPDATE metadata.alerts a
SET resolved_by_user_id = m.new_id
FROM metadata.tsid_mapping_users m
WHERE a.resolved_by_user_id = m.old_id;
-- Luego actualizar id
UPDATE metadata.alerts a
SET id = m.new_id
FROM metadata.tsid_mapping_alerts m
WHERE a.id = m.old_id;

-- 3.7 SETTINGS (depende de greenhouses, tenants)
DO $$ BEGIN RAISE NOTICE '  3.7 Updating settings...'; END $$;
-- Actualizar FK greenhouse_id
UPDATE metadata.settings s
SET greenhouse_id = m.new_id
FROM metadata.tsid_mapping_greenhouses m
WHERE s.greenhouse_id = m.old_id;
-- Actualizar FK tenant_id
UPDATE metadata.settings s
SET tenant_id = m.new_id
FROM metadata.tsid_mapping_tenants m
WHERE s.tenant_id = m.old_id;
-- Luego actualizar id
UPDATE metadata.settings s
SET id = m.new_id
FROM metadata.tsid_mapping_settings m
WHERE s.id = m.old_id;

-- 3.8 COMMAND_HISTORY (depende de devices, users)
DO $$ BEGIN RAISE NOTICE '  3.8 Updating command_history...'; END $$;
-- Actualizar FK device_id
UPDATE metadata.command_history ch
SET device_id = m.new_id
FROM metadata.tsid_mapping_devices m
WHERE ch.device_id = m.old_id;
-- Actualizar FK user_id
UPDATE metadata.command_history ch
SET user_id = m.new_id
FROM metadata.tsid_mapping_users m
WHERE ch.user_id = m.old_id;
-- Luego actualizar id
UPDATE metadata.command_history ch
SET id = m.new_id
FROM metadata.tsid_mapping_command_history m
WHERE ch.id = m.old_id;

-- 3.9 MQTT_USERS (solo FKs, id UUID se mantiene)
DO $$ BEGIN RAISE NOTICE '  3.9 Updating mqtt_users FKs...'; END $$;
-- Actualizar FK greenhouse_id
UPDATE metadata.mqtt_users mu
SET greenhouse_id = m.new_id
FROM metadata.tsid_mapping_greenhouses m
WHERE mu.greenhouse_id = m.old_id;
-- Actualizar FK tenant_id
UPDATE metadata.mqtt_users mu
SET tenant_id = m.new_id
FROM metadata.tsid_mapping_tenants m
WHERE mu.tenant_id = m.old_id;

-- =============================================================================
-- FASE 4: Rehabilitar FKs
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 4: Re-enabling foreign key constraints...'; END $$;

SET session_replication_role = 'origin';

-- =============================================================================
-- FASE 5: Actualizar secuencias de codigo para reflejar nuevos maximos
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 5: Updating code sequences...'; END $$;

-- Reiniciar secuencias basadas en el numero de registros + 1
DO $$
DECLARE
    v_count BIGINT;
BEGIN
    SELECT COUNT(*) + 1 INTO v_count FROM metadata.tenants;
    EXECUTE format('ALTER SEQUENCE metadata.tenants_code_seq RESTART WITH %s', v_count);

    SELECT COUNT(*) + 1 INTO v_count FROM metadata.users;
    EXECUTE format('ALTER SEQUENCE metadata.users_code_seq RESTART WITH %s', v_count);

    SELECT COUNT(*) + 1 INTO v_count FROM metadata.greenhouses;
    EXECUTE format('ALTER SEQUENCE metadata.greenhouses_code_seq RESTART WITH %s', v_count);

    SELECT COUNT(*) + 1 INTO v_count FROM metadata.sectors;
    EXECUTE format('ALTER SEQUENCE metadata.sectors_code_seq RESTART WITH %s', v_count);

    SELECT COUNT(*) + 1 INTO v_count FROM metadata.devices;
    EXECUTE format('ALTER SEQUENCE metadata.devices_code_seq RESTART WITH %s', v_count);

    SELECT COUNT(*) + 1 INTO v_count FROM metadata.alerts;
    EXECUTE format('ALTER SEQUENCE metadata.alerts_code_seq RESTART WITH %s', v_count);

    SELECT COUNT(*) + 1 INTO v_count FROM metadata.settings;
    EXECUTE format('ALTER SEQUENCE metadata.settings_code_seq RESTART WITH %s', v_count);

    SELECT COUNT(*) + 1 INTO v_count FROM metadata.command_history;
    EXECUTE format('ALTER SEQUENCE metadata.command_history_code_seq RESTART WITH %s', v_count);

    RAISE NOTICE 'All code sequences updated';
END $$;

-- =============================================================================
-- FASE 6: Verificacion de integridad
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 6: Verifying data integrity...'; END $$;

DO $$
DECLARE
    v_errors INT := 0;
    v_count INT;
BEGIN
    -- Verificar que todos los IDs son grandes (TSID)
    SELECT COUNT(*) INTO v_count FROM metadata.tenants WHERE id < 1000000;
    IF v_count > 0 THEN
        RAISE WARNING 'Found % tenants with small IDs', v_count;
        v_errors := v_errors + v_count;
    END IF;

    SELECT COUNT(*) INTO v_count FROM metadata.users WHERE id < 1000000;
    IF v_count > 0 THEN
        RAISE WARNING 'Found % users with small IDs', v_count;
        v_errors := v_errors + v_count;
    END IF;

    SELECT COUNT(*) INTO v_count FROM metadata.greenhouses WHERE id < 1000000;
    IF v_count > 0 THEN
        RAISE WARNING 'Found % greenhouses with small IDs', v_count;
        v_errors := v_errors + v_count;
    END IF;

    SELECT COUNT(*) INTO v_count FROM metadata.sectors WHERE id < 1000000;
    IF v_count > 0 THEN
        RAISE WARNING 'Found % sectors with small IDs', v_count;
        v_errors := v_errors + v_count;
    END IF;

    SELECT COUNT(*) INTO v_count FROM metadata.devices WHERE id < 1000000;
    IF v_count > 0 THEN
        RAISE WARNING 'Found % devices with small IDs', v_count;
        v_errors := v_errors + v_count;
    END IF;

    SELECT COUNT(*) INTO v_count FROM metadata.alerts WHERE id < 1000000;
    IF v_count > 0 THEN
        RAISE WARNING 'Found % alerts with small IDs', v_count;
        v_errors := v_errors + v_count;
    END IF;

    SELECT COUNT(*) INTO v_count FROM metadata.settings WHERE id < 1000000;
    IF v_count > 0 THEN
        RAISE WARNING 'Found % settings with small IDs', v_count;
        v_errors := v_errors + v_count;
    END IF;

    IF v_errors = 0 THEN
        RAISE NOTICE 'All IDs successfully migrated to TSID format';
    ELSE
        RAISE EXCEPTION 'Migration failed: % records still have small IDs', v_errors;
    END IF;
END $$;

-- =============================================================================
-- FASE 7: Mostrar ejemplos de nuevos IDs
-- =============================================================================
DO $$
DECLARE
    v_tenant_sample RECORD;
    v_user_sample RECORD;
    v_greenhouse_sample RECORD;
BEGIN
    SELECT id, code, name INTO v_tenant_sample FROM metadata.tenants LIMIT 1;
    SELECT id, code, email INTO v_user_sample FROM metadata.users LIMIT 1;
    SELECT id, code, name INTO v_greenhouse_sample FROM metadata.greenhouses LIMIT 1;

    RAISE NOTICE '=== V25 MIGRATION COMPLETE ===';
    RAISE NOTICE 'Sample TSID values:';
    RAISE NOTICE '  Tenant: id=%, code=%, name=%', v_tenant_sample.id, v_tenant_sample.code, v_tenant_sample.name;
    RAISE NOTICE '  User: id=%, code=%, email=%', v_user_sample.id, v_user_sample.code, v_user_sample.email;
    RAISE NOTICE '  Greenhouse: id=%, code=%, name=%', v_greenhouse_sample.id, v_greenhouse_sample.code, v_greenhouse_sample.name;
    RAISE NOTICE 'All existing records now have TSID format IDs';
    RAISE NOTICE 'mqtt_users.id (UUID) unchanged - only FKs updated';
    RAISE NOTICE 'Timestamp: %', NOW();
END $$;

-- =============================================================================
-- FASE 8: Limpiar tablas de mapeo (opcional, mantener para auditoria)
-- =============================================================================
-- Mantener las tablas de mapeo por si se necesitan para rollback o referencia
-- DROP TABLE IF EXISTS metadata.tsid_mapping_tenants CASCADE;
-- DROP TABLE IF EXISTS metadata.tsid_mapping_users CASCADE;
-- ... etc

DO $$ BEGIN RAISE NOTICE 'Mapping tables preserved for audit purposes'; END $$;
DO $$ BEGIN RAISE NOTICE 'To clean up: DROP TABLE metadata.tsid_mapping_* CASCADE;'; END $$;
