package com.disasteralert.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByAlertIdOrderByCreatedAtDesc(Long alertId);
    Optional<NotificationLog> findTopByAlertIdAndDeviceIdOrderByCreatedAtDesc(Long alertId, Long deviceId);
}
