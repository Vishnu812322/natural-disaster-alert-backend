USE disaster_alert;
USE defaultdb;

-- Run once against an existing Phase 5 database.
ALTER TABLE notification_logs
  ADD INDEX idx_notification_device (device_id);
