USE disaster_alert;

-- Run once against an existing Phase 5 database.
ALTER TABLE notification_logs
  ADD COLUMN device_id BIGINT NULL,
  ADD INDEX idx_notification_device (device_id);
