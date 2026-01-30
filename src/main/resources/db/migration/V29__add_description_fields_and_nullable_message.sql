-- =============================================================================
-- V29: Añadir campos description y hacer message nullable
-- Fecha: 2026-01-30
--
-- CAMBIOS:
--   1. alerts.message: NOT NULL -> NULLABLE
--   2. alerts.description: NUEVO campo TEXT NULLABLE
--   3. settings.description: NUEVO campo VARCHAR(500) NULLABLE
--
-- NOTA: settings.value ya es NULLABLE, no requiere cambios.
-- =============================================================================

-- Registrar inicio de migracion
DO $$ BEGIN RAISE NOTICE '=== V29 MIGRATION START: Add description fields ==='; END $$;
DO $$ BEGIN RAISE NOTICE 'Timestamp: %', NOW(); END $$;

-- =============================================================================
-- FASE 1: ALERTS - Hacer message NULLABLE
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 1: Making alerts.message NULLABLE...'; END $$;

-- -----------------------------------------------------------------------------
-- 1.1 Eliminar constraint NOT NULL de message
-- -----------------------------------------------------------------------------
-- Cambiamos message de obligatorio a opcional.
-- Esto permite crear alertas sin mensaje inicial.
ALTER TABLE metadata.alerts
    ALTER COLUMN message DROP NOT NULL;

DO $$ BEGIN RAISE NOTICE 'alerts.message is now NULLABLE'; END $$;

-- =============================================================================
-- FASE 2: ALERTS - Añadir campo description
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 2: Adding alerts.description field...'; END $$;

-- -----------------------------------------------------------------------------
-- 2.1 Añadir columna description
-- -----------------------------------------------------------------------------
-- Campo para descripción detallada de la alerta.
-- Separado de message para permitir título corto (message) y descripción larga.
-- TEXT permite almacenar descripciones extensas sin límite práctico.
ALTER TABLE metadata.alerts
    ADD COLUMN IF NOT EXISTS description TEXT;

DO $$ BEGIN RAISE NOTICE 'alerts.description added (TEXT, NULLABLE)'; END $$;

-- =============================================================================
-- FASE 3: SETTINGS - Añadir campo description
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 3: Adding settings.description field...'; END $$;

-- -----------------------------------------------------------------------------
-- 3.1 Añadir columna description
-- -----------------------------------------------------------------------------
-- Campo para descripción de la configuración.
-- VARCHAR(500) es suficiente para descripciones de configuraciones.
ALTER TABLE metadata.settings
    ADD COLUMN IF NOT EXISTS description VARCHAR(500);

DO $$ BEGIN RAISE NOTICE 'settings.description added (VARCHAR 500, NULLABLE)'; END $$;

-- =============================================================================
-- FASE 4: VERIFICACION FINAL
-- =============================================================================
DO $$
DECLARE
    v_alerts_message_nullable BOOLEAN;
    v_alerts_description_exists BOOLEAN;
    v_settings_description_exists BOOLEAN;
BEGIN
    -- Verificar que alerts.message es nullable
    SELECT is_nullable = 'YES' INTO v_alerts_message_nullable
    FROM information_schema.columns
    WHERE table_schema = 'metadata'
      AND table_name = 'alerts'
      AND column_name = 'message';

    -- Verificar que alerts.description existe
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'metadata'
          AND table_name = 'alerts'
          AND column_name = 'description'
    ) INTO v_alerts_description_exists;

    -- Verificar que settings.description existe
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'metadata'
          AND table_name = 'settings'
          AND column_name = 'description'
    ) INTO v_settings_description_exists;

    RAISE NOTICE '=== V29 MIGRATION COMPLETE ===';
    RAISE NOTICE 'alerts.message NULLABLE: %', v_alerts_message_nullable;
    RAISE NOTICE 'alerts.description exists: %', v_alerts_description_exists;
    RAISE NOTICE 'settings.description exists: %', v_settings_description_exists;

    -- Validaciones
    IF NOT v_alerts_message_nullable THEN
        RAISE EXCEPTION 'ERROR: alerts.message no es NULLABLE!';
    END IF;
    IF NOT v_alerts_description_exists THEN
        RAISE EXCEPTION 'ERROR: alerts.description no existe!';
    END IF;
    IF NOT v_settings_description_exists THEN
        RAISE EXCEPTION 'ERROR: settings.description no existe!';
    END IF;

    RAISE NOTICE 'Todas las validaciones pasaron correctamente';
    RAISE NOTICE 'Timestamp: %', NOW();
END $$;

-- =============================================================================
-- RESUMEN DE CAMBIOS:
--
-- ALERTS:
--   - message: NOT NULL -> NULLABLE (ahora opcional)
--   - description: NUEVO campo TEXT NULLABLE
--
-- SETTINGS:
--   - description: NUEVO campo VARCHAR(500) NULLABLE
--
-- NOTA: settings.value ya era NULLABLE, no se modificó.
-- =============================================================================
