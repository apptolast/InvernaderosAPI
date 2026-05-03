-- =============================================================================
-- V42 — Add locale column to metadata.users
-- =============================================================================
-- Adds a BCP-47 language tag to users so the notification module can render
-- push payloads in the user's preferred language via MessageSource bundles.
--
-- Default 'es-ES' preserves the current implicit behaviour for all existing
-- users (all current push notifications are rendered in Spanish).
--
-- The CHECK constraint is added NOT VALID so it is enforced only for new rows
-- and future updates; existing rows already satisfy the DEFAULT 'es-ES'.
-- =============================================================================

ALTER TABLE metadata.users
    ADD COLUMN IF NOT EXISTS locale VARCHAR(8) NOT NULL DEFAULT 'es-ES';

ALTER TABLE metadata.users
    ADD CONSTRAINT users_locale_format
    CHECK (locale ~ '^[a-z]{2}-[A-Z]{2}$') NOT VALID;

COMMENT ON COLUMN metadata.users.locale IS 'BCP-47 language tag (e.g. es-ES, en-US). Used for i18n of push notifications.';
