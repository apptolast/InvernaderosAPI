-- =====================================================
-- V14: CREATE STAGING TABLES (PostgreSQL)
-- =====================================================
-- Purpose: Create staging tables for bulk imports and audit trails
-- Impact: Enables safe bulk operations + complete audit history
-- Target: PostgreSQL metadata database
-- Estimated execution time: <5 seconds
-- =====================================================

-- =====================================================
-- 1. AUDIT LOG - Complete change history
-- =====================================================
-- Tracks ALL changes to critical tables (sensors, actuators, greenhouses, users, alerts)

CREATE TABLE IF NOT EXISTS metadata.audit_log (
    id BIGSERIAL PRIMARY KEY,

    -- What was changed
    table_name VARCHAR(100) NOT NULL,
    record_id UUID NOT NULL,
    operation VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),

    -- Complete before/after snapshots
    old_values JSONB,  -- NULL for INSERT
    new_values JSONB,  -- NULL for DELETE
    changed_fields TEXT[],  -- Array of field names that changed (for UPDATE)

    -- Who and when
    changed_by UUID REFERENCES metadata.users(id) ON DELETE SET NULL,
    changed_by_username VARCHAR(100),  -- Denormalized for historical record
    changed_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,

    -- Context
    change_reason TEXT,
    ip_address INET,
    user_agent TEXT,
    session_id VARCHAR(100),

    -- Metadata
    application_version VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE metadata.audit_log IS
'Complete audit trail of all changes to critical tables. Immutable after creation.';

COMMENT ON COLUMN metadata.audit_log.old_values IS
'Complete JSON snapshot of record BEFORE change (NULL for INSERT)';

COMMENT ON COLUMN metadata.audit_log.new_values IS
'Complete JSON snapshot of record AFTER change (NULL for DELETE)';

COMMENT ON COLUMN metadata.audit_log.changed_fields IS
'Array of field names that changed (helpful for filtering specific field changes)';

-- Indexes for common queries
CREATE INDEX idx_audit_log_table_record
    ON metadata.audit_log(table_name, record_id, changed_at DESC);

CREATE INDEX idx_audit_log_user
    ON metadata.audit_log(changed_by, changed_at DESC);

CREATE INDEX idx_audit_log_time
    ON metadata.audit_log(changed_at DESC);

CREATE INDEX idx_audit_log_operation
    ON metadata.audit_log(operation, table_name, changed_at DESC);

-- Index on changed_fields array (for finding changes to specific fields)
CREATE INDEX idx_audit_log_changed_fields
    ON metadata.audit_log USING GIN (changed_fields);

-- =====================================================
-- 2. ACTUATOR COMMAND HISTORY
-- =====================================================
-- Complete history of ALL commands sent to actuators

CREATE TABLE IF NOT EXISTS metadata.actuator_command_history (
    id BIGSERIAL PRIMARY KEY,
    actuator_id UUID NOT NULL REFERENCES metadata.actuators(id) ON DELETE CASCADE,
    greenhouse_id UUID NOT NULL REFERENCES metadata.greenhouses(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES metadata.tenants(id) ON DELETE CASCADE,

    -- Command details
    command VARCHAR(50) NOT NULL,  -- 'TURN_ON', 'TURN_OFF', 'SET_VALUE', 'SET_AUTO', 'SET_MANUAL'
    target_value DOUBLE PRECISION,
    target_state_id SMALLINT REFERENCES metadata.actuator_states(id),

    -- Previous state (for rollback)
    previous_state_id SMALLINT REFERENCES metadata.actuator_states(id),
    previous_value DOUBLE PRECISION,

    -- Result state
    new_state_id SMALLINT REFERENCES metadata.actuator_states(id),
    new_value DOUBLE PRECISION,

    -- Trigger source
    triggered_by VARCHAR(20) NOT NULL CHECK (triggered_by IN ('USER', 'AUTOMATION', 'SCHEDULE', 'ALERT', 'API', 'SYSTEM')),
    triggered_by_user_id UUID REFERENCES metadata.users(id) ON DELETE SET NULL,
    triggered_by_rule_id UUID,  -- References automation rule if triggered by automation

    -- Execution tracking
    command_sent_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    command_executed_at TIMESTAMPTZ,
    execution_status VARCHAR(20) DEFAULT 'PENDING' CHECK (execution_status IN ('PENDING', 'SENT', 'ACKNOWLEDGED', 'EXECUTED', 'FAILED', 'TIMEOUT')),
    error_message TEXT,

    -- Metadata
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE metadata.actuator_command_history IS
'Complete history of actuator commands. Critical for debugging, compliance, and analytics.';

COMMENT ON COLUMN metadata.actuator_command_history.triggered_by IS
'Source of command: USER (manual), AUTOMATION (rule-based), SCHEDULE (scheduled), ALERT (alert-triggered)';

-- Partition by month for better performance with large datasets
-- ALTER TABLE metadata.actuator_command_history
--     PARTITION BY RANGE (command_sent_at);

-- Indexes
CREATE INDEX idx_actuator_history_actuator
    ON metadata.actuator_command_history(actuator_id, command_sent_at DESC);

CREATE INDEX idx_actuator_history_greenhouse
    ON metadata.actuator_command_history(greenhouse_id, command_sent_at DESC);

CREATE INDEX idx_actuator_history_tenant
    ON metadata.actuator_command_history(tenant_id, command_sent_at DESC);

CREATE INDEX idx_actuator_history_user
    ON metadata.actuator_command_history(triggered_by_user_id, command_sent_at DESC)
    WHERE triggered_by = 'USER';

CREATE INDEX idx_actuator_history_status
    ON metadata.actuator_command_history(execution_status, command_sent_at DESC)
    WHERE execution_status IN ('PENDING', 'SENT', 'FAILED');

-- =====================================================
-- 3. SENSOR CONFIGURATION HISTORY
-- =====================================================
-- Track all calibration and configuration changes to sensors

CREATE TABLE IF NOT EXISTS metadata.sensor_configuration_history (
    id BIGSERIAL PRIMARY KEY,
    sensor_id UUID NOT NULL REFERENCES metadata.sensors(id) ON DELETE CASCADE,

    -- Previous configuration
    old_sensor_type_id SMALLINT,
    old_unit_id SMALLINT,
    old_min_threshold DECIMAL(10,2),
    old_max_threshold DECIMAL(10,2),
    old_calibration_data JSONB,
    old_mqtt_field_name VARCHAR(100),

    -- New configuration
    new_sensor_type_id SMALLINT,
    new_unit_id SMALLINT,
    new_min_threshold DECIMAL(10,2),
    new_max_threshold DECIMAL(10,2),
    new_calibration_data JSONB,
    new_mqtt_field_name VARCHAR(100),

    -- Audit
    changed_by UUID REFERENCES metadata.users(id) ON DELETE SET NULL,
    changed_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    change_reason TEXT,
    change_type VARCHAR(50),  -- 'CALIBRATION', 'THRESHOLD_UPDATE', 'TYPE_CHANGE', 'MQTT_CONFIG'

    created_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE metadata.sensor_configuration_history IS
'History of sensor configuration changes. Essential for tracking calibration drift and troubleshooting.';

-- Indexes
CREATE INDEX idx_sensor_config_history_sensor
    ON metadata.sensor_configuration_history(sensor_id, changed_at DESC);

CREATE INDEX idx_sensor_config_history_type
    ON metadata.sensor_configuration_history(change_type, changed_at DESC);

-- =====================================================
-- 4. ALERT RESOLUTION HISTORY
-- =====================================================
-- Track how alerts were resolved

CREATE TABLE IF NOT EXISTS metadata.alert_resolution_history (
    id BIGSERIAL PRIMARY KEY,
    alert_id UUID NOT NULL REFERENCES metadata.alerts(id) ON DELETE CASCADE,

    -- Previous state
    previous_status VARCHAR(20),
    previous_severity_id SMALLINT REFERENCES metadata.alert_severities(id),

    -- Resolution details
    resolved_by UUID NOT NULL REFERENCES metadata.users(id) ON DELETE CASCADE,
    resolved_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    resolution_action VARCHAR(50),  -- 'ACKNOWLEDGED', 'FALSE_POSITIVE', 'FIXED', 'IGNORED', 'ESCALATED'
    resolution_notes TEXT,

    -- Follow-up actions taken
    actions_taken TEXT[],  -- Array of actions: ['SENSOR_RECALIBRATED', 'ACTUATOR_REPAIRED', etc.]

    -- Performance metrics
    time_to_resolution INTERVAL,

    created_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE metadata.alert_resolution_history IS
'Tracks how alerts are resolved. Useful for response time analytics and alert effectiveness.';

-- Indexes
CREATE INDEX idx_alert_resolution_alert
    ON metadata.alert_resolution_history(alert_id, resolved_at DESC);

CREATE INDEX idx_alert_resolution_user
    ON metadata.alert_resolution_history(resolved_by, resolved_at DESC);

CREATE INDEX idx_alert_resolution_action
    ON metadata.alert_resolution_history(resolution_action);

-- =====================================================
-- 5. GREENHOUSE SNAPSHOT (Configuration backups)
-- =====================================================
-- Periodic snapshots of complete greenhouse configuration

CREATE TABLE IF NOT EXISTS metadata.greenhouse_snapshot (
    id BIGSERIAL PRIMARY KEY,
    greenhouse_id UUID NOT NULL REFERENCES metadata.greenhouses(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES metadata.tenants(id) ON DELETE CASCADE,

    snapshot_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    snapshot_type VARCHAR(20) DEFAULT 'SCHEDULED' CHECK (snapshot_type IN ('SCHEDULED', 'MANUAL', 'BEFORE_CHANGE', 'BACKUP')),

    -- Complete configuration snapshots (JSONB)
    greenhouse_config JSONB NOT NULL,
    sensors_config JSONB NOT NULL,
    actuators_config JSONB NOT NULL,
    active_alerts JSONB,
    automation_rules JSONB,

    -- Aggregated metrics for the snapshot period
    avg_temperature DECIMAL(5,2),
    avg_humidity DECIMAL(5,2),
    total_readings INT,
    total_alerts INT,
    total_commands INT,

    -- Metadata
    created_by UUID REFERENCES metadata.users(id) ON DELETE SET NULL,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE metadata.greenhouse_snapshot IS
'Periodic snapshots of greenhouse configuration. Enables restoration and temporal analysis.';

COMMENT ON COLUMN metadata.greenhouse_snapshot.greenhouse_config IS
'Complete greenhouse record as JSON';

COMMENT ON COLUMN metadata.greenhouse_snapshot.sensors_config IS
'Array of all sensor configurations at snapshot time';

-- Indexes
CREATE INDEX idx_greenhouse_snapshot_greenhouse
    ON metadata.greenhouse_snapshot(greenhouse_id, snapshot_at DESC);

CREATE INDEX idx_greenhouse_snapshot_tenant
    ON metadata.greenhouse_snapshot(tenant_id, snapshot_at DESC);

CREATE INDEX idx_greenhouse_snapshot_type
    ON metadata.greenhouse_snapshot(snapshot_type, snapshot_at DESC);

-- =====================================================
-- 6. BULK OPERATION LOG (For staging imports)
-- =====================================================
-- Tracks bulk import/update operations

CREATE TABLE IF NOT EXISTS metadata.bulk_operation_log (
    id BIGSERIAL PRIMARY KEY,

    operation_type VARCHAR(50) NOT NULL,  -- 'SENSOR_IMPORT', 'ACTUATOR_UPDATE', 'CALIBRATION_BATCH'
    target_table VARCHAR(100) NOT NULL,
    tenant_id UUID REFERENCES metadata.tenants(id) ON DELETE CASCADE,

    -- Execution tracking
    started_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    completed_at TIMESTAMPTZ,
    status VARCHAR(20) DEFAULT 'RUNNING' CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'PARTIAL', 'CANCELLED')),

    -- Statistics
    total_records INT NOT NULL DEFAULT 0,
    successful_records INT NOT NULL DEFAULT 0,
    failed_records INT NOT NULL DEFAULT 0,
    skipped_records INT NOT NULL DEFAULT 0,

    -- Error tracking
    error_summary JSONB,  -- {'error_type': count}
    error_details TEXT,

    -- Performance
    duration_seconds INT,
    records_per_second DECIMAL(10,2),

    -- Metadata
    triggered_by UUID REFERENCES metadata.users(id) ON DELETE SET NULL,
    source_file VARCHAR(255),
    metadata JSONB,

    created_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE metadata.bulk_operation_log IS
'Audit log for bulk operations (imports, batch updates, mass calibrations)';

-- Indexes
CREATE INDEX idx_bulk_operation_log_status
    ON metadata.bulk_operation_log(status, started_at DESC);

CREATE INDEX idx_bulk_operation_log_type
    ON metadata.bulk_operation_log(operation_type, started_at DESC);

CREATE INDEX idx_bulk_operation_log_tenant
    ON metadata.bulk_operation_log(tenant_id, started_at DESC);

-- =====================================================
-- 7. DATA QUALITY LOG
-- =====================================================
-- Track data quality issues and anomalies

CREATE TABLE IF NOT EXISTS metadata.data_quality_log (
    id BIGSERIAL PRIMARY KEY,

    data_source VARCHAR(50) NOT NULL,  -- 'SENSOR_READINGS', 'MQTT', 'API', 'MANUAL_ENTRY'
    quality_issue_type VARCHAR(50) NOT NULL,  -- 'OUT_OF_RANGE', 'MISSING_DATA', 'DUPLICATE', 'SPIKE', 'DRIFT'
    severity VARCHAR(20) CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),

    -- Affected entity
    greenhouse_id UUID REFERENCES metadata.greenhouses(id) ON DELETE CASCADE,
    sensor_id UUID REFERENCES metadata.sensors(id) ON DELETE CASCADE,
    tenant_id UUID REFERENCES metadata.tenants(id) ON DELETE CASCADE,

    -- Issue details
    detected_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    time_range_start TIMESTAMPTZ,
    time_range_end TIMESTAMPTZ,
    affected_records_count INT,

    -- Issue description
    description TEXT NOT NULL,
    sample_data JSONB,  -- Sample of problematic data

    -- Resolution
    status VARCHAR(20) DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'INVESTIGATING', 'RESOLVED', 'IGNORED')),
    resolved_at TIMESTAMPTZ,
    resolved_by UUID REFERENCES metadata.users(id),
    resolution_notes TEXT,

    created_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE metadata.data_quality_log IS
'Tracks data quality issues for monitoring and improvement';

-- Indexes
CREATE INDEX idx_data_quality_log_status
    ON metadata.data_quality_log(status, detected_at DESC);

CREATE INDEX idx_data_quality_log_greenhouse
    ON metadata.data_quality_log(greenhouse_id, detected_at DESC);

CREATE INDEX idx_data_quality_log_type
    ON metadata.data_quality_log(quality_issue_type, severity, detected_at DESC);

-- =====================================================
-- 8. GRANT PERMISSIONS
-- =====================================================

-- Grant read access to audit logs (read-only)
GRANT SELECT ON metadata.audit_log TO PUBLIC;
GRANT SELECT ON metadata.actuator_command_history TO PUBLIC;
GRANT SELECT ON metadata.sensor_configuration_history TO PUBLIC;
GRANT SELECT ON metadata.alert_resolution_history TO PUBLIC;
GRANT SELECT ON metadata.greenhouse_snapshot TO PUBLIC;
GRANT SELECT ON metadata.bulk_operation_log TO PUBLIC;
GRANT SELECT ON metadata.data_quality_log TO PUBLIC;

-- =====================================================
-- 9. RETENTION POLICIES (Optional - configure as needed)
-- =====================================================

-- Example: Delete audit_log entries older than 2 years
-- CREATE OR REPLACE FUNCTION metadata.cleanup_old_audit_logs()
-- RETURNS INTEGER AS $$
-- DECLARE
--     v_deleted INT;
-- BEGIN
--     DELETE FROM metadata.audit_log
--     WHERE changed_at < NOW() - INTERVAL '730 days';

--     GET DIAGNOSTICS v_deleted = ROW_COUNT;
--     RETURN v_deleted;
-- END;
-- $$ LANGUAGE plpgsql;

-- =====================================================
-- 10. VERIFICATION
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '================================================================';
    RAISE NOTICE 'V14: STAGING AND AUDIT TABLES CREATED SUCCESSFULLY';
    RAISE NOTICE '================================================================';
    RAISE NOTICE 'Tables created:';
    RAISE NOTICE '  - metadata.audit_log';
    RAISE NOTICE '  - metadata.actuator_command_history';
    RAISE NOTICE '  - metadata.sensor_configuration_history';
    RAISE NOTICE '  - metadata.alert_resolution_history';
    RAISE NOTICE '  - metadata.greenhouse_snapshot';
    RAISE NOTICE '  - metadata.bulk_operation_log';
    RAISE NOTICE '  - metadata.data_quality_log';
    RAISE NOTICE '';
    RAISE NOTICE 'Next steps:';
    RAISE NOTICE '  1. Configure triggers to populate audit_log automatically';
    RAISE NOTICE '  2. Update application to log actuator commands to history';
    RAISE NOTICE '  3. Set up scheduled snapshots (daily/weekly)';
    RAISE NOTICE '  4. Configure retention policies for old data';
    RAISE NOTICE '================================================================';
END $$;
