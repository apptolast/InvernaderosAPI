-- =============================================================================
-- V28: Cambiar greenhouse_id a sector_id en alerts y settings
-- Fecha: 2026-01-30
--
-- OBJETIVO:
--   Tanto alerts como settings pertenecerán a un SECTOR en lugar de un GREENHOUSE.
--   Esto alinea la estructura con el modelo lógico donde los dispositivos,
--   alertas y configuraciones pertenecen a sectores específicos.
--
-- CAMBIOS:
--   1. alerts: greenhouse_id (FK greenhouses) → sector_id (FK sectors)
--   2. settings: greenhouse_id (FK greenhouses) → sector_id (FK sectors)
--
-- MIGRACIÓN DE DATOS:
--   Los datos existentes se migran buscando el sector que pertenece al
--   greenhouse actual. Como cada greenhouse tiene al menos un sector,
--   se asigna el primer sector encontrado.
-- =============================================================================

-- Registrar inicio de migracion
DO $$ BEGIN RAISE NOTICE '=== V28 MIGRATION START: greenhouse_id -> sector_id ==='; END $$;
DO $$ BEGIN RAISE NOTICE 'Timestamp: %', NOW(); END $$;

-- =============================================================================
-- FASE 1: ALERTS - Cambiar greenhouse_id -> sector_id
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 1: Migrating ALERTS table...'; END $$;

-- -----------------------------------------------------------------------------
-- 1.1 Anadir nueva columna sector_id (permite NULL temporalmente)
-- -----------------------------------------------------------------------------
-- Anadimos la columna que reemplazara a greenhouse_id.
-- La dejamos NULL temporalmente para poder poblarla antes de hacerla NOT NULL.
ALTER TABLE metadata.alerts
    ADD COLUMN IF NOT EXISTS sector_id BIGINT;

-- -----------------------------------------------------------------------------
-- 1.2 Migrar datos: greenhouse_id -> sector_id
-- -----------------------------------------------------------------------------
-- Para cada alerta, buscamos el sector que pertenece a su greenhouse actual.
-- Usamos una subconsulta correlacionada que obtiene el primer sector del greenhouse.
-- LIMIT 1 garantiza que si hay multiples sectores, tomamos el primero.
UPDATE metadata.alerts a
SET sector_id = (
    SELECT s.id
    FROM metadata.sectors s
    WHERE s.greenhouse_id = a.greenhouse_id
    LIMIT 1
)
WHERE a.sector_id IS NULL;

-- Verificar que todos los registros tienen sector_id
DO $$
DECLARE
    v_null_count INT;
BEGIN
    SELECT COUNT(*) INTO v_null_count
    FROM metadata.alerts
    WHERE sector_id IS NULL;

    IF v_null_count > 0 THEN
        RAISE EXCEPTION 'ERROR: % alerts no tienen sector_id asignado. Verifica que todos los greenhouses tengan sectores.', v_null_count;
    END IF;

    RAISE NOTICE 'Alerts: Todos los registros migrados correctamente a sector_id';
END $$;

-- -----------------------------------------------------------------------------
-- 1.3 Eliminar FK antigua a greenhouses
-- -----------------------------------------------------------------------------
-- Antes de eliminar la columna greenhouse_id, debemos eliminar su constraint FK.
-- El nombre viene de V23: fk_alerts_greenhouse
ALTER TABLE metadata.alerts
    DROP CONSTRAINT IF EXISTS fk_alerts_greenhouse;

-- -----------------------------------------------------------------------------
-- 1.4 Eliminar indices que usan greenhouse_id
-- -----------------------------------------------------------------------------
-- Estos indices ya no seran validos porque la columna desaparecera.
-- Los recrearemos con sector_id despues.
DROP INDEX IF EXISTS metadata.idx_alerts_greenhouse;
DROP INDEX IF EXISTS metadata.idx_alerts_greenhouse_severity_status;

-- -----------------------------------------------------------------------------
-- 1.5 Eliminar columna greenhouse_id
-- -----------------------------------------------------------------------------
-- Ya no la necesitamos porque ahora usamos sector_id.
ALTER TABLE metadata.alerts
    DROP COLUMN IF EXISTS greenhouse_id;

-- -----------------------------------------------------------------------------
-- 1.6 Hacer sector_id NOT NULL
-- -----------------------------------------------------------------------------
-- Ahora que todos los registros tienen valor, podemos hacer la columna obligatoria.
ALTER TABLE metadata.alerts
    ALTER COLUMN sector_id SET NOT NULL;

-- -----------------------------------------------------------------------------
-- 1.7 Anadir FK a sectors
-- -----------------------------------------------------------------------------
-- Creamos la relacion de integridad referencial con la tabla sectors.
-- ON DELETE CASCADE: si se elimina un sector, sus alertas tambien se eliminan.
ALTER TABLE metadata.alerts
    ADD CONSTRAINT fk_alerts_sector
    FOREIGN KEY (sector_id) REFERENCES metadata.sectors(id)
    ON DELETE CASCADE;

-- -----------------------------------------------------------------------------
-- 1.8 Crear nuevos indices con sector_id
-- -----------------------------------------------------------------------------
-- idx_alerts_sector: Busqueda rapida de alertas por sector
CREATE INDEX idx_alerts_sector ON metadata.alerts(sector_id);

-- idx_alerts_sector_severity_status: Busqueda de alertas por sector, severidad y estado
-- Util para dashboards que muestran alertas activas por sector
CREATE INDEX idx_alerts_sector_severity_status
    ON metadata.alerts(sector_id, severity_id, is_resolved, created_at DESC);

DO $$ BEGIN RAISE NOTICE 'PHASE 1 COMPLETE: ALERTS migrated to sector_id'; END $$;

-- =============================================================================
-- FASE 2: SETTINGS - Cambiar greenhouse_id -> sector_id
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 2: Migrating SETTINGS table...'; END $$;

-- -----------------------------------------------------------------------------
-- 2.1 Eliminar constraint UNIQUE antiguo
-- -----------------------------------------------------------------------------
-- El constraint actual es UNIQUE(greenhouse_id, parameter_id, actuator_state_id)
-- Lo eliminamos porque vamos a cambiar greenhouse_id por sector_id.
-- El nombre viene de V26: uq_settings_greenhouse_parameter_actuator_state
ALTER TABLE metadata.settings
    DROP CONSTRAINT IF EXISTS uq_settings_greenhouse_parameter_actuator_state;

-- Tambien puede existir con nombre del V23 (por si acaso)
ALTER TABLE metadata.settings
    DROP CONSTRAINT IF EXISTS settings_greenhouse_id_parameter_id_period_id_key;

-- -----------------------------------------------------------------------------
-- 2.2 Anadir nueva columna sector_id (permite NULL temporalmente)
-- -----------------------------------------------------------------------------
ALTER TABLE metadata.settings
    ADD COLUMN IF NOT EXISTS sector_id BIGINT;

-- -----------------------------------------------------------------------------
-- 2.3 Migrar datos: greenhouse_id -> sector_id
-- -----------------------------------------------------------------------------
-- Misma logica que alerts: buscamos el sector que pertenece al greenhouse.
UPDATE metadata.settings s
SET sector_id = (
    SELECT sec.id
    FROM metadata.sectors sec
    WHERE sec.greenhouse_id = s.greenhouse_id
    LIMIT 1
)
WHERE s.sector_id IS NULL;

-- Verificar que todos los registros tienen sector_id
DO $$
DECLARE
    v_null_count INT;
BEGIN
    SELECT COUNT(*) INTO v_null_count
    FROM metadata.settings
    WHERE sector_id IS NULL;

    IF v_null_count > 0 THEN
        RAISE EXCEPTION 'ERROR: % settings no tienen sector_id asignado. Verifica que todos los greenhouses tengan sectores.', v_null_count;
    END IF;

    RAISE NOTICE 'Settings: Todos los registros migrados correctamente a sector_id';
END $$;

-- -----------------------------------------------------------------------------
-- 2.4 Eliminar FK antigua a greenhouses
-- -----------------------------------------------------------------------------
-- El nombre viene de V23: fk_settings_greenhouse
ALTER TABLE metadata.settings
    DROP CONSTRAINT IF EXISTS fk_settings_greenhouse;

-- -----------------------------------------------------------------------------
-- 2.5 Eliminar indices que usan greenhouse_id
-- -----------------------------------------------------------------------------
DROP INDEX IF EXISTS metadata.idx_settings_greenhouse;

-- -----------------------------------------------------------------------------
-- 2.6 Eliminar columna greenhouse_id
-- -----------------------------------------------------------------------------
ALTER TABLE metadata.settings
    DROP COLUMN IF EXISTS greenhouse_id;

-- -----------------------------------------------------------------------------
-- 2.7 Hacer sector_id NOT NULL
-- -----------------------------------------------------------------------------
ALTER TABLE metadata.settings
    ALTER COLUMN sector_id SET NOT NULL;

-- -----------------------------------------------------------------------------
-- 2.8 Anadir FK a sectors
-- -----------------------------------------------------------------------------
ALTER TABLE metadata.settings
    ADD CONSTRAINT fk_settings_sector
    FOREIGN KEY (sector_id) REFERENCES metadata.sectors(id)
    ON DELETE CASCADE;

-- -----------------------------------------------------------------------------
-- 2.9 Crear nuevo constraint UNIQUE
-- -----------------------------------------------------------------------------
-- La combinacion (sector_id, parameter_id, actuator_state_id) debe ser unica.
-- Esto evita configuraciones duplicadas para el mismo parametro en el mismo sector.
ALTER TABLE metadata.settings
    ADD CONSTRAINT uq_settings_sector_parameter_actuator_state
    UNIQUE (sector_id, parameter_id, actuator_state_id);

-- -----------------------------------------------------------------------------
-- 2.10 Crear nuevos indices con sector_id
-- -----------------------------------------------------------------------------
-- idx_settings_sector: Busqueda rapida de configuraciones por sector
CREATE INDEX idx_settings_sector ON metadata.settings(sector_id);

DO $$ BEGIN RAISE NOTICE 'PHASE 2 COMPLETE: SETTINGS migrated to sector_id'; END $$;

-- =============================================================================
-- FASE 3: VERIFICACION FINAL
-- =============================================================================
DO $$
DECLARE
    v_alerts_count INT;
    v_settings_count INT;
    v_alerts_sector_col BOOLEAN;
    v_settings_sector_col BOOLEAN;
    v_alerts_greenhouse_col BOOLEAN;
    v_settings_greenhouse_col BOOLEAN;
BEGIN
    -- Contar registros
    SELECT COUNT(*) INTO v_alerts_count FROM metadata.alerts;
    SELECT COUNT(*) INTO v_settings_count FROM metadata.settings;

    -- Verificar que sector_id existe
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'metadata' AND table_name = 'alerts' AND column_name = 'sector_id'
    ) INTO v_alerts_sector_col;

    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'metadata' AND table_name = 'settings' AND column_name = 'sector_id'
    ) INTO v_settings_sector_col;

    -- Verificar que greenhouse_id NO existe
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'metadata' AND table_name = 'alerts' AND column_name = 'greenhouse_id'
    ) INTO v_alerts_greenhouse_col;

    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'metadata' AND table_name = 'settings' AND column_name = 'greenhouse_id'
    ) INTO v_settings_greenhouse_col;

    RAISE NOTICE '=== V28 MIGRATION COMPLETE ===';
    RAISE NOTICE 'Alerts: % registros, sector_id: %, greenhouse_id: %',
        v_alerts_count, v_alerts_sector_col, v_alerts_greenhouse_col;
    RAISE NOTICE 'Settings: % registros, sector_id: %, greenhouse_id: %',
        v_settings_count, v_settings_sector_col, v_settings_greenhouse_col;

    -- Validaciones finales
    IF NOT v_alerts_sector_col THEN
        RAISE EXCEPTION 'ERROR: alerts.sector_id no existe!';
    END IF;
    IF NOT v_settings_sector_col THEN
        RAISE EXCEPTION 'ERROR: settings.sector_id no existe!';
    END IF;
    IF v_alerts_greenhouse_col THEN
        RAISE EXCEPTION 'ERROR: alerts.greenhouse_id todavia existe!';
    END IF;
    IF v_settings_greenhouse_col THEN
        RAISE EXCEPTION 'ERROR: settings.greenhouse_id todavia existe!';
    END IF;

    RAISE NOTICE 'Todas las validaciones pasaron correctamente';
    RAISE NOTICE 'Timestamp: %', NOW();
END $$;

-- =============================================================================
-- RESUMEN DE CAMBIOS:
--
-- ALERTS:
--   - ELIMINADO: greenhouse_id BIGINT (FK a greenhouses)
--   - ANADIDO:   sector_id BIGINT NOT NULL (FK a sectors)
--   - INDICES:   idx_alerts_sector, idx_alerts_sector_severity_status
--
-- SETTINGS:
--   - ELIMINADO: greenhouse_id BIGINT (FK a greenhouses)
--   - ANADIDO:   sector_id BIGINT NOT NULL (FK a sectors)
--   - CONSTRAINT: UNIQUE(sector_id, parameter_id, actuator_state_id)
--   - INDICES:   idx_settings_sector
--
-- DATOS MIGRADOS:
--   - Cada registro se asigno al sector correspondiente a su greenhouse original
-- =============================================================================
