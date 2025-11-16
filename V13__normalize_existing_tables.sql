-- =====================================================
-- V13: NORMALIZE EXISTING TABLES (PostgreSQL)
-- =====================================================
-- Purpose: Add *_id columns to existing tables and migrate data from VARCHAR to SMALLINT
-- Impact: Prepares tables for future optimization (70% storage reduction after VARCHAR drop)
-- Target: PostgreSQL metadata database
-- Estimated execution time: DEV <10s, PROD 2-10 minutes (depends on data volume)
-- IMPORTANT: VARCHAR columns are kept for backward compatibility (DROP in future migration)
-- =====================================================

-- =====================================================
-- 1. SENSORS TABLE - Add normalized columns
-- =====================================================

ALTER TABLE metadata.sensors
    ADD COLUMN IF NOT EXISTS sensor_type_id SMALLINT,
    ADD COLUMN IF NOT EXISTS unit_id SMALLINT;

COMMENT ON COLUMN metadata.sensors.sensor_type_id IS
'Normalized sensor type (references sensor_types.id). Replaces sensor_type VARCHAR.';

COMMENT ON COLUMN metadata.sensors.unit_id IS
'Normalized unit (references units.id). Replaces unit VARCHAR.';

-- =====================================================
-- 2. SENSORS - Migrate data from VARCHAR to *_id
-- =====================================================

-- Migrate sensor_type → sensor_type_id
UPDATE metadata.sensors s
SET sensor_type_id = st.id
FROM metadata.sensor_types st
WHERE s.sensor_type_id IS NULL
  AND UPPER(TRIM(s.sensor_type)) = st.name;

-- Log sensors without matching type
DO $$
DECLARE
    v_unmatched INT;
BEGIN
    SELECT COUNT(*) INTO v_unmatched
    FROM metadata.sensors
    WHERE sensor_type_id IS NULL AND sensor_type IS NOT NULL;

    IF v_unmatched > 0 THEN
        RAISE WARNING 'Found % sensors with unmatched sensor_type. Creating SENSOR type as fallback.', v_unmatched;

        -- Assign default 'SENSOR' type to unmatched
        UPDATE metadata.sensors
        SET sensor_type_id = (SELECT id FROM metadata.sensor_types WHERE name = 'SENSOR')
        WHERE sensor_type_id IS NULL AND sensor_type IS NOT NULL;
    END IF;
END $$;

-- Migrate unit → unit_id
UPDATE metadata.sensors s
SET unit_id = u.id
FROM metadata.units u
WHERE s.unit_id IS NULL
  AND TRIM(s.unit) = u.symbol;

-- Log sensors without matching unit
DO $$
DECLARE
    v_unmatched INT;
BEGIN
    SELECT COUNT(*) INTO v_unmatched
    FROM metadata.sensors
    WHERE unit_id IS NULL AND unit IS NOT NULL;

    IF v_unmatched > 0 THEN
        RAISE WARNING 'Found % sensors with unmatched unit. Assigning default "unit".', v_unmatched;

        UPDATE metadata.sensors
        SET unit_id = (SELECT id FROM metadata.units WHERE symbol = 'unit')
        WHERE unit_id IS NULL AND unit IS NOT NULL;
    END IF;
END $$;

-- Add foreign key constraints
ALTER TABLE metadata.sensors
    ADD CONSTRAINT fk_sensors_sensor_type
        FOREIGN KEY (sensor_type_id) REFERENCES metadata.sensor_types(id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_sensors_unit
        FOREIGN KEY (unit_id) REFERENCES metadata.units(id)
        ON DELETE RESTRICT;

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_sensors_sensor_type_id
    ON metadata.sensors(sensor_type_id) WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_sensors_unit_id
    ON metadata.sensors(unit_id);

-- =====================================================
-- 3. ACTUATORS TABLE - Add normalized columns
-- =====================================================

ALTER TABLE metadata.actuators
    ADD COLUMN IF NOT EXISTS actuator_type_id SMALLINT,
    ADD COLUMN IF NOT EXISTS unit_id SMALLINT,
    ADD COLUMN IF NOT EXISTS state_id SMALLINT;

COMMENT ON COLUMN metadata.actuators.actuator_type_id IS
'Normalized actuator type (references actuator_types.id). Replaces actuator_type VARCHAR.';

COMMENT ON COLUMN metadata.actuators.unit_id IS
'Normalized unit (references units.id). Replaces unit VARCHAR.';

COMMENT ON COLUMN metadata.actuators.state_id IS
'Normalized state (references actuator_states.id). Replaces current_state VARCHAR.';

-- =====================================================
-- 4. ACTUATORS - Migrate data from VARCHAR to *_id
-- =====================================================

-- Migrate actuator_type → actuator_type_id
UPDATE metadata.actuators a
SET actuator_type_id = at.id
FROM metadata.actuator_types at
WHERE a.actuator_type_id IS NULL
  AND UPPER(TRIM(a.actuator_type)) = at.name;

-- Fallback for unmatched actuator types
DO $$
DECLARE
    v_unmatched INT;
BEGIN
    SELECT COUNT(*) INTO v_unmatched
    FROM metadata.actuators
    WHERE actuator_type_id IS NULL AND actuator_type IS NOT NULL;

    IF v_unmatched > 0 THEN
        RAISE WARNING 'Found % actuators with unmatched actuator_type. Creating RELAY type as fallback.', v_unmatched;

        UPDATE metadata.actuators
        SET actuator_type_id = (SELECT id FROM metadata.actuator_types WHERE name = 'RELAY')
        WHERE actuator_type_id IS NULL AND actuator_type IS NOT NULL;
    END IF;
END $$;

-- Migrate unit → unit_id
UPDATE metadata.actuators a
SET unit_id = u.id
FROM metadata.units u
WHERE a.unit_id IS NULL
  AND TRIM(a.unit) = u.symbol;

-- Fallback for unmatched units
UPDATE metadata.actuators
SET unit_id = (SELECT id FROM metadata.units WHERE symbol = 'unit')
WHERE unit_id IS NULL AND unit IS NOT NULL;

-- Migrate current_state → state_id
UPDATE metadata.actuators a
SET state_id = s.id
FROM metadata.actuator_states s
WHERE a.state_id IS NULL
  AND UPPER(TRIM(a.current_state)) = s.name;

-- Fallback for unmatched states (default to OFF)
UPDATE metadata.actuators
SET state_id = (SELECT id FROM metadata.actuator_states WHERE name = 'OFF')
WHERE state_id IS NULL;

-- Add foreign key constraints
ALTER TABLE metadata.actuators
    ADD CONSTRAINT fk_actuators_actuator_type
        FOREIGN KEY (actuator_type_id) REFERENCES metadata.actuator_types(id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_actuators_unit
        FOREIGN KEY (unit_id) REFERENCES metadata.units(id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_actuators_state
        FOREIGN KEY (state_id) REFERENCES metadata.actuator_states(id)
        ON DELETE RESTRICT;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_actuators_actuator_type_id
    ON metadata.actuators(actuator_type_id) WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_actuators_state_id
    ON metadata.actuators(state_id);

-- =====================================================
-- 5. ALERTS TABLE - Add normalized columns
-- =====================================================

ALTER TABLE metadata.alerts
    ADD COLUMN IF NOT EXISTS alert_type_id SMALLINT,
    ADD COLUMN IF NOT EXISTS severity_id SMALLINT;

COMMENT ON COLUMN metadata.alerts.alert_type_id IS
'Normalized alert type (references alert_types.id). Replaces alert_type VARCHAR.';

COMMENT ON COLUMN metadata.alerts.severity_id IS
'Normalized severity (references alert_severities.id). Replaces severity VARCHAR.';

-- =====================================================
-- 6. ALERTS - Migrate data from VARCHAR to *_id
-- =====================================================

-- Migrate alert_type → alert_type_id
-- NOTE: alerts.alert_type might have custom values, so we only migrate known types
UPDATE metadata.alerts a
SET alert_type_id = at.id
FROM metadata.alert_types at
WHERE a.alert_type_id IS NULL
  AND a.alert_type = at.name;

-- Log unmapped alert_types (these will need manual mapping or new entries in alert_types)
DO $$
DECLARE
    v_unmapped_count INT;
    v_unmapped_types TEXT;
BEGIN
    SELECT COUNT(DISTINCT alert_type), STRING_AGG(DISTINCT alert_type, ', ')
    INTO v_unmapped_count, v_unmapped_types
    FROM metadata.alerts
    WHERE alert_type_id IS NULL AND alert_type IS NOT NULL;

    IF v_unmapped_count > 0 THEN
        RAISE NOTICE 'Found % unmapped alert_types: %', v_unmapped_count, v_unmapped_types;
        RAISE NOTICE 'These can be mapped manually or added to metadata.alert_types';
    END IF;
END $$;

-- Migrate severity → severity_id
UPDATE metadata.alerts a
SET severity_id = s.id
FROM metadata.alert_severities s
WHERE a.severity_id IS NULL
  AND UPPER(TRIM(a.severity)) = s.name;

-- Fallback for unmatched severities (default to WARNING)
UPDATE metadata.alerts
SET severity_id = (SELECT id FROM metadata.alert_severities WHERE name = 'WARNING')
WHERE severity_id IS NULL AND severity IS NOT NULL;

-- Add foreign key constraints
ALTER TABLE metadata.alerts
    ADD CONSTRAINT fk_alerts_alert_type
        FOREIGN KEY (alert_type_id) REFERENCES metadata.alert_types(id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_alerts_severity
        FOREIGN KEY (severity_id) REFERENCES metadata.alert_severities(id)
        ON DELETE RESTRICT;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_alerts_alert_type_id
    ON metadata.alerts(alert_type_id);

CREATE INDEX IF NOT EXISTS idx_alerts_severity_id
    ON metadata.alerts(severity_id) WHERE is_resolved = FALSE;

-- Composite index for common query pattern
CREATE INDEX IF NOT EXISTS idx_alerts_greenhouse_severity_status
    ON metadata.alerts(greenhouse_id, severity_id, is_resolved, created_at DESC);

-- =====================================================
-- 7. CREATE VIEWS FOR BACKWARD COMPATIBILITY
-- =====================================================
-- These views provide the old schema with VARCHAR columns filled from normalized data
-- Useful during transition period where application code hasn't been updated yet

CREATE OR REPLACE VIEW metadata.v_sensors_denormalized AS
SELECT
    s.*,
    st.name AS sensor_type_denorm,
    u.symbol AS unit_denorm
FROM metadata.sensors s
LEFT JOIN metadata.sensor_types st ON s.sensor_type_id = st.id
LEFT JOIN metadata.units u ON s.unit_id = u.id;

COMMENT ON VIEW metadata.v_sensors_denormalized IS
'Backward compatibility view. Shows sensors with denormalized sensor_type and unit names.';

CREATE OR REPLACE VIEW metadata.v_actuators_denormalized AS
SELECT
    a.*,
    at.name AS actuator_type_denorm,
    u.symbol AS unit_denorm,
    s.name AS current_state_denorm
FROM metadata.actuators a
LEFT JOIN metadata.actuator_types at ON a.actuator_type_id = at.id
LEFT JOIN metadata.units u ON a.unit_id = u.id
LEFT JOIN metadata.actuator_states s ON a.state_id = s.id;

COMMENT ON VIEW metadata.v_actuators_denormalized IS
'Backward compatibility view. Shows actuators with denormalized type, unit, and state names.';

CREATE OR REPLACE VIEW metadata.v_alerts_denormalized AS
SELECT
    a.*,
    at.name AS alert_type_denorm,
    s.name AS severity_denorm,
    s.level AS severity_level,
    s.color AS severity_color
FROM metadata.alerts a
LEFT JOIN metadata.alert_types at ON a.alert_type_id = at.id
LEFT JOIN metadata.alert_severities s ON a.severity_id = s.id;

COMMENT ON VIEW metadata.v_alerts_denormalized IS
'Backward compatibility view. Shows alerts with denormalized type and severity names.';

-- =====================================================
-- 8. STATISTICS AND VALIDATION
-- =====================================================

DO $$
DECLARE
    v_sensors_total INT;
    v_sensors_normalized INT;
    v_actuators_total INT;
    v_actuators_normalized INT;
    v_alerts_total INT;
    v_alerts_normalized INT;
BEGIN
    -- Sensors statistics
    SELECT COUNT(*), COUNT(sensor_type_id)
    INTO v_sensors_total, v_sensors_normalized
    FROM metadata.sensors;

    -- Actuators statistics
    SELECT COUNT(*), COUNT(actuator_type_id)
    INTO v_actuators_total, v_actuators_normalized
    FROM metadata.actuators;

    -- Alerts statistics
    SELECT COUNT(*), COUNT(alert_type_id)
    INTO v_alerts_total, v_alerts_normalized
    FROM metadata.alerts;

    RAISE NOTICE '================================================================';
    RAISE NOTICE 'V13: NORMALIZATION COMPLETED';
    RAISE NOTICE '================================================================';
    RAISE NOTICE 'Sensors: %/% normalized (%.1f%%)',
        v_sensors_normalized, v_sensors_total,
        (v_sensors_normalized::NUMERIC / NULLIF(v_sensors_total, 0) * 100);
    RAISE NOTICE 'Actuators: %/% normalized (%.1f%%)',
        v_actuators_normalized, v_actuators_total,
        (v_actuators_normalized::NUMERIC / NULLIF(v_actuators_total, 0) * 100);
    RAISE NOTICE 'Alerts: %/% normalized (%.1f%%)',
        v_alerts_normalized, v_alerts_total,
        (v_alerts_normalized::NUMERIC / NULLIF(v_alerts_total, 0) * 100);
    RAISE NOTICE '';
    RAISE NOTICE 'Backward compatibility views created:';
    RAISE NOTICE '  - metadata.v_sensors_denormalized';
    RAISE NOTICE '  - metadata.v_actuators_denormalized';
    RAISE NOTICE '  - metadata.v_alerts_denormalized';
    RAISE NOTICE '';
    RAISE NOTICE 'Next steps:';
    RAISE NOTICE '  1. Update application code to use *_id columns';
    RAISE NOTICE '  2. Test with denormalized views for transition period';
    RAISE NOTICE '  3. After stabilization, DROP VARCHAR columns in future migration';
    RAISE NOTICE '================================================================';
END $$;
