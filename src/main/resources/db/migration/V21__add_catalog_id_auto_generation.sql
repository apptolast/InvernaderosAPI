-- =============================================================================
-- V21: Add Auto-Generation for Catalog Table IDs
-- Fecha: 2026-01-08
-- Descripcion: Añade secuencias y defaults para auto-generar IDs en tablas de
--              catálogo que fueron creadas sin GENERATED AS IDENTITY.
--              Esto permite que JPA use @GeneratedValue(strategy = IDENTITY)
-- =============================================================================

-- =============================================================================
-- 1. device_categories
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS metadata.device_categories_id_seq
    AS SMALLINT
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Establecer el valor actual de la secuencia al máximo ID existente + 1
SELECT setval('metadata.device_categories_id_seq',
    COALESCE((SELECT MAX(id) FROM metadata.device_categories), 0) + 1,
    false);

-- Establecer el default de la columna id
ALTER TABLE metadata.device_categories
    ALTER COLUMN id SET DEFAULT nextval('metadata.device_categories_id_seq');

-- Asociar la secuencia a la columna (para que se elimine si se elimina la tabla)
ALTER SEQUENCE metadata.device_categories_id_seq
    OWNED BY metadata.device_categories.id;

-- =============================================================================
-- 2. alert_types
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS metadata.alert_types_id_seq
    AS SMALLINT
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

SELECT setval('metadata.alert_types_id_seq',
    COALESCE((SELECT MAX(id) FROM metadata.alert_types), 0) + 1,
    false);

ALTER TABLE metadata.alert_types
    ALTER COLUMN id SET DEFAULT nextval('metadata.alert_types_id_seq');

ALTER SEQUENCE metadata.alert_types_id_seq
    OWNED BY metadata.alert_types.id;

-- =============================================================================
-- 3. alert_severities
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS metadata.alert_severities_id_seq
    AS SMALLINT
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

SELECT setval('metadata.alert_severities_id_seq',
    COALESCE((SELECT MAX(id) FROM metadata.alert_severities), 0) + 1,
    false);

ALTER TABLE metadata.alert_severities
    ALTER COLUMN id SET DEFAULT nextval('metadata.alert_severities_id_seq');

ALTER SEQUENCE metadata.alert_severities_id_seq
    OWNED BY metadata.alert_severities.id;

-- =============================================================================
-- 4. periods
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS metadata.periods_id_seq
    AS SMALLINT
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

SELECT setval('metadata.periods_id_seq',
    COALESCE((SELECT MAX(id) FROM metadata.periods), 0) + 1,
    false);

ALTER TABLE metadata.periods
    ALTER COLUMN id SET DEFAULT nextval('metadata.periods_id_seq');

ALTER SEQUENCE metadata.periods_id_seq
    OWNED BY metadata.periods.id;

-- =============================================================================
-- Verificación
-- =============================================================================
DO $$
DECLARE
    dc_default TEXT;
    at_default TEXT;
    as_default TEXT;
    p_default TEXT;
BEGIN
    SELECT column_default INTO dc_default
    FROM information_schema.columns
    WHERE table_schema = 'metadata' AND table_name = 'device_categories' AND column_name = 'id';

    SELECT column_default INTO at_default
    FROM information_schema.columns
    WHERE table_schema = 'metadata' AND table_name = 'alert_types' AND column_name = 'id';

    SELECT column_default INTO as_default
    FROM information_schema.columns
    WHERE table_schema = 'metadata' AND table_name = 'alert_severities' AND column_name = 'id';

    SELECT column_default INTO p_default
    FROM information_schema.columns
    WHERE table_schema = 'metadata' AND table_name = 'periods' AND column_name = 'id';

    RAISE NOTICE 'V21 Migration Results:';
    RAISE NOTICE '  device_categories.id default: %', dc_default;
    RAISE NOTICE '  alert_types.id default: %', at_default;
    RAISE NOTICE '  alert_severities.id default: %', as_default;
    RAISE NOTICE '  periods.id default: %', p_default;
END $$;
