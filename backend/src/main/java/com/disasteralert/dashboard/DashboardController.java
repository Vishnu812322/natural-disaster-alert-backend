package com.disasteralert.dashboard;
import com.disasteralert.disaster.DisasterAlertRepository;
import com.disasteralert.users.UserRepository;
import com.disasteralert.notification.NotificationLogRepository;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/dashboard")
public class DashboardController {
    private final DisasterAlertRepository alerts; private final UserRepository users; private final NotificationLogRepository logs;
    public DashboardController(DisasterAlertRepository alerts,UserRepository users,NotificationLogRepository logs){this.alerts=alerts;this.users=users;this.logs=logs;}
    @GetMapping("/summary")
    public Map<String,Object> summary(){
        long active=alerts.findByStatusOrderByCreatedAtDesc("ACTIVE").size();
        return Map.of("activeAlerts",active,"users",users.count(),"enabledUsers",users.countByEnabledTrue(),"notificationLogs",logs.count());
    }
}
