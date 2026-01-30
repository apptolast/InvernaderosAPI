-- =============================================================================
-- V26: Settings - Reemplazar Period por ActuatorState y min/max por value
-- Fecha: 2026-01-15
--
-- Esta migracion:
-- 1. Elimina period_id, min_value, max_value de settings
-- 2. Anade actuator_state_id (FK a actuator_states)
-- 3. Anade value (un solo campo numerico)
-- 4. Actualiza constraints e indices
-- =============================================================================

DO $$ BEGIN RAISE NOTICE '=== V26 MIGRATION START: Settings - Period to ActuatorState ==='; END $$;
DO $$ BEGIN RAISE NOTICE 'Timestamp: %', NOW(); END $$;

-- =============================================================================
-- FASE 1: Anadir nuevas columnas
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 1: Adding new columns...'; END $$;

-- Anadir columna actuator_state_id
ALTER TABLE metadata.settings
    ADD COLUMN IF NOT EXISTS actuator_state_id SMALLINT;

-- Anadir columna value
ALTER TABLE metadata.settings
    ADD COLUMN IF NOT EXISTS value NUMERIC(10,2);

-- =============================================================================
-- FASE 2: Eliminar constraint UNIQUE antigua
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 2: Dropping old UNIQUE constraint...'; END $$;

ALTER TABLE metadata.settings
    DROP CONSTRAINT IF EXISTS uq_setting_greenhouse_parameter_period;

-- Tambien eliminar por si tiene otro nombre
ALTER TABLE metadata.settings
    DROP CONSTRAINT IF EXISTS uq_settings_greenhouse_parameter_period;

-- =============================================================================
-- FASE 3: Eliminar columnas antiguas
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 3: Dropping old columns...'; END $$;

ALTER TABLE metadata.settings
    DROP COLUMN IF EXISTS period_id;

ALTER TABLE metadata.settings
    DROP COLUMN IF EXISTS min_value;

ALTER TABLE metadata.settings
    DROP COLUMN IF EXISTS max_value;

-- =============================================================================
-- FASE 4: Anadir FK a actuator_states
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 4: Adding FK to actuator_states...'; END $$;

ALTER TABLE metadata.settings
    ADD CONSTRAINT fk_settings_actuator_state
    FOREIGN KEY (actuator_state_id)
    REFERENCES metadata.actuator_states(id)
    ON DELETE SET NULL;

-- =============================================================================
-- FASE 5: Anadir nuevo constraint UNIQUE
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 5: Adding new UNIQUE constraint...'; END $$;

ALTER TABLE metadata.settings
    ADD CONSTRAINT uq_settings_greenhouse_parameter_actuator_state
    UNIQUE (greenhouse_id, parameter_id, actuator_state_id);

-- =============================================================================
-- FASE 6: Anadir indice para actuator_state_id
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 6: Creating index for actuator_state_id...'; END $$;

CREATE INDEX IF NOT EXISTS idx_settings_actuator_state
    ON metadata.settings(actuator_state_id);

-- =============================================================================
-- FASE 7: Verificacion final
-- =============================================================================
DO $$
DECLARE
    v_settings_count INT;
    v_columns TEXT;
BEGIN
    SELECT COUNT(*) INTO v_settings_count FROM metadata.settings;

    SELECT string_agg(column_name, ', ' ORDER BY ordinal_position)
    INTO v_columns
    FROM information_schema.columns
    WHERE table_schema = 'metadata'
      AND table_name = 'settings';

    RAISE NOTICE '=== V26 MIGRATION COMPLETE ===';
    RAISE NOTICE 'Settings count: %', v_settings_count;
    RAISE NOTICE 'New columns: %', v_columns;
    RAISE NOTICE 'Removed: period_id, min_value, max_value';
    RAISE NOTICE 'Added: actuator_state_id, value';
    RAISE NOTICE 'New UNIQUE constraint: (greenhouse_id, parameter_id, actuator_state_id)';
    RAISE NOTICE 'Timestamp: %', NOW();
END $$;
