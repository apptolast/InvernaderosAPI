CREATE TABLE IF NOT EXISTS metadata.shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at TIMESTAMP(3) NOT NULL DEFAULT now(),
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

CREATE INDEX IF NOT EXISTS idx_notification_log_sent_at
    ON metadata.notification_log(sent_at);
