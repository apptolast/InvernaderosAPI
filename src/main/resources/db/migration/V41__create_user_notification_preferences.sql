-- =============================================================================
-- V41 — User notification preferences
-- =============================================================================
-- Creates metadata.user_notification_preferences: per-user settings for push
-- notification delivery (category filters, severity threshold, quiet hours,
-- preferred channel). One row per user; defaults enable all categories with
-- no quiet hours, matching current implicit behaviour before this migration.
--
-- updated_at is managed by the application layer (Kotlin sets Instant.now()
-- on every save). No trigger dependency.
--
-- Idempotent (uses IF NOT EXISTS) to make re-runs against the dev/prod
-- databases safe.
-- =============================================================================

CREATE TABLE IF NOT EXISTS metadata.user_notification_preferences (
    user_id              BIGINT      PRIMARY KEY REFERENCES metadata.users(id) ON DELETE CASCADE,

    -- Category toggles (extensible: add new BOOLEAN columns per category)
    category_alerts      BOOLEAN     NOT NULL DEFAULT TRUE,
    category_devices     BOOLEAN     NOT NULL DEFAULT TRUE,
    category_subscription BOOLEAN   NOT NULL DEFAULT TRUE,

    -- Minimum severity level to receive a push (1=INFO, 2=WARNING, 3=ERROR, 4=CRITICAL).
    -- Reuses AlertSeverity.level scale. Notifications below this threshold are dropped.
    min_alert_severity   SMALLINT    NOT NULL DEFAULT 1
                         CHECK (min_alert_severity BETWEEN 1 AND 4),

    -- Quiet hours. NULL start/end means no quiet hours configured.
    -- Wrap-around (e.g. 22:00–07:00) is handled in application logic.
    quiet_hours_start    TIME        NULL,
    quiet_hours_end      TIME        NULL,
    quiet_hours_timezone VARCHAR(64) NOT NULL DEFAULT 'Europe/Madrid',

    -- Delivery channel. Only PUSH is dispatched today; others reserved for future phases.
    preferred_channel    VARCHAR(16) NOT NULL DEFAULT 'PUSH'
                         CHECK (preferred_channel IN ('PUSH','EMAIL','SMS','WHATSAPP')),

    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Backfill: create a defaults row for every existing user so the table is
-- immediately consistent. New users receive their row at registration time.
INSERT INTO metadata.user_notification_preferences (user_id)
SELECT id FROM metadata.users
ON CONFLICT (user_id) DO NOTHING;
