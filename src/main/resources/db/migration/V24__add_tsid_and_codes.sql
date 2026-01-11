-- =============================================================================
-- V24: Add TSID Generator and Unique Codes to All Main Tables
-- Fecha: 2026-01-11
--
-- Esta migracion:
-- 1. Crea funcion generate_tsid() para generar IDs unicos globales
-- 2. Anade campo _code unico a cada tabla principal
-- 3. Genera codigos para registros existentes
-- 4. Cambia el DEFAULT de columnas id para usar TSID en nuevos registros
-- 5. Modifica MqttUsers: greenhouse_id y tenant_id de UUID a BIGINT
--
-- IMPORTANTE: Los IDs existentes NO se modifican. Solo los nuevos registros
-- usaran TSID. Los codigos permiten identificacion externa (PLCs, APIs, etc.)
-- =============================================================================

-- Registrar inicio de migracion
DO $$ BEGIN RAISE NOTICE '=== V24 MIGRATION START: TSID + Codes ==='; END $$;
DO $$ BEGIN RAISE NOTICE 'Timestamp: %', NOW(); END $$;

-- =============================================================================
-- FASE 1: Crear funcion generadora de TSID
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 1: Creating TSID generator function...'; END $$;

-- Epoch personalizado: 1 de Enero 2024 00:00:00 UTC
-- Esto da ~139 anos de rango desde 2024
CREATE OR REPLACE FUNCTION metadata.tsid_epoch()
RETURNS BIGINT AS $func$
BEGIN
    RETURN 1704067200000;  -- 2024-01-01 00:00:00 UTC en milisegundos
END;
$func$ LANGUAGE plpgsql IMMUTABLE;

-- Secuencia para el counter (0-4095 por milisegundo)
CREATE SEQUENCE IF NOT EXISTS metadata.tsid_counter_seq
    AS INTEGER
    MINVALUE 0
    MAXVALUE 4095
    CYCLE;

-- Funcion principal para generar TSID
-- Parametro: node_id (0-1023), default 1
-- Estructura: [42 bits timestamp][10 bits node][12 bits counter]
CREATE OR REPLACE FUNCTION metadata.generate_tsid(p_node_id INTEGER DEFAULT 1)
RETURNS BIGINT AS $func$
DECLARE
    v_timestamp BIGINT;
    v_counter INTEGER;
    v_tsid BIGINT;
BEGIN
    -- Validar node_id (10 bits = 0-1023)
    IF p_node_id < 0 OR p_node_id > 1023 THEN
        RAISE EXCEPTION 'node_id debe estar entre 0 y 1023, recibido: %', p_node_id;
    END IF;

    -- Timestamp actual en milisegundos desde epoch custom
    v_timestamp := (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT - metadata.tsid_epoch();

    -- Siguiente valor del counter (12 bits = 0-4095)
    v_counter := nextval('metadata.tsid_counter_seq');

    -- Construir TSID: [42 bits timestamp][10 bits node][12 bits counter]
    v_tsid := (v_timestamp << 22) | (p_node_id << 12) | v_counter;

    RETURN v_tsid;
END;
$func$ LANGUAGE plpgsql;

COMMENT ON FUNCTION metadata.generate_tsid(INTEGER) IS
'Genera un TSID (Time-Sorted ID) unico de 64 bits.
Estructura: [42 bits timestamp][10 bits node_id][12 bits counter]
Parametro node_id: identificador del nodo (0-1023), default 1
Capacidad: 4096 IDs por milisegundo por nodo';

-- =============================================================================
-- FASE 2: Anadir columnas _code a todas las tablas principales
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 2: Adding _code columns to main tables...'; END $$;

-- TENANTS
ALTER TABLE metadata.tenants
    ADD COLUMN IF NOT EXISTS code VARCHAR(50);

-- USERS
ALTER TABLE metadata.users
    ADD COLUMN IF NOT EXISTS code VARCHAR(50);

-- GREENHOUSES
ALTER TABLE metadata.greenhouses
    ADD COLUMN IF NOT EXISTS code VARCHAR(50);

-- SECTORS
ALTER TABLE metadata.sectors
    ADD COLUMN IF NOT EXISTS code VARCHAR(50);

-- DEVICES
ALTER TABLE metadata.devices
    ADD COLUMN IF NOT EXISTS code VARCHAR(50);

-- ALERTS
ALTER TABLE metadata.alerts
    ADD COLUMN IF NOT EXISTS code VARCHAR(50);

-- SETTINGS
ALTER TABLE metadata.settings
    ADD COLUMN IF NOT EXISTS code VARCHAR(50);

-- COMMAND_HISTORY
ALTER TABLE metadata.command_history
    ADD COLUMN IF NOT EXISTS code VARCHAR(50);

-- =============================================================================
-- FASE 3: Generar codigos para registros existentes
-- Formato: {PREFIX}-{ID_PADDED}
-- Ejemplo: TNT-00001, USR-00001, GRH-00001, DEV-00001, etc.
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 3: Generating codes for existing records...'; END $$;

-- TENANTS: TNT-{id}
UPDATE metadata.tenants
SET code = 'TNT-' || LPAD(id::TEXT, 5, '0')
WHERE code IS NULL;

-- USERS: USR-{id}
UPDATE metadata.users
SET code = 'USR-' || LPAD(id::TEXT, 5, '0')
WHERE code IS NULL;

-- GREENHOUSES: GRH-{id}
UPDATE metadata.greenhouses
SET code = 'GRH-' || LPAD(id::TEXT, 5, '0')
WHERE code IS NULL;

-- SECTORS: SEC-{id}
UPDATE metadata.sectors
SET code = 'SEC-' || LPAD(id::TEXT, 5, '0')
WHERE code IS NULL;

-- DEVICES: DEV-{id}
UPDATE metadata.devices
SET code = 'DEV-' || LPAD(id::TEXT, 5, '0')
WHERE code IS NULL;

-- ALERTS: ALT-{id}
UPDATE metadata.alerts
SET code = 'ALT-' || LPAD(id::TEXT, 5, '0')
WHERE code IS NULL;

-- SETTINGS: SET-{id}
UPDATE metadata.settings
SET code = 'SET-' || LPAD(id::TEXT, 5, '0')
WHERE code IS NULL;

-- COMMAND_HISTORY: CMD-{id}
UPDATE metadata.command_history
SET code = 'CMD-' || LPAD(id::TEXT, 5, '0')
WHERE code IS NULL;

-- =============================================================================
-- FASE 4: Hacer columnas _code NOT NULL y UNIQUE
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 4: Adding NOT NULL and UNIQUE constraints to code columns...'; END $$;

-- TENANTS
ALTER TABLE metadata.tenants
    ALTER COLUMN code SET NOT NULL;
ALTER TABLE metadata.tenants
    ADD CONSTRAINT uq_tenants_code UNIQUE (code);

-- USERS
ALTER TABLE metadata.users
    ALTER COLUMN code SET NOT NULL;
ALTER TABLE metadata.users
    ADD CONSTRAINT uq_users_code UNIQUE (code);

-- GREENHOUSES
ALTER TABLE metadata.greenhouses
    ALTER COLUMN code SET NOT NULL;
ALTER TABLE metadata.greenhouses
    ADD CONSTRAINT uq_greenhouses_code UNIQUE (code);

-- SECTORS
ALTER TABLE metadata.sectors
    ALTER COLUMN code SET NOT NULL;
ALTER TABLE metadata.sectors
    ADD CONSTRAINT uq_sectors_code UNIQUE (code);

-- DEVICES
ALTER TABLE metadata.devices
    ALTER COLUMN code SET NOT NULL;
ALTER TABLE metadata.devices
    ADD CONSTRAINT uq_devices_code UNIQUE (code);

-- ALERTS
ALTER TABLE metadata.alerts
    ALTER COLUMN code SET NOT NULL;
ALTER TABLE metadata.alerts
    ADD CONSTRAINT uq_alerts_code UNIQUE (code);

-- SETTINGS
ALTER TABLE metadata.settings
    ALTER COLUMN code SET NOT NULL;
ALTER TABLE metadata.settings
    ADD CONSTRAINT uq_settings_code UNIQUE (code);

-- COMMAND_HISTORY
ALTER TABLE metadata.command_history
    ALTER COLUMN code SET NOT NULL;
ALTER TABLE metadata.command_history
    ADD CONSTRAINT uq_command_history_code UNIQUE (code);

-- =============================================================================
-- FASE 5: Crear indices para columnas _code
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 5: Creating indexes for code columns...'; END $$;

CREATE INDEX IF NOT EXISTS idx_tenants_code ON metadata.tenants(code);
CREATE INDEX IF NOT EXISTS idx_users_code ON metadata.users(code);
CREATE INDEX IF NOT EXISTS idx_greenhouses_code ON metadata.greenhouses(code);
CREATE INDEX IF NOT EXISTS idx_sectors_code ON metadata.sectors(code);
CREATE INDEX IF NOT EXISTS idx_devices_code ON metadata.devices(code);
CREATE INDEX IF NOT EXISTS idx_alerts_code ON metadata.alerts(code);
CREATE INDEX IF NOT EXISTS idx_settings_code ON metadata.settings(code);
CREATE INDEX IF NOT EXISTS idx_command_history_code ON metadata.command_history(code);

-- =============================================================================
-- FASE 6: Cambiar DEFAULT de columnas id para usar TSID
-- Los IDs existentes NO cambian. Solo los nuevos registros usaran TSID.
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 6: Changing id column defaults to use TSID...'; END $$;

-- Primero, actualizar las secuencias existentes al valor maximo + rango de seguridad
-- Esto evita colisiones entre IDs antiguos y nuevos TSIDs
-- (TSID genera numeros en el rango de 10^18, muy lejos de los IDs pequenos)

ALTER TABLE metadata.tenants
    ALTER COLUMN id SET DEFAULT metadata.generate_tsid();

ALTER TABLE metadata.users
    ALTER COLUMN id SET DEFAULT metadata.generate_tsid();

ALTER TABLE metadata.greenhouses
    ALTER COLUMN id SET DEFAULT metadata.generate_tsid();

ALTER TABLE metadata.sectors
    ALTER COLUMN id SET DEFAULT metadata.generate_tsid();

ALTER TABLE metadata.devices
    ALTER COLUMN id SET DEFAULT metadata.generate_tsid();

ALTER TABLE metadata.alerts
    ALTER COLUMN id SET DEFAULT metadata.generate_tsid();

ALTER TABLE metadata.settings
    ALTER COLUMN id SET DEFAULT metadata.generate_tsid();

ALTER TABLE metadata.command_history
    ALTER COLUMN id SET DEFAULT metadata.generate_tsid();

-- =============================================================================
-- FASE 7: Modificar MqttUsers - cambiar greenhouse_id y tenant_id de UUID a BIGINT
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 7: Modifying MqttUsers FK columns from UUID to BIGINT...'; END $$;

-- 7.1 Eliminar las constraints FK existentes (si existen)
ALTER TABLE metadata.mqtt_users
    DROP CONSTRAINT IF EXISTS fk_mqtt_users_greenhouse;
ALTER TABLE metadata.mqtt_users
    DROP CONSTRAINT IF EXISTS fk_mqtt_users_tenant;
ALTER TABLE metadata.mqtt_users
    DROP CONSTRAINT IF EXISTS mqtt_users_greenhouse_id_fkey;
ALTER TABLE metadata.mqtt_users
    DROP CONSTRAINT IF EXISTS mqtt_users_tenant_id_fkey;

-- 7.2 Eliminar indices existentes en las columnas FK
DROP INDEX IF EXISTS metadata.idx_mqtt_users_greenhouse;
DROP INDEX IF EXISTS metadata.idx_mqtt_users_tenant;
DROP INDEX IF EXISTS metadata.idx_mqtt_users_tenant_active;

-- 7.3 Crear columnas temporales BIGINT
ALTER TABLE metadata.mqtt_users
    ADD COLUMN IF NOT EXISTS greenhouse_id_new BIGINT;
ALTER TABLE metadata.mqtt_users
    ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;

-- 7.4 Migrar datos de UUID a BIGINT usando las tablas de mapeo (si existen)
-- Si no hay mapeo, los valores quedaran NULL

-- Intentar migrar greenhouse_id
DO $$
BEGIN
    -- Verificar si existe la tabla de mapeo
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = 'metadata'
               AND table_name = 'uuid_mapping_greenhouses') THEN
        UPDATE metadata.mqtt_users mu
        SET greenhouse_id_new = m.new_id
        FROM metadata.uuid_mapping_greenhouses m
        WHERE mu.greenhouse_id = m.old_uuid;

        RAISE NOTICE 'Migrated greenhouse_id using uuid_mapping_greenhouses';
    ELSE
        RAISE NOTICE 'No uuid_mapping_greenhouses table found - greenhouse_id_new will be NULL';
    END IF;
END $$;

-- Intentar migrar tenant_id
DO $$
BEGIN
    -- Verificar si existe la tabla de mapeo
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = 'metadata'
               AND table_name = 'uuid_mapping_tenants') THEN
        UPDATE metadata.mqtt_users mu
        SET tenant_id_new = m.new_id
        FROM metadata.uuid_mapping_tenants m
        WHERE mu.tenant_id = m.old_uuid;

        RAISE NOTICE 'Migrated tenant_id using uuid_mapping_tenants';
    ELSE
        RAISE NOTICE 'No uuid_mapping_tenants table found - tenant_id_new will be NULL';
    END IF;
END $$;

-- 7.5 Eliminar columnas UUID antiguas
ALTER TABLE metadata.mqtt_users DROP COLUMN IF EXISTS greenhouse_id;
ALTER TABLE metadata.mqtt_users DROP COLUMN IF EXISTS tenant_id;

-- 7.6 Renombrar columnas nuevas
ALTER TABLE metadata.mqtt_users RENAME COLUMN greenhouse_id_new TO greenhouse_id;
ALTER TABLE metadata.mqtt_users RENAME COLUMN tenant_id_new TO tenant_id;

-- 7.7 Recrear indices
CREATE INDEX IF NOT EXISTS idx_mqtt_users_greenhouse
    ON metadata.mqtt_users(greenhouse_id);
CREATE INDEX IF NOT EXISTS idx_mqtt_users_tenant
    ON metadata.mqtt_users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_mqtt_users_tenant_active
    ON metadata.mqtt_users(tenant_id, is_active) WHERE is_active = true;

-- 7.8 Recrear FKs (opcional - pueden quedar sin FK si los datos no coinciden)
-- Solo crear FK si hay datos validos
DO $$
DECLARE
    v_invalid_greenhouses INT;
    v_invalid_tenants INT;
BEGIN
    -- Contar registros con greenhouse_id invalido
    SELECT COUNT(*) INTO v_invalid_greenhouses
    FROM metadata.mqtt_users mu
    WHERE mu.greenhouse_id IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM metadata.greenhouses g WHERE g.id = mu.greenhouse_id);

    -- Contar registros con tenant_id invalido
    SELECT COUNT(*) INTO v_invalid_tenants
    FROM metadata.mqtt_users mu
    WHERE mu.tenant_id IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM metadata.tenants t WHERE t.id = mu.tenant_id);

    IF v_invalid_greenhouses = 0 THEN
        ALTER TABLE metadata.mqtt_users
            ADD CONSTRAINT fk_mqtt_users_greenhouse
            FOREIGN KEY (greenhouse_id) REFERENCES metadata.greenhouses(id) ON DELETE SET NULL;
        RAISE NOTICE 'Created FK fk_mqtt_users_greenhouse';
    ELSE
        RAISE NOTICE 'Skipped FK fk_mqtt_users_greenhouse - % invalid references found', v_invalid_greenhouses;
    END IF;

    IF v_invalid_tenants = 0 THEN
        ALTER TABLE metadata.mqtt_users
            ADD CONSTRAINT fk_mqtt_users_tenant
            FOREIGN KEY (tenant_id) REFERENCES metadata.tenants(id) ON DELETE SET NULL;
        RAISE NOTICE 'Created FK fk_mqtt_users_tenant';
    ELSE
        RAISE NOTICE 'Skipped FK fk_mqtt_users_tenant - % invalid references found', v_invalid_tenants;
    END IF;
END $$;

-- =============================================================================
-- FASE 8: Verificacion final
-- =============================================================================
DO $$
DECLARE
    v_tenants_count INT;
    v_users_count INT;
    v_greenhouses_count INT;
    v_sectors_count INT;
    v_devices_count INT;
    v_alerts_count INT;
    v_settings_count INT;
    v_command_history_count INT;
    v_mqtt_users_count INT;
BEGIN
    SELECT COUNT(*) INTO v_tenants_count FROM metadata.tenants;
    SELECT COUNT(*) INTO v_users_count FROM metadata.users;
    SELECT COUNT(*) INTO v_greenhouses_count FROM metadata.greenhouses;
    SELECT COUNT(*) INTO v_sectors_count FROM metadata.sectors;
    SELECT COUNT(*) INTO v_devices_count FROM metadata.devices;
    SELECT COUNT(*) INTO v_alerts_count FROM metadata.alerts;
    SELECT COUNT(*) INTO v_settings_count FROM metadata.settings;
    SELECT COUNT(*) INTO v_command_history_count FROM metadata.command_history;
    SELECT COUNT(*) INTO v_mqtt_users_count FROM metadata.mqtt_users;

    RAISE NOTICE '=== V24 MIGRATION COMPLETE ===';
    RAISE NOTICE 'Record counts with codes:';
    RAISE NOTICE '  tenants: % (all have code)', v_tenants_count;
    RAISE NOTICE '  users: % (all have code)', v_users_count;
    RAISE NOTICE '  greenhouses: % (all have code)', v_greenhouses_count;
    RAISE NOTICE '  sectors: % (all have code)', v_sectors_count;
    RAISE NOTICE '  devices: % (all have code)', v_devices_count;
    RAISE NOTICE '  alerts: % (all have code)', v_alerts_count;
    RAISE NOTICE '  settings: % (all have code)', v_settings_count;
    RAISE NOTICE '  command_history: % (all have code)', v_command_history_count;
    RAISE NOTICE '  mqtt_users: % (greenhouse_id/tenant_id now BIGINT)', v_mqtt_users_count;
    RAISE NOTICE 'New records will use TSID for id column';
    RAISE NOTICE 'Existing records keep their original ids';
    RAISE NOTICE 'Timestamp: %', NOW();
END $$;

-- =============================================================================
-- FASE 9: Crear secuencias para generacion de codigos
-- Las secuencias inician en el max(id) + 1 para evitar colisiones con codigos existentes
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 9: Creating sequences for code generation...'; END $$;

-- Secuencia para tenants
DO $$
DECLARE v_max_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) INTO v_max_id FROM metadata.tenants;
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS metadata.tenants_code_seq START WITH %s', v_max_id + 1);
    RAISE NOTICE 'Created metadata.tenants_code_seq starting at %', v_max_id + 1;
END $$;

-- Secuencia para users
DO $$
DECLARE v_max_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) INTO v_max_id FROM metadata.users;
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS metadata.users_code_seq START WITH %s', v_max_id + 1);
    RAISE NOTICE 'Created metadata.users_code_seq starting at %', v_max_id + 1;
END $$;

-- Secuencia para greenhouses
DO $$
DECLARE v_max_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) INTO v_max_id FROM metadata.greenhouses;
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS metadata.greenhouses_code_seq START WITH %s', v_max_id + 1);
    RAISE NOTICE 'Created metadata.greenhouses_code_seq starting at %', v_max_id + 1;
END $$;

-- Secuencia para sectors
DO $$
DECLARE v_max_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) INTO v_max_id FROM metadata.sectors;
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS metadata.sectors_code_seq START WITH %s', v_max_id + 1);
    RAISE NOTICE 'Created metadata.sectors_code_seq starting at %', v_max_id + 1;
END $$;

-- Secuencia para devices
DO $$
DECLARE v_max_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) INTO v_max_id FROM metadata.devices;
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS metadata.devices_code_seq START WITH %s', v_max_id + 1);
    RAISE NOTICE 'Created metadata.devices_code_seq starting at %', v_max_id + 1;
END $$;

-- Secuencia para alerts
DO $$
DECLARE v_max_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) INTO v_max_id FROM metadata.alerts;
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS metadata.alerts_code_seq START WITH %s', v_max_id + 1);
    RAISE NOTICE 'Created metadata.alerts_code_seq starting at %', v_max_id + 1;
END $$;

-- Secuencia para settings
DO $$
DECLARE v_max_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) INTO v_max_id FROM metadata.settings;
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS metadata.settings_code_seq START WITH %s', v_max_id + 1);
    RAISE NOTICE 'Created metadata.settings_code_seq starting at %', v_max_id + 1;
END $$;

-- Secuencia para command_history
DO $$
DECLARE v_max_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) INTO v_max_id FROM metadata.command_history;
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS metadata.command_history_code_seq START WITH %s', v_max_id + 1);
    RAISE NOTICE 'Created metadata.command_history_code_seq starting at %', v_max_id + 1;
END $$;

-- =============================================================================
-- EJEMPLOS DE USO:
-- =============================================================================
-- Insertar nuevo tenant (id sera TSID automaticamente):
--   INSERT INTO metadata.tenants (name, email, code)
--   VALUES ('Nuevo Tenant', 'nuevo@test.com', 'TNT-NUEVO1');
--
-- El id generado sera algo como: 7154891587387392001
--
-- Buscar por code (para PLCs y APIs externas):
--   SELECT * FROM metadata.devices WHERE code = 'DEV-00001';
--
-- Generar TSID manualmente:
--   SELECT metadata.generate_tsid();     -- Usa node_id=1
--   SELECT metadata.generate_tsid(2);    -- Usa node_id=2
--
-- Generar codigo para nuevo tenant:
--   SELECT 'TNT-' || LPAD(nextval('metadata.tenants_code_seq')::TEXT, 5, '0');
-- =============================================================================
