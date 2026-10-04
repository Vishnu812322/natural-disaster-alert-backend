package com.disasteralert.notification;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.disasteralert.devices.UserDevice;
import com.disasteralert.devices.UserDeviceRepository;
import com.disasteralert.disaster.DisasterAlert;
import com.disasteralert.fcm.FirebaseMessagingService;
import com.disasteralert.geolocation.GeoService;
import com.disasteralert.users.User;
import com.disasteralert.users.UserRepository;

@Service
public class NotificationService {

    private final UserDeviceRepository devices;
    private final NotificationLogRepository logs;
    private final UserRepository users;
    private final GeoService geoService;
    private final FirebaseMessagingService fcm;

    public NotificationService(
            UserDeviceRepository devices,
            NotificationLogRepository logs,
            UserRepository users,
            GeoService geoService,
            FirebaseMessagingService fcm) {

        this.devices = devices;
        this.logs = logs;
        this.users = users;
        this.geoService = geoService;
        this.fcm = fcm;
    }

    /**
     * Geographic targeting + real Firebase FCM delivery.
     *
     * Test alerts are deliberately not pushed to real devices.
     */
    public TargetingResult prepareAlert(DisasterAlert alert) {

        // ---------------------------------------------------------
        // 1. Validate alert geography
        // ---------------------------------------------------------

        if (alert.getCenterLatitude() == null
                || alert.getCenterLongitude() == null
                || alert.getRadiusKm() == null
                || alert.getRadiusKm() <= 0) {

            return new TargetingResult(
                    0,
                    0,
                    0,
                    0,
                    "NO_GEOGRAPHY"
            );
        }

        // ---------------------------------------------------------
        // 2. Never send test alerts to real devices
        // ---------------------------------------------------------

        if (alert.isTestAlert()) {

            return new TargetingResult(
                    0,
                    0,
                    0,
                    0,
                    "TEST_ALERT_NOT_SENT"
            );
        }

        int targetedUsers = 0;
        int targetedDevices = 0;
        int skippedUsers = 0;
        int sentDevices = 0;

        // ---------------------------------------------------------
        // 3. Find enabled users with location
        // ---------------------------------------------------------

        List<User> candidates =
                users.findByEnabledTrueAndLatitudeIsNotNullAndLongitudeIsNotNull();

        for (User user : candidates) {

            // -----------------------------------------------------
            // 4. Geographic targeting
            // -----------------------------------------------------

            boolean inside = geoService.insideCircle(
                    user.getLatitude(),
                    user.getLongitude(),
                    alert.getCenterLatitude(),
                    alert.getCenterLongitude(),
                    alert.getRadiusKm()
            );

            if (!inside) {

                skippedUsers++;

                continue;
            }

            targetedUsers++;

            // -----------------------------------------------------
            // 5. Find user's registered devices
            // -----------------------------------------------------

            List<UserDevice> userDevices =
                    devices.findByUserId(user.getId());

            if (userDevices.isEmpty()) {

                NotificationLog log = new NotificationLog();

                log.setAlertId(alert.getId());
                log.setUserId(user.getId());
                log.setChannel("PUSH");
                log.setStatus("TARGETED_NO_DEVICE");
                log.setProviderMessage(
                        "User is inside the alert area but has no registered FCM device."
                );
                log.setCreatedAt(Instant.now());

                logs.save(log);

                continue;
            }

            // -----------------------------------------------------
            // 6. Send to every registered device
            // -----------------------------------------------------

            for (UserDevice device : userDevices) {

                targetedDevices++;

                /*
                 * IMPORTANT:
                 *
                 * We intentionally do NOT skip an existing SENT log
                 * here.
                 *
                 * This allows repeated /prepare calls during development
                 * to actually test FCM delivery.
                 */

                NotificationLog log = new NotificationLog();

                log.setAlertId(alert.getId());
                log.setUserId(user.getId());
                log.setDeviceId(device.getId());
                log.setChannel("PUSH");
                log.setStatus("SENDING");
                log.setProviderMessage(
                        "Targeted geographically; sending through Firebase FCM."
                );
                log.setCreatedAt(Instant.now());

                log = logs.save(log);

                // -------------------------------------------------
                // 7. Send through Firebase
                // -------------------------------------------------

                FirebaseMessagingService.SendResult result =
                        fcm.send(alert, device);

                // -------------------------------------------------
                // 8. Save Firebase result
                // -------------------------------------------------

                log.setStatus(result.status());
                log.setProviderMessage(result.message());
                log.setCreatedAt(Instant.now());

                logs.save(log);

                // -------------------------------------------------
                // 9. Count successful delivery request
                // -------------------------------------------------

                if (result.success()) {

                    sentDevices++;
                }

                // -------------------------------------------------
                // 10. Remove invalid FCM device
                // -------------------------------------------------

                if ("UNREGISTERED".equals(result.status())) {

                    devices.delete(device);
                }
            }
        }

        // ---------------------------------------------------------
        // 11. Final status
        // ---------------------------------------------------------

        String status;

        if (targetedDevices == 0) {

            status = "TARGETED_NO_DEVICES";

        } else if (sentDevices > 0) {

            status = "FCM_PROCESSED";

        } else {

            status = "FCM_FAILED";
        }

        return new TargetingResult(
                targetedUsers,
                targetedDevices,
                skippedUsers,
                sentDevices,
                status
        );
    }

    public record TargetingResult(
            int targetedUsers,
            int targetedDevices,
            int skippedUsers,
            int sentDevices,
            String status
    ) {}

    public List<NotificationLog> logsForAlert(Long alertId) {

        return logs.findByAlertIdOrderByCreatedAtDesc(alertId);
    }
}