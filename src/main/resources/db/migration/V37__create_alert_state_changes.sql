CREATE TABLE metadata.alert_state_changes (
    id            BIGINT       PRIMARY KEY DEFAULT metadata.generate_tsid(),
    alert_id      BIGINT       NOT NULL REFERENCES metadata.alerts(id) ON DELETE CASCADE,
    from_resolved BOOLEAN      NOT NULL,
    to_resolved   BOOLEAN      NOT NULL,
    source        VARCHAR(16)  NOT NULL CHECK (source IN ('MQTT', 'API', 'SYSTEM')),
    raw_value     VARCHAR(64),
    at            TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alert_state_changes_alert_at ON metadata.alert_state_changes(alert_id, at DESC);
CREATE INDEX idx_alert_state_changes_at ON metadata.alert_state_changes(at DESC);
CREATE INDEX idx_alert_state_changes_source ON metadata.alert_state_changes(source);

COMMENT ON TABLE metadata.alert_state_changes IS 'Audit trail of every transition of metadata.alerts.is_resolved. Source MQTT means the transition came from a GREENHOUSE/STATUS message with id starting with ALT-.';
