-- =============================================================================
-- V38 — FCM push notification support
-- =============================================================================
-- 1) New table metadata.push_tokens: stores Firebase Cloud Messaging device
--    tokens registered by mobile/web clients after login. Indexed by tenant_id
--    so the FCM service can fan-out alerts to all devices of a tenant in one
--    query.
-- 2) New column metadata.alert_severities.notify_push: per-severity feature
--    flag so an admin can disable push notifications for a given severity
--    without redeploying (e.g. INFO → no notif, CRITICAL → always notif).
--
-- Idempotent (uses IF NOT EXISTS) to make re-runs against the dev/prod
-- databases safe.
-- =============================================================================

-- Per-severity push notification flag.
ALTER TABLE metadata.alert_severities
    ADD COLUMN IF NOT EXISTS notify_push BOOLEAN NOT NULL DEFAULT TRUE;

-- Device push tokens. One row per (device, user) login pair; rotates via
-- onNewToken on the client.
CREATE TABLE IF NOT EXISTS metadata.push_tokens (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES metadata.users(id)   ON DELETE CASCADE,
    tenant_id    BIGINT      NOT NULL REFERENCES metadata.tenants(id) ON DELETE CASCADE,
    token        TEXT        NOT NULL UNIQUE,
    platform     VARCHAR(16) NOT NULL CHECK (platform IN ('ANDROID','IOS','WEB')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_push_tokens_tenant ON metadata.push_tokens(tenant_id);
CREATE INDEX IF NOT EXISTS idx_push_tokens_user   ON metadata.push_tokens(user_id);
