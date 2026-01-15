-- =============================================================================
-- V27: Create Data Types Table and Modify Settings
-- Fecha: 2026-01-15
--
-- Esta migracion:
-- 1. Crea tabla data_types para definir tipos de datos basicos
-- 2. Inserta los 9 tipos de datos soportados
-- 3. Modifica settings.value de NUMERIC a VARCHAR para soportar todos los tipos
-- 4. Anade data_type_id FK a settings
-- =============================================================================

DO $$ BEGIN RAISE NOTICE '=== V27 MIGRATION START: Data Types Table ==='; END $$;
DO $$ BEGIN RAISE NOTICE 'Timestamp: %', NOW(); END $$;

-- =============================================================================
-- FASE 1: Crear tabla data_types
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 1: Creating data_types table...'; END $$;

CREATE TABLE IF NOT EXISTS metadata.data_types (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE,
    description TEXT,
    validation_regex VARCHAR(500),
    example_value VARCHAR(100),
    display_order SMALLINT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE metadata.data_types IS 'Catalogo de tipos de datos para configuraciones';
COMMENT ON COLUMN metadata.data_types.name IS 'Nombre del tipo de dato (INTEGER, BOOLEAN, STRING, etc.)';
COMMENT ON COLUMN metadata.data_types.validation_regex IS 'Expresion regular para validar valores de este tipo';
COMMENT ON COLUMN metadata.data_types.example_value IS 'Ejemplo de valor valido para este tipo';

-- =============================================================================
-- FASE 2: Insertar tipos de datos basicos
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 2: Inserting basic data types...'; END $$;

INSERT INTO metadata.data_types (name, description, validation_regex, example_value, display_order) VALUES
    ('INTEGER', 'Numero entero (32 bits)', '^-?\d+$', '25', 1),
    ('LONG', 'Numero entero grande (64 bits)', '^-?\d+$', '9223372036854775807', 2),
    ('DOUBLE', 'Numero decimal de precision doble', '^-?\d+(\.\d+)?$', '25.5', 3),
    ('BOOLEAN', 'Valor verdadero o falso', '^(true|false|TRUE|FALSE|1|0)$', 'true', 4),
    ('STRING', 'Cadena de texto', '.*', 'Invernadero Norte', 5),
    ('DATE', 'Fecha en formato ISO 8601', '^\d{4}-\d{2}-\d{2}$', '2026-01-15', 6),
    ('TIME', 'Hora en formato HH:mm:ss', '^\d{2}:\d{2}(:\d{2})?$', '14:30:00', 7),
    ('DATETIME', 'Fecha y hora en formato ISO 8601', '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?', '2026-01-15T14:30:00', 8),
    ('JSON', 'Objeto JSON valido', '^\{.*\}$|^\[.*\]$', '{"key": "value"}', 9)
ON CONFLICT (name) DO NOTHING;

-- =============================================================================
-- FASE 3: Crear indices
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 3: Creating indexes...'; END $$;

CREATE INDEX IF NOT EXISTS idx_data_types_name ON metadata.data_types(name);
CREATE INDEX IF NOT EXISTS idx_data_types_active ON metadata.data_types(is_active) WHERE is_active = TRUE;

-- =============================================================================
-- FASE 4: Modificar tabla settings
-- =============================================================================
DO $$ BEGIN RAISE NOTICE 'PHASE 4: Modifying settings table...'; END $$;

-- 4.1 Anadir columna data_type_id
ALTER TABLE metadata.settings
    ADD COLUMN IF NOT EXISTS data_type_id SMALLINT;

-- 4.2 Convertir value de NUMERIC a VARCHAR
-- Primero crear columna temporal
ALTER TABLE metadata.settings
    ADD COLUMN IF NOT EXISTS value_text VARCHAR(500);

-- Copiar valores existentes (si hay)
UPDATE metadata.settings
SET value_text = value::TEXT
WHERE value IS NOT NULL;

-- Eliminar columna value original
ALTER TABLE metadata.settings
    DROP COLUMN IF EXISTS value;

-- Renombrar columna temporal
ALTER TABLE metadata.settings
    RENAME COLUMN value_text TO value;

-- 4.3 Anadir FK a data_types
ALTER TABLE metadata.settings
    ADD CONSTRAINT fk_settings_data_type
    FOREIGN KEY (data_type_id)
    REFERENCES metadata.data_types(id)
    ON DELETE SET NULL;

-- 4.4 Crear indice
CREATE INDEX IF NOT EXISTS idx_settings_data_type ON metadata.settings(data_type_id);

-- =============================================================================
-- FASE 5: Verificacion final
-- =============================================================================
DO $$
DECLARE
    v_data_types_count INT;
    v_settings_count INT;
BEGIN
    SELECT COUNT(*) INTO v_data_types_count FROM metadata.data_types;
    SELECT COUNT(*) INTO v_settings_count FROM metadata.settings;

    RAISE NOTICE '=== V27 MIGRATION COMPLETE ===';
    RAISE NOTICE 'Data types created: %', v_data_types_count;
    RAISE NOTICE 'Settings count: %', v_settings_count;
    RAISE NOTICE 'Settings now supports: INTEGER, LONG, DOUBLE, BOOLEAN, STRING, DATE, TIME, DATETIME, JSON';
    RAISE NOTICE 'Timestamp: %', NOW();
END $$;
