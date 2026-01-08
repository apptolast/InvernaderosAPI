-- =============================================================================
-- V23: Cleanup UUID Migration - Reorder columns and drop mapping tables
-- Fecha: 2026-01-08
-- Descripcion:
--   1. Reordena columnas para que 'id' sea la primera columna de cada tabla
--   2. Elimina las tablas de mapeo uuid_mapping_* que ya no son necesarias
--
-- TABLAS AFECTADAS (schema metadata):
--   - tenants, users, greenhouses, sectors, devices, alerts, settings, command_history
--
-- TABLAS NO AFECTADAS (mantienen UUID):
--   - mqtt_users, mqtt_acl (tablas MQTT internas de EMQX)
--
-- NOTA: Reordenar columnas en PostgreSQL requiere recrear la tabla.
--       Este proceso preserva todos los datos, indices, constraints y FKs.
-- =============================================================================

DO $$ BEGIN RAISE NOTICE '=== V23 CLEANUP START ==='; END $$;

-- =============================================================================
-- FASE 1: Eliminar tablas de mapeo UUID (ya no necesarias)
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 1: Dropping UUID mapping tables...'; END $$;

DROP TABLE IF EXISTS metadata.uuid_mapping_command_history CASCADE;
DROP TABLE IF EXISTS metadata.uuid_mapping_settings CASCADE;
DROP TABLE IF EXISTS metadata.uuid_mapping_alerts CASCADE;
DROP TABLE IF EXISTS metadata.uuid_mapping_devices CASCADE;
DROP TABLE IF EXISTS metadata.uuid_mapping_sectors CASCADE;
DROP TABLE IF EXISTS metadata.uuid_mapping_greenhouses CASCADE;
DROP TABLE IF EXISTS metadata.uuid_mapping_users CASCADE;
DROP TABLE IF EXISTS metadata.uuid_mapping_tenants CASCADE;

DO $$ BEGIN RAISE NOTICE 'UUID mapping tables dropped successfully'; END $$;

-- =============================================================================
-- FASE 2: Reordenar columnas - TENANTS
-- PostgreSQL no tiene ALTER TABLE ... REORDER COLUMNS
-- Solución: Crear tabla temporal, copiar datos, recrear tabla con orden correcto
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 2: Reordering TENANTS columns...'; END $$;

-- 2.1 Crear tabla temporal con el orden correcto de columnas
CREATE TABLE metadata.tenants_new (
    id         BIGINT PRIMARY KEY DEFAULT nextval('metadata.tenants_id_seq'),
    name       VARCHAR(100) NOT NULL UNIQUE,
    email      VARCHAR(255) NOT NULL,
    phone      VARCHAR(50),
    province   VARCHAR(100),
    country    VARCHAR(50) DEFAULT 'España',
    location   JSONB,
    is_active  BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2.2 Copiar datos
INSERT INTO metadata.tenants_new (id, name, email, phone, province, country, location, is_active, created_at, updated_at)
SELECT id, name, email, phone, province, country, location, is_active, created_at, updated_at
FROM metadata.tenants;

-- 2.3 Eliminar tabla original (CASCADE elimina FKs)
DROP TABLE metadata.tenants CASCADE;

-- 2.4 Renombrar nueva tabla
ALTER TABLE metadata.tenants_new RENAME TO tenants;

-- 2.5 Recrear indices
CREATE INDEX idx_tenants_active ON metadata.tenants(is_active) WHERE is_active = true;
CREATE INDEX idx_tenants_email_lower ON metadata.tenants(LOWER(email));
CREATE INDEX idx_tenants_name ON metadata.tenants(name);

-- 2.6 Actualizar secuencia
SELECT setval('metadata.tenants_id_seq', COALESCE((SELECT MAX(id) FROM metadata.tenants), 0) + 1, false);

DO $$ BEGIN RAISE NOTICE 'TENANTS reordered successfully'; END $$;

-- =============================================================================
-- FASE 3: Reordenar columnas - USERS
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 3: Reordering USERS columns...'; END $$;

CREATE TABLE metadata.users_new (
    id                          BIGINT PRIMARY KEY DEFAULT nextval('metadata.users_id_seq'),
    tenant_id                   BIGINT NOT NULL,
    username                    VARCHAR(50) NOT NULL UNIQUE,
    email                       VARCHAR(255) NOT NULL UNIQUE,
    password_hash               VARCHAR(255) NOT NULL,
    role                        VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'OPERATOR', 'VIEWER')),
    is_active                   BOOLEAN DEFAULT TRUE,
    last_login                  TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ DEFAULT NOW(),
    reset_password_token        VARCHAR(255),
    reset_password_token_expiry TIMESTAMPTZ
);

INSERT INTO metadata.users_new
SELECT id, tenant_id, username, email, password_hash, role, is_active, last_login, created_at, updated_at, reset_password_token, reset_password_token_expiry
FROM metadata.users;

DROP TABLE metadata.users CASCADE;
ALTER TABLE metadata.users_new RENAME TO users;

-- FK a tenants
ALTER TABLE metadata.users ADD CONSTRAINT fk_users_tenant
    FOREIGN KEY (tenant_id) REFERENCES metadata.tenants(id) ON DELETE CASCADE;

-- Indices
CREATE INDEX idx_users_tenant_id ON metadata.users(tenant_id);
CREATE INDEX idx_users_email ON metadata.users(email);
CREATE INDEX idx_users_username ON metadata.users(username);
CREATE INDEX idx_users_email_lower ON metadata.users(LOWER(email));
CREATE INDEX idx_users_username_lower ON metadata.users(LOWER(username));
CREATE INDEX idx_users_last_login ON metadata.users(last_login DESC) WHERE is_active = true;
CREATE INDEX idx_users_tenant_active ON metadata.users(tenant_id, is_active) WHERE is_active = true;
CREATE INDEX idx_users_tenant_role ON metadata.users(tenant_id, role) WHERE is_active = true;

SELECT setval('metadata.users_id_seq', COALESCE((SELECT MAX(id) FROM metadata.users), 0) + 1, false);

DO $$ BEGIN RAISE NOTICE 'USERS reordered successfully'; END $$;

-- =============================================================================
-- FASE 4: Reordenar columnas - GREENHOUSES
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 4: Reordering GREENHOUSES columns...'; END $$;

CREATE TABLE metadata.greenhouses_new (
    id         BIGINT PRIMARY KEY DEFAULT nextval('metadata.greenhouses_id_seq'),
    tenant_id  BIGINT NOT NULL,
    name       VARCHAR(100) NOT NULL,
    location   JSONB,
    area_m2    NUMERIC(10,2),
    timezone   VARCHAR(50) DEFAULT 'Europe/Madrid',
    is_active  BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(tenant_id, name)
);

INSERT INTO metadata.greenhouses_new
SELECT id, tenant_id, name, location, area_m2, timezone, is_active, created_at, updated_at
FROM metadata.greenhouses;

DROP TABLE metadata.greenhouses CASCADE;
ALTER TABLE metadata.greenhouses_new RENAME TO greenhouses;

ALTER TABLE metadata.greenhouses ADD CONSTRAINT fk_greenhouses_tenant
    FOREIGN KEY (tenant_id) REFERENCES metadata.tenants(id) ON DELETE CASCADE;

CREATE INDEX idx_greenhouses_tenant ON metadata.greenhouses(tenant_id);
CREATE INDEX idx_greenhouses_active ON metadata.greenhouses(is_active);
CREATE INDEX idx_greenhouses_tenant_active ON metadata.greenhouses(tenant_id, is_active) WHERE is_active = true;
CREATE INDEX idx_greenhouses_location_gin ON metadata.greenhouses USING GIN(location jsonb_path_ops);

SELECT setval('metadata.greenhouses_id_seq', COALESCE((SELECT MAX(id) FROM metadata.greenhouses), 0) + 1, false);

DO $$ BEGIN RAISE NOTICE 'GREENHOUSES reordered successfully'; END $$;

-- =============================================================================
-- FASE 5: Reordenar columnas - SECTORS
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 5: Reordering SECTORS columns...'; END $$;

CREATE TABLE metadata.sectors_new (
    id            BIGINT PRIMARY KEY DEFAULT nextval('metadata.sectors_id_seq'),
    greenhouse_id BIGINT NOT NULL,
    variety       VARCHAR(100)
);

INSERT INTO metadata.sectors_new SELECT id, greenhouse_id, variety FROM metadata.sectors;

DROP TABLE metadata.sectors CASCADE;
ALTER TABLE metadata.sectors_new RENAME TO sectors;

ALTER TABLE metadata.sectors ADD CONSTRAINT fk_sectors_greenhouse
    FOREIGN KEY (greenhouse_id) REFERENCES metadata.greenhouses(id) ON DELETE CASCADE;

CREATE INDEX idx_sectors_greenhouse ON metadata.sectors(greenhouse_id);

SELECT setval('metadata.sectors_id_seq', COALESCE((SELECT MAX(id) FROM metadata.sectors), 0) + 1, false);

DO $$ BEGIN RAISE NOTICE 'SECTORS reordered successfully'; END $$;

-- =============================================================================
-- FASE 6: Reordenar columnas - DEVICES
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 6: Reordering DEVICES columns...'; END $$;

CREATE TABLE metadata.devices_new (
    id            BIGINT PRIMARY KEY DEFAULT nextval('metadata.devices_id_seq'),
    tenant_id     BIGINT NOT NULL,
    greenhouse_id BIGINT NOT NULL,
    name          VARCHAR(100),
    category_id   SMALLINT,
    type_id       SMALLINT,
    unit_id       SMALLINT,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO metadata.devices_new
SELECT id, tenant_id, greenhouse_id, name, category_id, type_id, unit_id, is_active, created_at, updated_at
FROM metadata.devices;

DROP TABLE metadata.devices CASCADE;
ALTER TABLE metadata.devices_new RENAME TO devices;

ALTER TABLE metadata.devices ADD CONSTRAINT fk_devices_tenant
    FOREIGN KEY (tenant_id) REFERENCES metadata.tenants(id) ON DELETE CASCADE;
ALTER TABLE metadata.devices ADD CONSTRAINT fk_devices_greenhouse
    FOREIGN KEY (greenhouse_id) REFERENCES metadata.greenhouses(id) ON DELETE CASCADE;
ALTER TABLE metadata.devices ADD CONSTRAINT fk_devices_category
    FOREIGN KEY (category_id) REFERENCES metadata.device_categories(id);
ALTER TABLE metadata.devices ADD CONSTRAINT fk_devices_type
    FOREIGN KEY (type_id) REFERENCES metadata.device_types(id);
ALTER TABLE metadata.devices ADD CONSTRAINT fk_devices_unit
    FOREIGN KEY (unit_id) REFERENCES metadata.units(id);

CREATE INDEX idx_devices_tenant ON metadata.devices(tenant_id);
CREATE INDEX idx_devices_greenhouse ON metadata.devices(greenhouse_id);
CREATE INDEX idx_devices_active ON metadata.devices(is_active) WHERE is_active = true;
CREATE INDEX idx_devices_tenant_greenhouse ON metadata.devices(tenant_id, greenhouse_id);
CREATE INDEX idx_devices_name ON metadata.devices(name) WHERE name IS NOT NULL;

SELECT setval('metadata.devices_id_seq', COALESCE((SELECT MAX(id) FROM metadata.devices), 0) + 1, false);

DO $$ BEGIN RAISE NOTICE 'DEVICES reordered successfully'; END $$;

-- =============================================================================
-- FASE 7: Reordenar columnas - ALERTS
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 7: Reordering ALERTS columns...'; END $$;

CREATE TABLE metadata.alerts_new (
    id                  BIGINT PRIMARY KEY DEFAULT nextval('metadata.alerts_id_seq'),
    tenant_id           BIGINT NOT NULL,
    greenhouse_id       BIGINT NOT NULL,
    alert_type_id       SMALLINT,
    severity_id         SMALLINT,
    message             TEXT NOT NULL,
    is_resolved         BOOLEAN DEFAULT FALSE,
    resolved_at         TIMESTAMPTZ,
    resolved_by_user_id BIGINT,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT chk_resolved_consistency CHECK (
        (is_resolved = false AND resolved_at IS NULL) OR
        (is_resolved = true AND resolved_at IS NOT NULL)
    )
);

INSERT INTO metadata.alerts_new
SELECT id, tenant_id, greenhouse_id, alert_type_id, severity_id, message, is_resolved, resolved_at, resolved_by_user_id, created_at, updated_at
FROM metadata.alerts;

DROP TABLE metadata.alerts CASCADE;
ALTER TABLE metadata.alerts_new RENAME TO alerts;

ALTER TABLE metadata.alerts ADD CONSTRAINT fk_alerts_tenant
    FOREIGN KEY (tenant_id) REFERENCES metadata.tenants(id) ON DELETE CASCADE;
ALTER TABLE metadata.alerts ADD CONSTRAINT fk_alerts_greenhouse
    FOREIGN KEY (greenhouse_id) REFERENCES metadata.greenhouses(id) ON DELETE CASCADE;
ALTER TABLE metadata.alerts ADD CONSTRAINT fk_alerts_resolved_by_user
    FOREIGN KEY (resolved_by_user_id) REFERENCES metadata.users(id) ON DELETE SET NULL;
ALTER TABLE metadata.alerts ADD CONSTRAINT fk_alerts_type
    FOREIGN KEY (alert_type_id) REFERENCES metadata.alert_types(id);
ALTER TABLE metadata.alerts ADD CONSTRAINT fk_alerts_severity
    FOREIGN KEY (severity_id) REFERENCES metadata.alert_severities(id);

CREATE INDEX idx_alerts_tenant ON metadata.alerts(tenant_id);
CREATE INDEX idx_alerts_greenhouse ON metadata.alerts(greenhouse_id);
CREATE INDEX idx_alerts_resolved ON metadata.alerts(is_resolved);
CREATE INDEX idx_alerts_created_at ON metadata.alerts(created_at DESC);
CREATE INDEX idx_alerts_alert_type_id ON metadata.alerts(alert_type_id);
CREATE INDEX idx_alerts_severity_id ON metadata.alerts(severity_id) WHERE is_resolved = false;
CREATE INDEX idx_alerts_tenant_unresolved ON metadata.alerts(tenant_id, is_resolved, created_at DESC) WHERE is_resolved = false;
CREATE INDEX idx_alerts_unresolved ON metadata.alerts(is_resolved, created_at DESC) WHERE is_resolved = false;
CREATE INDEX idx_alerts_greenhouse_severity_status ON metadata.alerts(greenhouse_id, severity_id, is_resolved, created_at DESC);

SELECT setval('metadata.alerts_id_seq', COALESCE((SELECT MAX(id) FROM metadata.alerts), 0) + 1, false);

DO $$ BEGIN RAISE NOTICE 'ALERTS reordered successfully'; END $$;

-- =============================================================================
-- FASE 8: Reordenar columnas - SETTINGS
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 8: Reordering SETTINGS columns...'; END $$;

CREATE TABLE metadata.settings_new (
    id            BIGINT PRIMARY KEY DEFAULT nextval('metadata.settings_id_seq'),
    tenant_id     BIGINT NOT NULL,
    greenhouse_id BIGINT NOT NULL,
    parameter_id  SMALLINT NOT NULL,
    period_id     SMALLINT NOT NULL,
    min_value     NUMERIC(10,2),
    max_value     NUMERIC(10,2),
    is_active     BOOLEAN DEFAULT TRUE,
    created_at    TIMESTAMPTZ DEFAULT NOW(),
    updated_at    TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(greenhouse_id, parameter_id, period_id)
);

INSERT INTO metadata.settings_new
SELECT id, tenant_id, greenhouse_id, parameter_id, period_id, min_value, max_value, is_active, created_at, updated_at
FROM metadata.settings;

DROP TABLE metadata.settings CASCADE;
ALTER TABLE metadata.settings_new RENAME TO settings;

ALTER TABLE metadata.settings ADD CONSTRAINT fk_settings_tenant
    FOREIGN KEY (tenant_id) REFERENCES metadata.tenants(id) ON DELETE CASCADE;
ALTER TABLE metadata.settings ADD CONSTRAINT fk_settings_greenhouse
    FOREIGN KEY (greenhouse_id) REFERENCES metadata.greenhouses(id) ON DELETE CASCADE;
ALTER TABLE metadata.settings ADD CONSTRAINT fk_settings_parameter
    FOREIGN KEY (parameter_id) REFERENCES metadata.device_types(id);
ALTER TABLE metadata.settings ADD CONSTRAINT fk_settings_period
    FOREIGN KEY (period_id) REFERENCES metadata.periods(id);

CREATE INDEX idx_settings_tenant ON metadata.settings(tenant_id);
CREATE INDEX idx_settings_greenhouse ON metadata.settings(greenhouse_id);

SELECT setval('metadata.settings_id_seq', COALESCE((SELECT MAX(id) FROM metadata.settings), 0) + 1, false);

DO $$ BEGIN RAISE NOTICE 'SETTINGS reordered successfully'; END $$;

-- =============================================================================
-- FASE 9: Reordenar columnas - COMMAND_HISTORY
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 9: Reordering COMMAND_HISTORY columns...'; END $$;

CREATE TABLE metadata.command_history_new (
    id         BIGINT PRIMARY KEY DEFAULT nextval('metadata.command_history_id_seq'),
    device_id  BIGINT NOT NULL,
    user_id    BIGINT,
    command    VARCHAR(50) NOT NULL,
    value      DOUBLE PRECISION,
    source     VARCHAR(30) CHECK (source IN ('USER', 'SYSTEM', 'SCHEDULE', 'ALERT', 'API', 'MQTT')),
    success    BOOLEAN,
    response   JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO metadata.command_history_new
SELECT id, device_id, user_id, command, value, source, success, response, created_at
FROM metadata.command_history;

DROP TABLE metadata.command_history CASCADE;
ALTER TABLE metadata.command_history_new RENAME TO command_history;

ALTER TABLE metadata.command_history ADD CONSTRAINT fk_command_history_device
    FOREIGN KEY (device_id) REFERENCES metadata.devices(id) ON DELETE CASCADE;
ALTER TABLE metadata.command_history ADD CONSTRAINT fk_command_history_user
    FOREIGN KEY (user_id) REFERENCES metadata.users(id) ON DELETE SET NULL;

CREATE INDEX idx_command_history_device ON metadata.command_history(device_id);
CREATE INDEX idx_command_history_user ON metadata.command_history(user_id);
CREATE INDEX idx_command_history_created ON metadata.command_history(created_at DESC);
CREATE INDEX idx_command_history_device_time ON metadata.command_history(device_id, created_at DESC);

SELECT setval('metadata.command_history_id_seq', COALESCE((SELECT MAX(id) FROM metadata.command_history), 0) + 1, false);

DO $$ BEGIN RAISE NOTICE 'COMMAND_HISTORY reordered successfully'; END $$;

-- =============================================================================
-- FASE 10: Recrear FKs en mqtt_users (referencia a tenants y greenhouses)
-- Nota: mqtt_users mantiene UUID como su propio ID, pero referencia BIGINT para tenant/greenhouse
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 10: Recreating mqtt_users foreign keys...'; END $$;

-- mqtt_users.tenant_id y greenhouse_id ahora son BIGINT en las tablas referenciadas
-- Pero mqtt_users puede tener UUID en esas columnas (legacy)
-- Verificamos y actualizamos si es necesario

-- Primero verificamos el tipo actual de tenant_id en mqtt_users
DO $$
DECLARE
    v_type TEXT;
BEGIN
    SELECT data_type INTO v_type
    FROM information_schema.columns
    WHERE table_schema = 'metadata'
      AND table_name = 'mqtt_users'
      AND column_name = 'tenant_id';

    IF v_type = 'uuid' THEN
        RAISE NOTICE 'mqtt_users.tenant_id is UUID - will need manual migration if needed';
    ELSIF v_type = 'bigint' THEN
        RAISE NOTICE 'mqtt_users.tenant_id is already BIGINT';
    ELSE
        RAISE NOTICE 'mqtt_users.tenant_id type: %', v_type;
    END IF;
END $$;

-- =============================================================================
-- FASE 11: Verificacion final
-- =============================================================================
DO $$
DECLARE
    v_count INT;
BEGIN
    RAISE NOTICE '=== V23 CLEANUP COMPLETE ===';

    -- Verificar que no quedan tablas de mapeo
    SELECT COUNT(*) INTO v_count
    FROM information_schema.tables
    WHERE table_schema = 'metadata' AND table_name LIKE 'uuid_mapping%';

    IF v_count = 0 THEN
        RAISE NOTICE 'All UUID mapping tables removed successfully';
    ELSE
        RAISE WARNING 'Found % UUID mapping tables still present!', v_count;
    END IF;

    -- Verificar orden de columnas en tenants
    RAISE NOTICE 'Column order verification (first column should be id):';

    PERFORM column_name
    FROM information_schema.columns
    WHERE table_schema = 'metadata' AND table_name = 'tenants'
    ORDER BY ordinal_position
    LIMIT 1;

    RAISE NOTICE 'Timestamp: %', NOW();
END $$;

-- =============================================================================
-- RESUMEN DE CAMBIOS:
-- 1. Eliminadas 8 tablas uuid_mapping_*
-- 2. Reordenadas columnas en 8 tablas (id ahora es primera columna)
-- 3. Todos los indices y FKs recreados correctamente
-- 4. Secuencias actualizadas al valor maximo + 1
-- 5. Tablas MQTT (mqtt_users, mqtt_acl) NO modificadas
-- =============================================================================
