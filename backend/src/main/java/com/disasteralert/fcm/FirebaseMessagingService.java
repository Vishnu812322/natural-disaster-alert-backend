package com.disasteralert.fcm;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.disasteralert.devices.UserDevice;
import com.disasteralert.disaster.DisasterAlert;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;

@Service
public class FirebaseMessagingService {

    private final boolean enabled;
    private final String projectId;

    public FirebaseMessagingService(
            @Value("${app.fcm.enabled:false}") boolean enabled,
            @Value("${app.fcm.project-id:}") String projectId) {

        this.enabled = enabled;
        this.projectId = projectId;
    }

    public SendResult sendTest(UserDevice device) {

        if (!enabled) {
            return new SendResult(
                    false,
                    "FCM_DISABLED",
                    "Firebase FCM is disabled by configuration."
            );
        }

        if (device.getFcmToken() == null
                || device.getFcmToken().isBlank()) {

            return new SendResult(
                    false,
                    "INVALID_TOKEN",
                    "Device has no FCM registration token."
            );
        }

        try {

            FirebaseMessaging messaging = getMessaging();

            Message message = Message.builder()
                    .setToken(device.getFcmToken())
                    .setNotification(
                            com.google.firebase.messaging.Notification.builder()
                                    .setTitle("🚨 Natural Disaster Alert")
                                    .setBody(
                                            "Backend FCM test notification received successfully."
                                    )
                                    .build()
                    )
                    .putData("type", "DEVELOPMENT_FCM_TEST")
                    .putData("testAlert", "true")
                    .setAndroidConfig(
                            AndroidConfig.builder()
                                    .setPriority(AndroidConfig.Priority.HIGH)
                                    .setNotification(
                                            AndroidNotification.builder()
                                                    .setChannelId("disaster_alerts")
                                                    .setSound("default")
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            String messageId = messaging.send(message);

            System.out.println("========================================");
            System.out.println("FCM TEST SUCCESS: " + messageId);
            System.out.println("========================================");

            return new SendResult(
                    true,
                    "SENT",
                    "FCM message accepted by Firebase: " + messageId
            );

        } catch (FirebaseMessagingException ex) {

            MessagingErrorCode code =
                    ex.getMessagingErrorCode();

            String errorCode =
                    code == null
                            ? "UNKNOWN"
                            : code.name();

            String errorMessage =
                    ex.getMessage() == null
                            ? "No Firebase error message."
                            : ex.getMessage();

            System.out.println("========================================");
            System.out.println("FCM TEST SEND FAILED");
            System.out.println("Error Code : " + errorCode);
            System.out.println("Message    : " + errorMessage);
            System.out.println("========================================");

            ex.printStackTrace();

            return new SendResult(
                    false,
                    "FAILED",
                    "FCM error "
                            + errorCode
                            + ": "
                            + errorMessage
            );

        } catch (IOException | RuntimeException ex) {

            String errorMessage =
                    ex.getMessage() == null
                            ? "Unknown Firebase initialization/send error."
                            : ex.getMessage();

            System.out.println("========================================");
            System.out.println("FCM TEST INITIALIZATION/SEND FAILED");
            System.out.println("Exception : " + ex.getClass().getName());
            System.out.println("Message   : " + errorMessage);
            System.out.println("========================================");

            ex.printStackTrace();

            return new SendResult(
                    false,
                    "FAILED",
                    "Firebase initialization/send error: "
                            + errorMessage
            );
        }
    }

    public SendResult send(
            DisasterAlert alert,
            UserDevice device) {

        if (!enabled) {
            return new SendResult(
                    false,
                    "FCM_DISABLED",
                    "Firebase FCM is disabled by configuration."
            );
        }

        if (device.getFcmToken() == null
                || device.getFcmToken().isBlank()) {

            return new SendResult(
                    false,
                    "INVALID_TOKEN",
                    "Device has no FCM registration token."
            );
        }

        try {

            FirebaseMessaging messaging = getMessaging();

            String title = buildTitle(alert);
            String body = buildBody(alert);

            Map<String, String> data = new HashMap<>();

            data.put(
                    "type",
                    "DISASTER_ALERT"
            );

            data.put(
                    "alertId",
                    String.valueOf(alert.getId())
            );

            data.put(
                    "title",
                    title
            );

            data.put(
                    "body",
                    body
            );

            data.put(
                    "disasterType",
                    safe(alert.getDisasterType())
            );

            data.put(
                    "severity",
                    safe(alert.getSeverity())
            );

            data.put(
                    "area",
                    safe(alert.getAreaText())
            );

            data.put(
                    "source",
                    safe(alert.getSource())
            );

            data.put(
                    "testAlert",
                    String.valueOf(alert.isTestAlert())
            );

            /*
             * Data-only FCM message.
             *
             * Android receives this in
             * MyFirebaseMessagingService.onMessageReceived()
             * and creates the visible notification itself.
             */

            Message message = Message.builder()
                    .setToken(device.getFcmToken())
                    .putAllData(data)
                    .setAndroidConfig(
                            AndroidConfig.builder()
                                    .setPriority(
                                            AndroidConfig.Priority.HIGH
                                    )
                                    .build()
                    )
                    .build();

            String messageId =
                    messaging.send(message);

            System.out.println("========================================");
            System.out.println("FCM SEND SUCCESS");
            System.out.println("Alert ID   : " + alert.getId());
            System.out.println("Device ID  : " + device.getId());
            System.out.println("Message ID : " + messageId);
            System.out.println("========================================");

            return new SendResult(
                    true,
                    "SENT",
                    "FCM message accepted by Firebase: "
                            + messageId
            );

        } catch (FirebaseMessagingException ex) {

            MessagingErrorCode code =
                    ex.getMessagingErrorCode();

            String errorCode =
                    code == null
                            ? "UNKNOWN"
                            : code.name();

            String errorMessage =
                    ex.getMessage() == null
                            ? "No Firebase error message."
                            : ex.getMessage();

            System.out.println("========================================");
            System.out.println("FCM SEND FAILED");
            System.out.println("Alert ID   : " + alert.getId());
            System.out.println("Device ID  : " + device.getId());
            System.out.println("Error Code : " + errorCode);
            System.out.println("Message    : " + errorMessage);
            System.out.println("========================================");

            ex.printStackTrace();

            if (code == MessagingErrorCode.UNREGISTERED) {

                return new SendResult(
                        false,
                        "UNREGISTERED",
                        "FCM device registration is no longer valid: "
                                + errorMessage
                );
            }

            return new SendResult(
                    false,
                    "FAILED",
                    "FCM error "
                            + errorCode
                            + ": "
                            + errorMessage
            );

        } catch (IOException | RuntimeException ex) {

            String errorMessage =
                    ex.getMessage() == null
                            ? "Unknown Firebase initialization/send error."
                            : ex.getMessage();

            System.out.println("========================================");
            System.out.println("FIREBASE INITIALIZATION/SEND FAILED");
            System.out.println("Alert ID   : " + alert.getId());
            System.out.println("Device ID  : " + device.getId());
            System.out.println("Exception  : " + ex.getClass().getName());
            System.out.println("Message    : " + errorMessage);
            System.out.println("========================================");

            ex.printStackTrace();

            return new SendResult(
                    false,
                    "FAILED",
                    "Firebase initialization/send error: "
                            + errorMessage
            );
        }
    }

    /**
     * Initializes Firebase using the following priority:
     *
     * 1. FIREBASE_SERVICE_ACCOUNT_BASE64
     *    Used on Render/production.
     *
     * 2. GOOGLE_APPLICATION_CREDENTIALS
     *    Used locally with the Firebase JSON file.
     *
     * 3. Google Application Default Credentials
     *    Final fallback.
     */
    private FirebaseMessaging getMessaging()
            throws IOException {

        FirebaseApp app;

        if (FirebaseApp.getApps().isEmpty()) {

            GoogleCredentials credentials;

            String firebaseBase64 =
                    System.getenv("FIREBASE_SERVICE_ACCOUNT_BASE64");

            String credentialsPath =
                    System.getenv("GOOGLE_APPLICATION_CREDENTIALS");

            /*
             * -------------------------------------------------
             * OPTION 1: Render / Production
             * -------------------------------------------------
             */
            if (firebaseBase64 != null
                    && !firebaseBase64.isBlank()) {

                System.out.println(
                        "Loading Firebase credentials from "
                                + "FIREBASE_SERVICE_ACCOUNT_BASE64"
                );

                try {

                    byte[] decoded =
                            Base64.getDecoder()
                                    .decode(firebaseBase64);

                    credentials =
                            GoogleCredentials.fromStream(
                                    new ByteArrayInputStream(decoded)
                            );

                } catch (IllegalArgumentException ex) {

                    System.err.println(
                            "Invalid FIREBASE_SERVICE_ACCOUNT_BASE64"
                    );

                    throw new IOException(
                            "Invalid Firebase Base64 credentials.",
                            ex
                    );
                }
            }

            /*
             * -------------------------------------------------
             * OPTION 2: Local PC
             * -------------------------------------------------
             */
            else if (credentialsPath != null
                    && !credentialsPath.isBlank()) {

                System.out.println(
                        "Loading Firebase credentials from: "
                                + credentialsPath
                );

                credentials =
                        GoogleCredentials.fromStream(
                                new FileInputStream(credentialsPath)
                        );
            }

            /*
             * -------------------------------------------------
             * OPTION 3: Application Default Credentials
             * -------------------------------------------------
             */
            else {

                System.out.println(
                        "Loading Firebase credentials using "
                                + "Application Default Credentials"
                );

                credentials =
                        GoogleCredentials.getApplicationDefault();
            }

            FirebaseOptions options =
                    FirebaseOptions.builder()
                            .setCredentials(credentials)
                            .setProjectId(projectId)
                            .build();

            app =
                    FirebaseApp.initializeApp(options);

            System.out.println(
                    "Firebase initialized with project: "
                            + projectId
            );

        } else {

            app = FirebaseApp.getInstance();
        }

        return FirebaseMessaging.getInstance(app);
    }

    private String buildTitle(DisasterAlert alert) {

        String severity =
                safe(alert.getSeverity());

        if (severity.isBlank()) {
            return "⚠️ Disaster Alert";
        }

        return "⚠️ " + severity + " Disaster Alert";
    }

    private String buildBody(DisasterAlert alert) {

        String title =
                safe(alert.getTitle());

        String area =
                safe(alert.getAreaText());

        if (!area.isBlank()) {
            return title + " — " + area;
        }

        return title;
    }

    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }

    public record SendResult(
            boolean success,
            String status,
            String message
    ) {
    }
}