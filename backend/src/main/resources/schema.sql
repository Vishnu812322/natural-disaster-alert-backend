CREATE DATABASE IF NOT EXISTS disaster_alert CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE disaster_alert;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  phone VARCHAR(50) UNIQUE,
  role VARCHAR(50) DEFAULT 'ADMIN',
  latitude DOUBLE, longitude DOUBLE,
  district VARCHAR(255), state VARCHAR(255),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS disaster_types (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(100) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS disaster_alerts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  description TEXT NOT NULL,
  disaster_type VARCHAR(100) NOT NULL,
  severity VARCHAR(50) NOT NULL,
  status VARCHAR(50) NOT NULL,
  source VARCHAR(255) NOT NULL,
  source_alert_id VARCHAR(255),
  center_latitude DOUBLE,
  center_longitude DOUBLE,
  radius_km DOUBLE,
  area_text TEXT,
  instructions TEXT,
  test_alert BOOLEAN NOT NULL DEFAULT FALSE,
  effective_at TIMESTAMP NULL,
  expires_at TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_alert_status (status),
  INDEX idx_alert_created (created_at)
);

CREATE TABLE IF NOT EXISTS user_devices (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  fcm_token VARCHAR(500) NOT NULL UNIQUE,
  device_name VARCHAR(255),
  platform VARCHAR(50),
  last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_device_user (user_id)
);

CREATE TABLE IF NOT EXISTS notification_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  alert_id BIGINT,
  user_id BIGINT,
  device_id BIGINT,
  channel VARCHAR(50),
  status VARCHAR(100),
  provider_message TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_notification_alert (alert_id),
  INDEX idx_notification_user (user_id),
  INDEX idx_notification_device (device_id)
);


CREATE TABLE IF NOT EXISTS feed_sources (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(100) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  type VARCHAR(50) NOT NULL,
  url VARCHAR(1000) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  last_checked_at TIMESTAMP NULL,
  last_success_at TIMESTAMP NULL,
  last_http_status INT NULL,
  last_item_count INT NULL,
  etag VARCHAR(1000) NULL,
  last_error VARCHAR(4000) NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
