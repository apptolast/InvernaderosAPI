-- =============================================================================
-- V22: Migrate UUID Primary Keys to BIGINT with Auto-Generation
-- Fecha: 2026-01-08
-- Descripcion: Migra todas las tablas con UUID a BIGINT para mejor rendimiento.
--              EXCLUYE: mqtt_users, mqtt_acl (se mantienen con sus IDs actuales)
--
-- Tablas a migrar (orden por dependencias FK):
--   1. tenants        (raiz - sin FKs UUID)
--   2. users          (FK: tenants)
--   3. greenhouses    (FK: tenants)
--   4. sectors        (FK: greenhouses)
--   5. devices        (FK: greenhouses, tenants)
--   6. alerts         (FK: greenhouses, tenants, users)
--   7. settings       (FK: greenhouses, tenants)
--   8. command_history (FK: devices, users)
--
-- IMPORTANTE: Esta migración preserva TODOS los datos existentes mediante
--             tablas de mapeo UUID→BIGINT.
-- =============================================================================

-- Registrar inicio de migración
DO $$
BEGIN
    RAISE NOTICE '=== V22 MIGRATION START: UUID to BIGINT ===';
    RAISE NOTICE 'Timestamp: %', NOW();
END $$;

-- =============================================================================
-- FASE 0: Verificación previa
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
BEGIN
    -- Contar registros en cada tabla
    SELECT COUNT(*) INTO v_tenants_count FROM metadata.tenants;
    SELECT COUNT(*) INTO v_users_count FROM metadata.users;
    SELECT COUNT(*) INTO v_greenhouses_count FROM metadata.greenhouses;
    SELECT COUNT(*) INTO v_sectors_count FROM metadata.sectors;
    SELECT COUNT(*) INTO v_devices_count FROM metadata.devices;
    SELECT COUNT(*) INTO v_alerts_count FROM metadata.alerts;
    SELECT COUNT(*) INTO v_settings_count FROM metadata.settings;
    SELECT COUNT(*) INTO v_command_history_count FROM metadata.command_history;

    RAISE NOTICE 'Pre-migration record counts:';
    RAISE NOTICE '  tenants: %', v_tenants_count;
    RAISE NOTICE '  users: %', v_users_count;
    RAISE NOTICE '  greenhouses: %', v_greenhouses_count;
    RAISE NOTICE '  sectors: %', v_sectors_count;
    RAISE NOTICE '  devices: %', v_devices_count;
    RAISE NOTICE '  alerts: %', v_alerts_count;
    RAISE NOTICE '  settings: %', v_settings_count;
    RAISE NOTICE '  command_history: %', v_command_history_count;
END $$;

-- =============================================================================
-- FASE 1: Crear tablas de mapeo UUID → BIGINT
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 1: Creating UUID to BIGINT mapping tables...'; END $$;

CREATE TABLE IF NOT EXISTS metadata.uuid_mapping_tenants (
    old_uuid UUID PRIMARY KEY,
    new_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS metadata.uuid_mapping_users (
    old_uuid UUID PRIMARY KEY,
    new_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS metadata.uuid_mapping_greenhouses (
    old_uuid UUID PRIMARY KEY,
    new_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS metadata.uuid_mapping_sectors (
    old_uuid UUID PRIMARY KEY,
    new_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS metadata.uuid_mapping_devices (
    old_uuid UUID PRIMARY KEY,
    new_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS metadata.uuid_mapping_alerts (
    old_uuid UUID PRIMARY KEY,
    new_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS metadata.uuid_mapping_settings (
    old_uuid UUID PRIMARY KEY,
    new_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS metadata.uuid_mapping_command_history (
    old_uuid UUID PRIMARY KEY,
    new_id BIGINT NOT NULL
);

-- =============================================================================
-- FASE 2: Crear secuencias para nuevos IDs
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 2: Creating sequences...'; END $$;

CREATE SEQUENCE IF NOT EXISTS metadata.tenants_id_seq
    AS BIGINT START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE SEQUENCE IF NOT EXISTS metadata.users_id_seq
    AS BIGINT START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE SEQUENCE IF NOT EXISTS metadata.greenhouses_id_seq
    AS BIGINT START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE SEQUENCE IF NOT EXISTS metadata.sectors_id_seq
    AS BIGINT START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE SEQUENCE IF NOT EXISTS metadata.devices_id_seq
    AS BIGINT START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE SEQUENCE IF NOT EXISTS metadata.alerts_id_seq
    AS BIGINT START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE SEQUENCE IF NOT EXISTS metadata.settings_id_seq
    AS BIGINT START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE SEQUENCE IF NOT EXISTS metadata.command_history_id_seq
    AS BIGINT START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- =============================================================================
-- FASE 3: TENANTS - Tabla raíz (sin FKs a otras tablas UUID)
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 3: Migrating TENANTS table...'; END $$;

-- 3.1 Añadir nueva columna BIGINT
ALTER TABLE metadata.tenants ADD COLUMN IF NOT EXISTS new_id BIGINT;

-- 3.2 Poblar mapeo y nueva columna
INSERT INTO metadata.uuid_mapping_tenants (old_uuid, new_id)
SELECT id, nextval('metadata.tenants_id_seq')
FROM metadata.tenants
WHERE id IS NOT NULL
ON CONFLICT (old_uuid) DO NOTHING;

UPDATE metadata.tenants t
SET new_id = m.new_id
FROM metadata.uuid_mapping_tenants m
WHERE t.id = m.old_uuid AND t.new_id IS NULL;

-- =============================================================================
-- FASE 4: USERS - FK a tenants
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 4: Migrating USERS table...'; END $$;

-- 4.1 Añadir nuevas columnas
ALTER TABLE metadata.users ADD COLUMN IF NOT EXISTS new_id BIGINT;
ALTER TABLE metadata.users ADD COLUMN IF NOT EXISTS new_tenant_id BIGINT;

-- 4.2 Poblar mapeo y nuevas columnas
INSERT INTO metadata.uuid_mapping_users (old_uuid, new_id)
SELECT id, nextval('metadata.users_id_seq')
FROM metadata.users
WHERE id IS NOT NULL
ON CONFLICT (old_uuid) DO NOTHING;

UPDATE metadata.users u
SET new_id = m.new_id
FROM metadata.uuid_mapping_users m
WHERE u.id = m.old_uuid AND u.new_id IS NULL;

UPDATE metadata.users u
SET new_tenant_id = m.new_id
FROM metadata.uuid_mapping_tenants m
WHERE u.tenant_id = m.old_uuid AND u.new_tenant_id IS NULL;

-- =============================================================================
-- FASE 5: GREENHOUSES - FK a tenants
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 5: Migrating GREENHOUSES table...'; END $$;

-- 5.1 Añadir nuevas columnas
ALTER TABLE metadata.greenhouses ADD COLUMN IF NOT EXISTS new_id BIGINT;
ALTER TABLE metadata.greenhouses ADD COLUMN IF NOT EXISTS new_tenant_id BIGINT;

-- 5.2 Poblar mapeo y nuevas columnas
INSERT INTO metadata.uuid_mapping_greenhouses (old_uuid, new_id)
SELECT id, nextval('metadata.greenhouses_id_seq')
FROM metadata.greenhouses
WHERE id IS NOT NULL
ON CONFLICT (old_uuid) DO NOTHING;

UPDATE metadata.greenhouses g
SET new_id = m.new_id
FROM metadata.uuid_mapping_greenhouses m
WHERE g.id = m.old_uuid AND g.new_id IS NULL;

UPDATE metadata.greenhouses g
SET new_tenant_id = m.new_id
FROM metadata.uuid_mapping_tenants m
WHERE g.tenant_id = m.old_uuid AND g.new_tenant_id IS NULL;

-- =============================================================================
-- FASE 6: SECTORS - FK a greenhouses
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 6: Migrating SECTORS table...'; END $$;

-- 6.1 Añadir nuevas columnas
ALTER TABLE metadata.sectors ADD COLUMN IF NOT EXISTS new_id BIGINT;
ALTER TABLE metadata.sectors ADD COLUMN IF NOT EXISTS new_greenhouse_id BIGINT;

-- 6.2 Poblar mapeo y nuevas columnas
INSERT INTO metadata.uuid_mapping_sectors (old_uuid, new_id)
SELECT id, nextval('metadata.sectors_id_seq')
FROM metadata.sectors
WHERE id IS NOT NULL
ON CONFLICT (old_uuid) DO NOTHING;

UPDATE metadata.sectors s
SET new_id = m.new_id
FROM metadata.uuid_mapping_sectors m
WHERE s.id = m.old_uuid AND s.new_id IS NULL;

UPDATE metadata.sectors s
SET new_greenhouse_id = m.new_id
FROM metadata.uuid_mapping_greenhouses m
WHERE s.greenhouse_id = m.old_uuid AND s.new_greenhouse_id IS NULL;

-- =============================================================================
-- FASE 7: DEVICES - FK a greenhouses, tenants
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 7: Migrating DEVICES table...'; END $$;

-- 7.1 Añadir nuevas columnas
ALTER TABLE metadata.devices ADD COLUMN IF NOT EXISTS new_id BIGINT;
ALTER TABLE metadata.devices ADD COLUMN IF NOT EXISTS new_tenant_id BIGINT;
ALTER TABLE metadata.devices ADD COLUMN IF NOT EXISTS new_greenhouse_id BIGINT;

-- 7.2 Poblar mapeo y nuevas columnas
INSERT INTO metadata.uuid_mapping_devices (old_uuid, new_id)
SELECT id, nextval('metadata.devices_id_seq')
FROM metadata.devices
WHERE id IS NOT NULL
ON CONFLICT (old_uuid) DO NOTHING;

UPDATE metadata.devices d
SET new_id = m.new_id
FROM metadata.uuid_mapping_devices m
WHERE d.id = m.old_uuid AND d.new_id IS NULL;

UPDATE metadata.devices d
SET new_tenant_id = m.new_id
FROM metadata.uuid_mapping_tenants m
WHERE d.tenant_id = m.old_uuid AND d.new_tenant_id IS NULL;

UPDATE metadata.devices d
SET new_greenhouse_id = m.new_id
FROM metadata.uuid_mapping_greenhouses m
WHERE d.greenhouse_id = m.old_uuid AND d.new_greenhouse_id IS NULL;

-- =============================================================================
-- FASE 8: ALERTS - FK a greenhouses, tenants, users
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 8: Migrating ALERTS table...'; END $$;

-- 8.1 Añadir nuevas columnas
ALTER TABLE metadata.alerts ADD COLUMN IF NOT EXISTS new_id BIGINT;
ALTER TABLE metadata.alerts ADD COLUMN IF NOT EXISTS new_tenant_id BIGINT;
ALTER TABLE metadata.alerts ADD COLUMN IF NOT EXISTS new_greenhouse_id BIGINT;
ALTER TABLE metadata.alerts ADD COLUMN IF NOT EXISTS new_resolved_by_user_id BIGINT;

-- 8.2 Poblar mapeo y nuevas columnas
INSERT INTO metadata.uuid_mapping_alerts (old_uuid, new_id)
SELECT id, nextval('metadata.alerts_id_seq')
FROM metadata.alerts
WHERE id IS NOT NULL
ON CONFLICT (old_uuid) DO NOTHING;

UPDATE metadata.alerts a
SET new_id = m.new_id
FROM metadata.uuid_mapping_alerts m
WHERE a.id = m.old_uuid AND a.new_id IS NULL;

UPDATE metadata.alerts a
SET new_tenant_id = m.new_id
FROM metadata.uuid_mapping_tenants m
WHERE a.tenant_id = m.old_uuid AND a.new_tenant_id IS NULL;

UPDATE metadata.alerts a
SET new_greenhouse_id = m.new_id
FROM metadata.uuid_mapping_greenhouses m
WHERE a.greenhouse_id = m.old_uuid AND a.new_greenhouse_id IS NULL;

UPDATE metadata.alerts a
SET new_resolved_by_user_id = m.new_id
FROM metadata.uuid_mapping_users m
WHERE a.resolved_by_user_id = m.old_uuid AND a.new_resolved_by_user_id IS NULL;

-- =============================================================================
-- FASE 9: SETTINGS - FK a greenhouses, tenants
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 9: Migrating SETTINGS table...'; END $$;

-- 9.1 Añadir nuevas columnas
ALTER TABLE metadata.settings ADD COLUMN IF NOT EXISTS new_id BIGINT;
ALTER TABLE metadata.settings ADD COLUMN IF NOT EXISTS new_tenant_id BIGINT;
ALTER TABLE metadata.settings ADD COLUMN IF NOT EXISTS new_greenhouse_id BIGINT;

-- 9.2 Poblar mapeo y nuevas columnas
INSERT INTO metadata.uuid_mapping_settings (old_uuid, new_id)
SELECT id, nextval('metadata.settings_id_seq')
FROM metadata.settings
WHERE id IS NOT NULL
ON CONFLICT (old_uuid) DO NOTHING;

UPDATE metadata.settings s
SET new_id = m.new_id
FROM metadata.uuid_mapping_settings m
WHERE s.id = m.old_uuid AND s.new_id IS NULL;

UPDATE metadata.settings s
SET new_tenant_id = m.new_id
FROM metadata.uuid_mapping_tenants m
WHERE s.tenant_id = m.old_uuid AND s.new_tenant_id IS NULL;

UPDATE metadata.settings s
SET new_greenhouse_id = m.new_id
FROM metadata.uuid_mapping_greenhouses m
WHERE s.greenhouse_id = m.old_uuid AND s.new_greenhouse_id IS NULL;

-- =============================================================================
-- FASE 10: COMMAND_HISTORY - FK a devices, users
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 10: Migrating COMMAND_HISTORY table...'; END $$;

-- 10.1 Añadir nuevas columnas
ALTER TABLE metadata.command_history ADD COLUMN IF NOT EXISTS new_id BIGINT;
ALTER TABLE metadata.command_history ADD COLUMN IF NOT EXISTS new_device_id BIGINT;
ALTER TABLE metadata.command_history ADD COLUMN IF NOT EXISTS new_user_id BIGINT;

-- 10.2 Poblar mapeo y nuevas columnas
INSERT INTO metadata.uuid_mapping_command_history (old_uuid, new_id)
SELECT id, nextval('metadata.command_history_id_seq')
FROM metadata.command_history
WHERE id IS NOT NULL
ON CONFLICT (old_uuid) DO NOTHING;

UPDATE metadata.command_history c
SET new_id = m.new_id
FROM metadata.uuid_mapping_command_history m
WHERE c.id = m.old_uuid AND c.new_id IS NULL;

UPDATE metadata.command_history c
SET new_device_id = m.new_id
FROM metadata.uuid_mapping_devices m
WHERE c.device_id = m.old_uuid AND c.new_device_id IS NULL;

UPDATE metadata.command_history c
SET new_user_id = m.new_id
FROM metadata.uuid_mapping_users m
WHERE c.user_id = m.old_uuid AND c.new_user_id IS NULL;

-- =============================================================================
-- FASE 11: Eliminar constraints e índices UUID existentes
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 11: Dropping old constraints and indexes...'; END $$;

-- 11.1 COMMAND_HISTORY (hoja del árbol - sin dependencias)
ALTER TABLE metadata.command_history DROP CONSTRAINT IF EXISTS command_history_pkey CASCADE;
DROP INDEX IF EXISTS metadata.idx_command_history_device;
DROP INDEX IF EXISTS metadata.idx_command_history_device_time;

-- 11.2 SETTINGS
ALTER TABLE metadata.settings DROP CONSTRAINT IF EXISTS settings_pkey CASCADE;
DROP INDEX IF EXISTS metadata.idx_settings_greenhouse_id;
DROP INDEX IF EXISTS metadata.idx_settings_tenant_id;
ALTER TABLE metadata.settings DROP CONSTRAINT IF EXISTS uq_setting_greenhouse_parameter_period CASCADE;

-- 11.3 ALERTS
ALTER TABLE metadata.alerts DROP CONSTRAINT IF EXISTS alerts_pkey CASCADE;
DROP INDEX IF EXISTS metadata.idx_alerts_tenant;
DROP INDEX IF EXISTS metadata.idx_alerts_greenhouse;
DROP INDEX IF EXISTS metadata.idx_alerts_tenant_unresolved;
DROP INDEX IF EXISTS metadata.idx_alerts_greenhouse_severity_status;

-- 11.4 DEVICES
ALTER TABLE metadata.devices DROP CONSTRAINT IF EXISTS devices_pkey CASCADE;
DROP INDEX IF EXISTS metadata.idx_devices_new_tenant;
DROP INDEX IF EXISTS metadata.idx_devices_new_greenhouse;

-- 11.5 SECTORS
ALTER TABLE metadata.sectors DROP CONSTRAINT IF EXISTS sectors_pkey CASCADE;
DROP INDEX IF EXISTS metadata.idx_sectors_greenhouse;

-- 11.6 GREENHOUSES
ALTER TABLE metadata.greenhouses DROP CONSTRAINT IF EXISTS greenhouses_pkey CASCADE;
DROP INDEX IF EXISTS metadata.idx_greenhouses_tenant;
DROP INDEX IF EXISTS metadata.idx_greenhouses_tenant_active;
ALTER TABLE metadata.greenhouses DROP CONSTRAINT IF EXISTS uq_greenhouse_tenant_name CASCADE;

-- 11.7 USERS
ALTER TABLE metadata.users DROP CONSTRAINT IF EXISTS users_pkey CASCADE;
DROP INDEX IF EXISTS metadata.idx_users_tenant_id;

-- 11.8 TENANTS
ALTER TABLE metadata.tenants DROP CONSTRAINT IF EXISTS tenants_pkey CASCADE;

-- =============================================================================
-- FASE 12: Eliminar columnas UUID antiguas
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 12: Dropping old UUID columns...'; END $$;

-- COMMAND_HISTORY
ALTER TABLE metadata.command_history DROP COLUMN IF EXISTS id;
ALTER TABLE metadata.command_history DROP COLUMN IF EXISTS device_id;
ALTER TABLE metadata.command_history DROP COLUMN IF EXISTS user_id;

-- SETTINGS
ALTER TABLE metadata.settings DROP COLUMN IF EXISTS id;
ALTER TABLE metadata.settings DROP COLUMN IF EXISTS greenhouse_id;
ALTER TABLE metadata.settings DROP COLUMN IF EXISTS tenant_id;

-- ALERTS
ALTER TABLE metadata.alerts DROP COLUMN IF EXISTS id;
ALTER TABLE metadata.alerts DROP COLUMN IF EXISTS greenhouse_id;
ALTER TABLE metadata.alerts DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE metadata.alerts DROP COLUMN IF EXISTS resolved_by_user_id;

-- DEVICES
ALTER TABLE metadata.devices DROP COLUMN IF EXISTS id;
ALTER TABLE metadata.devices DROP COLUMN IF EXISTS greenhouse_id;
ALTER TABLE metadata.devices DROP COLUMN IF EXISTS tenant_id;

-- SECTORS
ALTER TABLE metadata.sectors DROP COLUMN IF EXISTS id;
ALTER TABLE metadata.sectors DROP COLUMN IF EXISTS greenhouse_id;

-- GREENHOUSES
ALTER TABLE metadata.greenhouses DROP COLUMN IF EXISTS id;
ALTER TABLE metadata.greenhouses DROP COLUMN IF EXISTS tenant_id;

-- USERS
ALTER TABLE metadata.users DROP COLUMN IF EXISTS id;
ALTER TABLE metadata.users DROP COLUMN IF EXISTS tenant_id;

-- TENANTS
ALTER TABLE metadata.tenants DROP COLUMN IF EXISTS id;

-- =============================================================================
-- FASE 13: Renombrar columnas nuevas a nombres originales
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 13: Renaming new columns to original names...'; END $$;

-- TENANTS
ALTER TABLE metadata.tenants RENAME COLUMN new_id TO id;

-- USERS
ALTER TABLE metadata.users RENAME COLUMN new_id TO id;
ALTER TABLE metadata.users RENAME COLUMN new_tenant_id TO tenant_id;

-- GREENHOUSES
ALTER TABLE metadata.greenhouses RENAME COLUMN new_id TO id;
ALTER TABLE metadata.greenhouses RENAME COLUMN new_tenant_id TO tenant_id;

-- SECTORS
ALTER TABLE metadata.sectors RENAME COLUMN new_id TO id;
ALTER TABLE metadata.sectors RENAME COLUMN new_greenhouse_id TO greenhouse_id;

-- DEVICES
ALTER TABLE metadata.devices RENAME COLUMN new_id TO id;
ALTER TABLE metadata.devices RENAME COLUMN new_tenant_id TO tenant_id;
ALTER TABLE metadata.devices RENAME COLUMN new_greenhouse_id TO greenhouse_id;

-- ALERTS
ALTER TABLE metadata.alerts RENAME COLUMN new_id TO id;
ALTER TABLE metadata.alerts RENAME COLUMN new_tenant_id TO tenant_id;
ALTER TABLE metadata.alerts RENAME COLUMN new_greenhouse_id TO greenhouse_id;
ALTER TABLE metadata.alerts RENAME COLUMN new_resolved_by_user_id TO resolved_by_user_id;

-- SETTINGS
ALTER TABLE metadata.settings RENAME COLUMN new_id TO id;
ALTER TABLE metadata.settings RENAME COLUMN new_tenant_id TO tenant_id;
ALTER TABLE metadata.settings RENAME COLUMN new_greenhouse_id TO greenhouse_id;

-- COMMAND_HISTORY
ALTER TABLE metadata.command_history RENAME COLUMN new_id TO id;
ALTER TABLE metadata.command_history RENAME COLUMN new_device_id TO device_id;
ALTER TABLE metadata.command_history RENAME COLUMN new_user_id TO user_id;

-- =============================================================================
-- FASE 14: Configurar defaults con secuencias
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 14: Setting up sequence defaults...'; END $$;

-- Actualizar secuencias al valor máximo + 1
SELECT setval('metadata.tenants_id_seq', COALESCE((SELECT MAX(id) FROM metadata.tenants), 0) + 1, false);
SELECT setval('metadata.users_id_seq', COALESCE((SELECT MAX(id) FROM metadata.users), 0) + 1, false);
SELECT setval('metadata.greenhouses_id_seq', COALESCE((SELECT MAX(id) FROM metadata.greenhouses), 0) + 1, false);
SELECT setval('metadata.sectors_id_seq', COALESCE((SELECT MAX(id) FROM metadata.sectors), 0) + 1, false);
SELECT setval('metadata.devices_id_seq', COALESCE((SELECT MAX(id) FROM metadata.devices), 0) + 1, false);
SELECT setval('metadata.alerts_id_seq', COALESCE((SELECT MAX(id) FROM metadata.alerts), 0) + 1, false);
SELECT setval('metadata.settings_id_seq', COALESCE((SELECT MAX(id) FROM metadata.settings), 0) + 1, false);
SELECT setval('metadata.command_history_id_seq', COALESCE((SELECT MAX(id) FROM metadata.command_history), 0) + 1, false);

-- Establecer defaults
ALTER TABLE metadata.tenants ALTER COLUMN id SET DEFAULT nextval('metadata.tenants_id_seq');
ALTER TABLE metadata.users ALTER COLUMN id SET DEFAULT nextval('metadata.users_id_seq');
ALTER TABLE metadata.greenhouses ALTER COLUMN id SET DEFAULT nextval('metadata.greenhouses_id_seq');
ALTER TABLE metadata.sectors ALTER COLUMN id SET DEFAULT nextval('metadata.sectors_id_seq');
ALTER TABLE metadata.devices ALTER COLUMN id SET DEFAULT nextval('metadata.devices_id_seq');
ALTER TABLE metadata.alerts ALTER COLUMN id SET DEFAULT nextval('metadata.alerts_id_seq');
ALTER TABLE metadata.settings ALTER COLUMN id SET DEFAULT nextval('metadata.settings_id_seq');
ALTER TABLE metadata.command_history ALTER COLUMN id SET DEFAULT nextval('metadata.command_history_id_seq');

-- Asociar secuencias a columnas
ALTER SEQUENCE metadata.tenants_id_seq OWNED BY metadata.tenants.id;
ALTER SEQUENCE metadata.users_id_seq OWNED BY metadata.users.id;
ALTER SEQUENCE metadata.greenhouses_id_seq OWNED BY metadata.greenhouses.id;
ALTER SEQUENCE metadata.sectors_id_seq OWNED BY metadata.sectors.id;
ALTER SEQUENCE metadata.devices_id_seq OWNED BY metadata.devices.id;
ALTER SEQUENCE metadata.alerts_id_seq OWNED BY metadata.alerts.id;
ALTER SEQUENCE metadata.settings_id_seq OWNED BY metadata.settings.id;
ALTER SEQUENCE metadata.command_history_id_seq OWNED BY metadata.command_history.id;

-- =============================================================================
-- FASE 15: Recrear Primary Keys
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 15: Recreating primary keys...'; END $$;

ALTER TABLE metadata.tenants ADD PRIMARY KEY (id);
ALTER TABLE metadata.users ADD PRIMARY KEY (id);
ALTER TABLE metadata.greenhouses ADD PRIMARY KEY (id);
ALTER TABLE metadata.sectors ADD PRIMARY KEY (id);
ALTER TABLE metadata.devices ADD PRIMARY KEY (id);
ALTER TABLE metadata.alerts ADD PRIMARY KEY (id);
ALTER TABLE metadata.settings ADD PRIMARY KEY (id);
ALTER TABLE metadata.command_history ADD PRIMARY KEY (id);

-- =============================================================================
-- FASE 16: Recrear Foreign Keys
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 16: Recreating foreign keys...'; END $$;

-- USERS → TENANTS
ALTER TABLE metadata.users
    ADD CONSTRAINT fk_users_tenant
    FOREIGN KEY (tenant_id) REFERENCES metadata.tenants(id)
    ON DELETE CASCADE;

-- GREENHOUSES → TENANTS
ALTER TABLE metadata.greenhouses
    ADD CONSTRAINT fk_greenhouses_tenant
    FOREIGN KEY (tenant_id) REFERENCES metadata.tenants(id)
    ON DELETE CASCADE;

-- SECTORS → GREENHOUSES
ALTER TABLE metadata.sectors
    ADD CONSTRAINT fk_sectors_greenhouse
    FOREIGN KEY (greenhouse_id) REFERENCES metadata.greenhouses(id)
    ON DELETE CASCADE;

-- DEVICES → TENANTS, GREENHOUSES
ALTER TABLE metadata.devices
    ADD CONSTRAINT fk_devices_tenant
    FOREIGN KEY (tenant_id) REFERENCES metadata.tenants(id)
    ON DELETE CASCADE;

ALTER TABLE metadata.devices
    ADD CONSTRAINT fk_devices_greenhouse
    FOREIGN KEY (greenhouse_id) REFERENCES metadata.greenhouses(id)
    ON DELETE CASCADE;

-- ALERTS → TENANTS, GREENHOUSES, USERS
ALTER TABLE metadata.alerts
    ADD CONSTRAINT fk_alerts_tenant
    FOREIGN KEY (tenant_id) REFERENCES metadata.tenants(id)
    ON DELETE CASCADE;

ALTER TABLE metadata.alerts
    ADD CONSTRAINT fk_alerts_greenhouse
    FOREIGN KEY (greenhouse_id) REFERENCES metadata.greenhouses(id)
    ON DELETE CASCADE;

ALTER TABLE metadata.alerts
    ADD CONSTRAINT fk_alerts_resolved_by_user
    FOREIGN KEY (resolved_by_user_id) REFERENCES metadata.users(id)
    ON DELETE SET NULL;

-- SETTINGS → TENANTS, GREENHOUSES
ALTER TABLE metadata.settings
    ADD CONSTRAINT fk_settings_tenant
    FOREIGN KEY (tenant_id) REFERENCES metadata.tenants(id)
    ON DELETE CASCADE;

ALTER TABLE metadata.settings
    ADD CONSTRAINT fk_settings_greenhouse
    FOREIGN KEY (greenhouse_id) REFERENCES metadata.greenhouses(id)
    ON DELETE CASCADE;

-- COMMAND_HISTORY → DEVICES, USERS
ALTER TABLE metadata.command_history
    ADD CONSTRAINT fk_command_history_device
    FOREIGN KEY (device_id) REFERENCES metadata.devices(id)
    ON DELETE CASCADE;

ALTER TABLE metadata.command_history
    ADD CONSTRAINT fk_command_history_user
    FOREIGN KEY (user_id) REFERENCES metadata.users(id)
    ON DELETE SET NULL;

-- =============================================================================
-- FASE 17: Recrear índices optimizados
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 17: Recreating optimized indexes...'; END $$;

-- TENANTS
CREATE INDEX idx_tenants_active ON metadata.tenants(is_active);
CREATE INDEX idx_tenants_name ON metadata.tenants(name);

-- USERS
CREATE INDEX idx_users_tenant_id ON metadata.users(tenant_id);
CREATE INDEX idx_users_email ON metadata.users(email);
CREATE INDEX idx_users_username ON metadata.users(username);

-- GREENHOUSES
CREATE INDEX idx_greenhouses_tenant ON metadata.greenhouses(tenant_id);
CREATE INDEX idx_greenhouses_active ON metadata.greenhouses(is_active);
CREATE INDEX idx_greenhouses_tenant_active ON metadata.greenhouses(tenant_id, is_active);

-- SECTORS
CREATE INDEX idx_sectors_greenhouse ON metadata.sectors(greenhouse_id);

-- DEVICES
CREATE INDEX idx_devices_tenant ON metadata.devices(tenant_id);
CREATE INDEX idx_devices_greenhouse ON metadata.devices(greenhouse_id);
CREATE INDEX idx_devices_active ON metadata.devices(is_active);
CREATE INDEX idx_devices_tenant_greenhouse ON metadata.devices(tenant_id, greenhouse_id);

-- ALERTS
CREATE INDEX idx_alerts_tenant ON metadata.alerts(tenant_id);
CREATE INDEX idx_alerts_greenhouse ON metadata.alerts(greenhouse_id);
CREATE INDEX idx_alerts_resolved ON metadata.alerts(is_resolved);
CREATE INDEX idx_alerts_created_at ON metadata.alerts(created_at DESC);
CREATE INDEX idx_alerts_tenant_unresolved ON metadata.alerts(tenant_id, is_resolved, created_at DESC);
CREATE INDEX idx_alerts_greenhouse_severity ON metadata.alerts(greenhouse_id, severity_id, is_resolved);

-- SETTINGS
CREATE INDEX idx_settings_tenant ON metadata.settings(tenant_id);
CREATE INDEX idx_settings_greenhouse ON metadata.settings(greenhouse_id);

-- COMMAND_HISTORY
CREATE INDEX idx_command_history_device ON metadata.command_history(device_id);
CREATE INDEX idx_command_history_user ON metadata.command_history(user_id);
CREATE INDEX idx_command_history_created ON metadata.command_history(created_at DESC);
CREATE INDEX idx_command_history_device_time ON metadata.command_history(device_id, created_at DESC);

-- =============================================================================
-- FASE 18: Recrear unique constraints
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 18: Recreating unique constraints...'; END $$;

ALTER TABLE metadata.greenhouses
    ADD CONSTRAINT uq_greenhouse_tenant_name
    UNIQUE (tenant_id, name);

ALTER TABLE metadata.settings
    ADD CONSTRAINT uq_setting_greenhouse_parameter_period
    UNIQUE (greenhouse_id, parameter_id, period_id);

-- =============================================================================
-- FASE 19: Actualizar NOT NULL constraints
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 19: Setting NOT NULL constraints...'; END $$;

ALTER TABLE metadata.tenants ALTER COLUMN id SET NOT NULL;
ALTER TABLE metadata.users ALTER COLUMN id SET NOT NULL;
ALTER TABLE metadata.users ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE metadata.greenhouses ALTER COLUMN id SET NOT NULL;
ALTER TABLE metadata.greenhouses ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE metadata.sectors ALTER COLUMN id SET NOT NULL;
ALTER TABLE metadata.sectors ALTER COLUMN greenhouse_id SET NOT NULL;
ALTER TABLE metadata.devices ALTER COLUMN id SET NOT NULL;
ALTER TABLE metadata.devices ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE metadata.devices ALTER COLUMN greenhouse_id SET NOT NULL;
ALTER TABLE metadata.alerts ALTER COLUMN id SET NOT NULL;
ALTER TABLE metadata.alerts ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE metadata.alerts ALTER COLUMN greenhouse_id SET NOT NULL;
ALTER TABLE metadata.settings ALTER COLUMN id SET NOT NULL;
ALTER TABLE metadata.settings ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE metadata.settings ALTER COLUMN greenhouse_id SET NOT NULL;
ALTER TABLE metadata.command_history ALTER COLUMN id SET NOT NULL;
ALTER TABLE metadata.command_history ALTER COLUMN device_id SET NOT NULL;

-- =============================================================================
-- FASE 20: Verificación final
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
    v_tenants_id_type TEXT;
    v_users_id_type TEXT;
    v_greenhouses_id_type TEXT;
BEGIN
    -- Contar registros post-migración
    SELECT COUNT(*) INTO v_tenants_count FROM metadata.tenants;
    SELECT COUNT(*) INTO v_users_count FROM metadata.users;
    SELECT COUNT(*) INTO v_greenhouses_count FROM metadata.greenhouses;
    SELECT COUNT(*) INTO v_sectors_count FROM metadata.sectors;
    SELECT COUNT(*) INTO v_devices_count FROM metadata.devices;
    SELECT COUNT(*) INTO v_alerts_count FROM metadata.alerts;
    SELECT COUNT(*) INTO v_settings_count FROM metadata.settings;
    SELECT COUNT(*) INTO v_command_history_count FROM metadata.command_history;

    -- Verificar tipos de datos
    SELECT data_type INTO v_tenants_id_type
    FROM information_schema.columns
    WHERE table_schema = 'metadata' AND table_name = 'tenants' AND column_name = 'id';

    SELECT data_type INTO v_users_id_type
    FROM information_schema.columns
    WHERE table_schema = 'metadata' AND table_name = 'users' AND column_name = 'id';

    SELECT data_type INTO v_greenhouses_id_type
    FROM information_schema.columns
    WHERE table_schema = 'metadata' AND table_name = 'greenhouses' AND column_name = 'id';

    RAISE NOTICE '=== V22 MIGRATION COMPLETE ===';
    RAISE NOTICE 'Post-migration record counts:';
    RAISE NOTICE '  tenants: % (id type: %)', v_tenants_count, v_tenants_id_type;
    RAISE NOTICE '  users: % (id type: %)', v_users_count, v_users_id_type;
    RAISE NOTICE '  greenhouses: % (id type: %)', v_greenhouses_count, v_greenhouses_id_type;
    RAISE NOTICE '  sectors: %', v_sectors_count;
    RAISE NOTICE '  devices: %', v_devices_count;
    RAISE NOTICE '  alerts: %', v_alerts_count;
    RAISE NOTICE '  settings: %', v_settings_count;
    RAISE NOTICE '  command_history: %', v_command_history_count;

    -- Verificar que los tipos son BIGINT
    IF v_tenants_id_type != 'bigint' THEN
        RAISE EXCEPTION 'tenants.id is not bigint! Got: %', v_tenants_id_type;
    END IF;
    IF v_users_id_type != 'bigint' THEN
        RAISE EXCEPTION 'users.id is not bigint! Got: %', v_users_id_type;
    END IF;
    IF v_greenhouses_id_type != 'bigint' THEN
        RAISE EXCEPTION 'greenhouses.id is not bigint! Got: %', v_greenhouses_id_type;
    END IF;

    RAISE NOTICE 'All ID columns successfully migrated to BIGINT';
    RAISE NOTICE 'UUID mapping tables preserved for reference';
    RAISE NOTICE 'Timestamp: %', NOW();
END $$;

-- =============================================================================
-- NOTA: Las tablas de mapeo (uuid_mapping_*) se mantienen para referencia.
-- Pueden eliminarse manualmente después de verificar que todo funciona:
--   DROP TABLE metadata.uuid_mapping_tenants;
--   DROP TABLE metadata.uuid_mapping_users;
--   DROP TABLE metadata.uuid_mapping_greenhouses;
--   DROP TABLE metadata.uuid_mapping_sectors;
--   DROP TABLE metadata.uuid_mapping_devices;
--   DROP TABLE metadata.uuid_mapping_alerts;
--   DROP TABLE metadata.uuid_mapping_settings;
--   DROP TABLE metadata.uuid_mapping_command_history;
-- =============================================================================

-- =============================================================================
-- =============================================================================
-- TIMESCALEDB: Migración de iot.sensor_readings
-- =============================================================================
-- =============================================================================
-- NOTA: Esta sección migra la tabla iot.sensor_readings de UUID a BIGINT.
--       Debido a las limitaciones de hypertables, se requiere recrear la tabla.
--       Los datos existentes se migran usando las tablas de mapeo creadas arriba.
-- =============================================================================

DO $$ BEGIN RAISE NOTICE '=== TIMESCALEDB MIGRATION START ==='; END $$;

-- =============================================================================
-- FASE T1: Verificar que existen los mapeos necesarios
-- =============================================================================
DO $$
DECLARE
    v_greenhouse_mappings INT;
    v_tenant_mappings INT;
    v_sensor_readings_count BIGINT;
BEGIN
    SELECT COUNT(*) INTO v_greenhouse_mappings FROM metadata.uuid_mapping_greenhouses;
    SELECT COUNT(*) INTO v_tenant_mappings FROM metadata.uuid_mapping_tenants;

    RAISE NOTICE 'PHASE T1: Verifying mappings...';
    RAISE NOTICE '  greenhouse mappings: %', v_greenhouse_mappings;
    RAISE NOTICE '  tenant mappings: %', v_tenant_mappings;

    -- Contar registros actuales (si existen)
    BEGIN
        SELECT COUNT(*) INTO v_sensor_readings_count FROM iot.sensor_readings;
        RAISE NOTICE '  sensor_readings count: %', v_sensor_readings_count;
    EXCEPTION WHEN undefined_table THEN
        RAISE NOTICE '  sensor_readings table does not exist yet';
        v_sensor_readings_count := 0;
    END;
END $$;

-- =============================================================================
-- FASE T2: Crear tabla temporal para preservar datos
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE T2: Creating backup table...'; END $$;

-- Crear tabla de backup solo si hay datos
DO $$
DECLARE
    v_has_data BOOLEAN;
BEGIN
    -- Verificar si la tabla existe y tiene datos
    BEGIN
        EXECUTE 'SELECT EXISTS(SELECT 1 FROM iot.sensor_readings LIMIT 1)' INTO v_has_data;
    EXCEPTION WHEN undefined_table THEN
        v_has_data := false;
    END;

    IF v_has_data THEN
        RAISE NOTICE 'Creating backup of existing data...';

        DROP TABLE IF EXISTS iot.sensor_readings_backup;

        CREATE TABLE iot.sensor_readings_backup AS
        SELECT
            time,
            sensor_id,
            greenhouse_id AS old_greenhouse_id,
            tenant_id AS old_tenant_id,
            sensor_type,
            value,
            unit
        FROM iot.sensor_readings;

        RAISE NOTICE 'Backup created successfully';
    ELSE
        RAISE NOTICE 'No existing data to backup';
    END IF;
END $$;

-- =============================================================================
-- FASE T3: Eliminar hypertable existente (si existe)
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE T3: Dropping existing hypertable...'; END $$;

-- Eliminar continuous aggregates primero (si existen)
DROP MATERIALIZED VIEW IF EXISTS iot.cagg_sensor_readings_hourly CASCADE;
DROP MATERIALIZED VIEW IF EXISTS iot.cagg_sensor_readings_daily CASCADE;
DROP MATERIALIZED VIEW IF EXISTS iot.sensor_readings_hourly CASCADE;
DROP MATERIALIZED VIEW IF EXISTS iot.sensor_readings_daily_by_tenant CASCADE;

-- Eliminar la tabla hypertable
DROP TABLE IF EXISTS iot.sensor_readings CASCADE;

-- =============================================================================
-- FASE T4: Crear nueva tabla con BIGINT
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE T4: Creating new sensor_readings table with BIGINT...'; END $$;

CREATE TABLE iot.sensor_readings (
    time TIMESTAMPTZ NOT NULL,
    sensor_id VARCHAR(50) NOT NULL,
    greenhouse_id BIGINT NOT NULL,
    tenant_id BIGINT,
    sensor_type VARCHAR(30) NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    unit VARCHAR(20),
    PRIMARY KEY (time, sensor_id)
);

-- Comentarios
COMMENT ON TABLE iot.sensor_readings IS 'Time-series sensor readings - BIGINT IDs (migrated from UUID V22)';
COMMENT ON COLUMN iot.sensor_readings.greenhouse_id IS 'FK to metadata.greenhouses.id (BIGINT)';
COMMENT ON COLUMN iot.sensor_readings.tenant_id IS 'FK to metadata.tenants.id (BIGINT) - denormalized for query performance';

-- =============================================================================
-- FASE T5: Convertir a hypertable
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE T5: Converting to hypertable...'; END $$;

SELECT create_hypertable(
    'iot.sensor_readings',
    'time',
    chunk_time_interval => INTERVAL '7 days',
    if_not_exists => TRUE
);

-- =============================================================================
-- FASE T6: Migrar datos (si hay backup)
-- =============================================================================
DO $$
DECLARE
    v_backup_exists BOOLEAN;
    v_migrated_count BIGINT;
BEGIN
    RAISE NOTICE 'PHASE T6: Migrating data from backup...';

    -- Verificar si existe la tabla de backup
    SELECT EXISTS(
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'iot' AND table_name = 'sensor_readings_backup'
    ) INTO v_backup_exists;

    IF v_backup_exists THEN
        -- Migrar datos con mapeo de UUIDs a BIGINTs
        INSERT INTO iot.sensor_readings (time, sensor_id, greenhouse_id, tenant_id, sensor_type, value, unit)
        SELECT
            b.time,
            b.sensor_id,
            COALESCE(mg.new_id, 1) AS greenhouse_id,  -- Default a 1 si no hay mapeo
            COALESCE(mt.new_id, 1) AS tenant_id,      -- Default a 1 si no hay mapeo
            b.sensor_type,
            b.value,
            b.unit
        FROM iot.sensor_readings_backup b
        LEFT JOIN metadata.uuid_mapping_greenhouses mg ON b.old_greenhouse_id = mg.old_uuid
        LEFT JOIN metadata.uuid_mapping_tenants mt ON b.old_tenant_id = mt.old_uuid;

        GET DIAGNOSTICS v_migrated_count = ROW_COUNT;
        RAISE NOTICE 'Migrated % records', v_migrated_count;

        -- Eliminar backup
        DROP TABLE IF EXISTS iot.sensor_readings_backup;
        RAISE NOTICE 'Backup table dropped';
    ELSE
        RAISE NOTICE 'No backup table found - no data migration needed';
    END IF;
END $$;

-- =============================================================================
-- FASE T7: Configurar compresión y retención
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE T7: Configuring compression and retention...'; END $$;

-- Habilitar compresión
ALTER TABLE iot.sensor_readings SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'sensor_id, greenhouse_id',
    timescaledb.compress_orderby = 'time DESC'
);

-- Política de compresión (comprimir después de 7 días)
SELECT add_compression_policy('iot.sensor_readings', INTERVAL '7 days', if_not_exists => true);

-- Política de retención (mantener 2 años)
SELECT add_retention_policy('iot.sensor_readings', INTERVAL '2 years', if_not_exists => true);

-- =============================================================================
-- FASE T8: Crear índices optimizados
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE T8: Creating optimized indexes...'; END $$;

CREATE INDEX IF NOT EXISTS idx_sensor_readings_greenhouse_id
    ON iot.sensor_readings(greenhouse_id, time DESC);

CREATE INDEX IF NOT EXISTS idx_sensor_readings_tenant_time
    ON iot.sensor_readings(tenant_id, time DESC)
    WHERE tenant_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sensor_readings_greenhouse_sensor_time
    ON iot.sensor_readings(greenhouse_id, sensor_id, time DESC);

CREATE INDEX IF NOT EXISTS idx_sensor_readings_sensor_type
    ON iot.sensor_readings(sensor_type, time DESC);

CREATE INDEX IF NOT EXISTS idx_sensor_readings_tenant_greenhouse
    ON iot.sensor_readings(tenant_id, greenhouse_id, time DESC);

-- =============================================================================
-- FASE T9: Recrear continuous aggregates
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE T9: Recreating continuous aggregates...'; END $$;

-- Agregación horaria
CREATE MATERIALIZED VIEW iot.cagg_sensor_readings_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) AS bucket,
    greenhouse_id,
    tenant_id,
    sensor_type,
    MAX(unit) AS unit,
    AVG(value) AS avg_value,
    MIN(value) AS min_value,
    MAX(value) AS max_value,
    STDDEV(value) AS stddev_value,
    COUNT(*) AS count_readings,
    COUNT(*) FILTER (WHERE value IS NULL) AS null_count,
    MIN(time) AS first_reading_at,
    MAX(time) AS last_reading_at
FROM iot.sensor_readings
GROUP BY bucket, greenhouse_id, tenant_id, sensor_type
WITH NO DATA;

-- Agregación diaria
CREATE MATERIALIZED VIEW iot.cagg_sensor_readings_daily
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', time) AS bucket,
    greenhouse_id,
    tenant_id,
    sensor_type,
    MAX(unit) AS unit,
    AVG(value) AS avg_value,
    MIN(value) AS min_value,
    MAX(value) AS max_value,
    STDDEV(value) AS stddev_value,
    COUNT(*) AS count_readings,
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY value) AS median_value,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY value) AS p95_value,
    PERCENTILE_CONT(0.05) WITHIN GROUP (ORDER BY value) AS p5_value,
    COUNT(*) FILTER (WHERE value IS NULL) AS null_count,
    COUNT(DISTINCT time_bucket('1 hour', time))::SMALLINT AS hours_with_data,
    MIN(time) AS first_reading_at,
    MAX(time) AS last_reading_at
FROM iot.sensor_readings
GROUP BY bucket, greenhouse_id, tenant_id, sensor_type
WITH NO DATA;

-- Políticas de refresh
SELECT add_continuous_aggregate_policy('iot.cagg_sensor_readings_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => true);

SELECT add_continuous_aggregate_policy('iot.cagg_sensor_readings_daily',
    start_offset => INTERVAL '3 days',
    end_offset => INTERVAL '1 day',
    schedule_interval => INTERVAL '6 hours',
    if_not_exists => true);

-- =============================================================================
-- FASE T10: Verificación final TimescaleDB
-- =============================================================================
DO $$
DECLARE
    v_sensor_readings_count BIGINT;
    v_greenhouse_id_type TEXT;
    v_tenant_id_type TEXT;
    v_is_hypertable BOOLEAN;
BEGIN
    RAISE NOTICE '=== TIMESCALEDB MIGRATION VERIFICATION ===';

    -- Contar registros
    SELECT COUNT(*) INTO v_sensor_readings_count FROM iot.sensor_readings;
    RAISE NOTICE '  sensor_readings count: %', v_sensor_readings_count;

    -- Verificar tipos de datos
    SELECT data_type INTO v_greenhouse_id_type
    FROM information_schema.columns
    WHERE table_schema = 'iot' AND table_name = 'sensor_readings' AND column_name = 'greenhouse_id';

    SELECT data_type INTO v_tenant_id_type
    FROM information_schema.columns
    WHERE table_schema = 'iot' AND table_name = 'sensor_readings' AND column_name = 'tenant_id';

    RAISE NOTICE '  greenhouse_id type: %', v_greenhouse_id_type;
    RAISE NOTICE '  tenant_id type: %', v_tenant_id_type;

    -- Verificar que es hypertable
    SELECT EXISTS(
        SELECT 1 FROM timescaledb_information.hypertables
        WHERE hypertable_schema = 'iot' AND hypertable_name = 'sensor_readings'
    ) INTO v_is_hypertable;

    RAISE NOTICE '  is_hypertable: %', v_is_hypertable;

    -- Validaciones
    IF v_greenhouse_id_type != 'bigint' THEN
        RAISE EXCEPTION 'sensor_readings.greenhouse_id is not bigint! Got: %', v_greenhouse_id_type;
    END IF;

    IF v_tenant_id_type != 'bigint' THEN
        RAISE EXCEPTION 'sensor_readings.tenant_id is not bigint! Got: %', v_tenant_id_type;
    END IF;

    IF NOT v_is_hypertable THEN
        RAISE EXCEPTION 'sensor_readings is not a hypertable!';
    END IF;

    RAISE NOTICE '=== TIMESCALEDB MIGRATION COMPLETE ===';
    RAISE NOTICE 'Timestamp: %', NOW();
END $$;

-- =============================================================================
-- FIN DE MIGRACIÓN V22
-- =============================================================================
DO $$ BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE '=== V22 FULL MIGRATION COMPLETE ===';
    RAISE NOTICE 'All tables migrated to BIGINT:';
    RAISE NOTICE '  - metadata.tenants';
    RAISE NOTICE '  - metadata.users';
    RAISE NOTICE '  - metadata.greenhouses';
    RAISE NOTICE '  - metadata.sectors';
    RAISE NOTICE '  - metadata.devices';
    RAISE NOTICE '  - metadata.alerts';
    RAISE NOTICE '  - metadata.settings';
    RAISE NOTICE '  - metadata.command_history';
    RAISE NOTICE '  - iot.sensor_readings (TimescaleDB)';
    RAISE NOTICE 'EXCLUDED (not modified):';
    RAISE NOTICE '  - mqtt_users';
    RAISE NOTICE '  - mqtt_acl';
    RAISE NOTICE '========================================';
END $$;
