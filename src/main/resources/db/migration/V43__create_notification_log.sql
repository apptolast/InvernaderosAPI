-- =============================================================================
-- V43 — Notification log (append-only audit trail)
-- =============================================================================
-- Creates metadata.notification_log: one row per notification dispatch attempt,
-- written with its terminal status (SENT, FAILED, DROPPED_*, TOKEN_INVALIDATED).
-- PENDING is never persisted (all dispatch is synchronous within the async
-- executor; only the final outcome is recorded). Pure append-only, like
-- metadata.alert_state_changes.
--
-- id uses the TSID generator already defined in earlier migrations (V37 seed).
-- payload_json stores the full notification payload (title, body, data, channel)
-- as JSONB for ad-hoc queries and deduplication lookups.
--
-- Idempotent (uses IF NOT EXISTS) to make re-runs against the dev/prod
-- databases safe.
-- =============================================================================

CREATE TABLE IF NOT EXISTS metadata.notification_log (
    id                  BIGINT       PRIMARY KEY DEFAULT metadata.generate_tsid(),
    tenant_id           BIGINT       NOT NULL REFERENCES metadata.tenants(id) ON DELETE CASCADE,
    user_id             BIGINT       NOT NULL REFERENCES metadata.users(id)   ON DELETE CASCADE,
    device_token_id     BIGINT       NULL     REFERENCES metadata.push_tokens(id) ON DELETE SET NULL,
    notification_type   VARCHAR(32)  NOT NULL,
    -- Full rendered payload (title, body, data map, android channel).
    payload_json        JSONB        NOT NULL,
    status              VARCHAR(32)  NOT NULL CHECK (status IN (
                            'SENT',
                            'FAILED',
                            'DROPPED_BY_PREFERENCE',
                            'DROPPED_BY_QUIET_HOURS',
                            'DROPPED_BY_DEDUP',
                            'TOKEN_INVALIDATED'
                        )),
    fcm_message_id      VARCHAR(128) NULL,
    error               TEXT         NULL,
    sent_at             TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Index for the user-facing GET /api/v1/users/me/notifications endpoint (cursor pagination by TSID desc)
CREATE INDEX IF NOT EXISTS idx_notification_log_user_at
    ON metadata.notification_log(user_id, sent_at DESC);

-- Index for tenant-scoped admin queries
CREATE INDEX IF NOT EXISTS idx_notification_log_tenant_at
    ON metadata.notification_log(tenant_id, sent_at DESC);

-- Index for type-based analytics / scheduler idempotency checks
CREATE INDEX IF NOT EXISTS idx_notification_log_type_at
    ON metadata.notification_log(notification_type, sent_at DESC);

-- Partial index to speed up queries on non-successful outcomes (failures, drops, invalidations)
CREATE INDEX IF NOT EXISTS idx_notification_log_status
    ON metadata.notification_log(status)
 WHERE status != 'SENT';

COMMENT ON TABLE metadata.notification_log IS
    'Audit trail append-only de cada intento de envío de notificación. Paralelo a metadata.alert_state_changes.';
