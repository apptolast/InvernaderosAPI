-- =====================================================
-- V10: Create alerts table for greenhouse monitoring
-- =====================================================
-- Description: Alerts system for sensor threshold violations,
--              actuator failures, and system events
-- Author: Claude Code
-- Date: 2025-11-16
-- =====================================================

-- Create alerts table
CREATE TABLE IF NOT EXISTS metadata.alerts (
    -- Primary key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Foreign keys
    greenhouse_id UUID NOT NULL,
    sensor_id UUID,  -- NULL for system/actuator alerts
    tenant_id UUID NOT NULL,  -- Denormalized for multi-tenant queries

    -- Alert information
    alert_type VARCHAR(50) NOT NULL,  -- THRESHOLD_EXCEEDED, SENSOR_OFFLINE, ACTUATOR_FAILURE, SYSTEM_ERROR
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',  -- INFO, WARNING, ERROR, CRITICAL
    message TEXT NOT NULL,
    alert_data JSONB,  -- Additional context: threshold values, sensor readings, etc.

    -- Resolution tracking
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by UUID,  -- User ID who resolved the alert

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Foreign key constraints
    CONSTRAINT fk_alerts_greenhouse
        FOREIGN KEY (greenhouse_id)
        REFERENCES metadata.greenhouses(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_alerts_sensor
        FOREIGN KEY (sensor_id)
        REFERENCES metadata.sensors(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_alerts_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES metadata.tenants(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_alerts_resolved_by
        FOREIGN KEY (resolved_by)
        REFERENCES metadata.users(id)
        ON DELETE SET NULL,

    -- Check constraints
    CONSTRAINT chk_alert_severity
        CHECK (severity IN ('INFO', 'WARNING', 'ERROR', 'CRITICAL')),

    CONSTRAINT chk_resolved_consistency
        CHECK (
            (is_resolved = FALSE AND resolved_at IS NULL AND resolved_by IS NULL) OR
            (is_resolved = TRUE AND resolved_at IS NOT NULL)
        )
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_alerts_greenhouse
    ON metadata.alerts(greenhouse_id);

CREATE INDEX IF NOT EXISTS idx_alerts_sensor
    ON metadata.alerts(sensor_id)
    WHERE sensor_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_alerts_tenant
    ON metadata.alerts(tenant_id);

CREATE INDEX IF NOT EXISTS idx_alerts_unresolved
    ON metadata.alerts(is_resolved, created_at DESC)
    WHERE is_resolved = FALSE;

CREATE INDEX IF NOT EXISTS idx_alerts_severity
    ON metadata.alerts(severity, created_at DESC)
    WHERE is_resolved = FALSE;

CREATE INDEX IF NOT EXISTS idx_alerts_type
    ON metadata.alerts(alert_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_alerts_created_at
    ON metadata.alerts(created_at DESC);

-- Create GIN index for JSONB alert_data queries
CREATE INDEX IF NOT EXISTS idx_alerts_data_gin
    ON metadata.alerts USING GIN (alert_data);

-- Comments
COMMENT ON TABLE metadata.alerts IS 'Alert system for greenhouse monitoring - threshold violations, sensor failures, actuator issues';
COMMENT ON COLUMN metadata.alerts.alert_type IS 'Type of alert: THRESHOLD_EXCEEDED, SENSOR_OFFLINE, ACTUATOR_FAILURE, SYSTEM_ERROR, etc.';
COMMENT ON COLUMN metadata.alerts.severity IS 'Alert severity level: INFO, WARNING, ERROR, CRITICAL';
COMMENT ON COLUMN metadata.alerts.alert_data IS 'Additional context as JSONB: {"threshold": 30, "current_value": 35, "sensor_type": "TEMPERATURE"}';
COMMENT ON COLUMN metadata.alerts.tenant_id IS 'Denormalized tenant_id for efficient multi-tenant filtering';
