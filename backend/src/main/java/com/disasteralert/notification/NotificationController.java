package com.disasteralert.notification;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.disasteralert.disaster.DisasterAlertRepository;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    private final DisasterAlertRepository alerts;

    public NotificationController(NotificationService service, DisasterAlertRepository alerts) {
        this.service = service;
        this.alerts = alerts;
    }

    @PostMapping("/alert/{id}/prepare")
    public Map<String, Object> prepare(@PathVariable Long id) {
        var alert = alerts.findById(id).orElseThrow();
        var result = service.prepareAlert(alert);
        return Map.of(
                "alertId", id,
                "targetedUsers", result.targetedUsers(),
                "targetedDevices", result.targetedDevices(),
                "skippedUsers", result.skippedUsers(),
                "sentDevices", result.sentDevices(),
                "status", result.status(),
                "mode", "geographic-targeting-and-fcm"
        );
    }

    @GetMapping("/alert/{id}")
    public List<NotificationLog> byAlert(@PathVariable Long id) {
        return service.logsForAlert(id);
    }

    
}
