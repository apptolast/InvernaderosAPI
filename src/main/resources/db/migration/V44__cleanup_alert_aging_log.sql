-- =============================================================================
-- V44 — Cleanup ALERT_AGING entries from notification_log
-- =============================================================================
-- The aging notification feature (AlertAgingDetectorScheduler + AlertAgingFcmListener)
-- was rolled back: it was emitting a push every 5 minutes per unresolved alert,
-- saturating FCM and bombarding operators with non-actionable noise.
--
-- The corresponding code is removed in the same change set; the NotificationType
-- enum no longer carries an ALERT_AGING value. Any pre-existing rows in
-- notification_log with notification_type='ALERT_AGING' would crash on read
-- because NotificationType.valueOf("ALERT_AGING") would throw.
--
-- This migration deletes those rows. Idempotent (DELETE ... WHERE matches 0
-- rows safely on re-run).
--
-- Audit trail of dropped rows is preserved in this migration's intent only;
-- the row data is not exported because it was operational noise, not a
-- business event the user ever consciously triggered.
-- =============================================================================

DELETE FROM metadata.notification_log
 WHERE notification_type = 'ALERT_AGING';
