CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notification_log_sent_at
    ON metadata.notification_log(sent_at);
