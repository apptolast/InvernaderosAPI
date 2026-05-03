-- =============================================================================
-- V40: Add actor metadata to alert_state_changes
-- =============================================================================
-- Adds actor_user_id, actor_kind, actor_ref columns so the audit trail can
-- answer "who triggered this transition" historically (USER for API,
-- DEVICE for MQTT, SYSTEM for scheduler/automatic).
--
-- Best-effort backfill:
--   - source=MQTT  → actor_kind='DEVICE' (no per-row gateway info recoverable)
--   - source=SYSTEM → actor_kind='SYSTEM'
--   - source=API   → actor_kind='USER'; for the LATEST API close per alert,
--                    copy alerts.resolved_by_user_id into actor_user_id.
--                    Older API transitions stay with actor_user_id=NULL
--                    honestly (data was never persisted).
-- =============================================================================

ALTER TABLE metadata.alert_state_changes
    ADD COLUMN actor_user_id BIGINT NULL REFERENCES metadata.users(id) ON DELETE SET NULL,
    ADD COLUMN actor_kind    VARCHAR(16) NOT NULL DEFAULT 'SYSTEM'
                CHECK (actor_kind IN ('USER', 'DEVICE', 'SYSTEM')),
    ADD COLUMN actor_ref     VARCHAR(128) NULL;

-- Backfill 1: MQTT rows
UPDATE metadata.alert_state_changes
   SET actor_kind = 'DEVICE'
 WHERE source = 'MQTT';

-- Backfill 2: SYSTEM rows (default already 'SYSTEM' but explicit for clarity if any pre-existed)
UPDATE metadata.alert_state_changes
   SET actor_kind = 'SYSTEM'
 WHERE source = 'SYSTEM';

-- Backfill 3: API rows base
UPDATE metadata.alert_state_changes
   SET actor_kind = 'USER'
 WHERE source = 'API';

-- Backfill 4: latest API close per alert → recover actor_user_id from alerts.resolved_by_user_id
WITH latest_resolution_per_alert AS (
    SELECT DISTINCT ON (alert_id) id, alert_id
      FROM metadata.alert_state_changes
     WHERE source = 'API' AND to_resolved = TRUE
     ORDER BY alert_id, at DESC
)
UPDATE metadata.alert_state_changes asc_row
   SET actor_user_id = a.resolved_by_user_id
  FROM latest_resolution_per_alert latest
  JOIN metadata.alerts a ON a.id = latest.alert_id
 WHERE asc_row.id = latest.id
   AND a.resolved_by_user_id IS NOT NULL;

-- Indexes for actor lookups
CREATE INDEX idx_alert_state_changes_actor_user
    ON metadata.alert_state_changes(actor_user_id)
 WHERE actor_user_id IS NOT NULL;

CREATE INDEX idx_alert_state_changes_actor_kind
    ON metadata.alert_state_changes(actor_kind);

COMMENT ON COLUMN metadata.alert_state_changes.actor_user_id IS 'User who triggered this transition. Populated for source=API only.';
COMMENT ON COLUMN metadata.alert_state_changes.actor_kind   IS 'USER (API), DEVICE (MQTT), SYSTEM (auto/scheduler).';
COMMENT ON COLUMN metadata.alert_state_changes.actor_ref    IS 'Free-form actor reference: gateway/device id for DEVICE, job name for SYSTEM.';
