package com.disasteralert.fcm;

import com.disasteralert.devices.UserDevice;
import com.disasteralert.devices.UserDeviceRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dev/fcm")
public class FcmTestController {

    private final UserDeviceRepository deviceRepository;
    private final FirebaseMessagingService firebaseMessagingService;

    public FcmTestController(
            UserDeviceRepository deviceRepository,
            FirebaseMessagingService firebaseMessagingService) {
        this.deviceRepository = deviceRepository;
        this.firebaseMessagingService = firebaseMessagingService;
    }

    @PostMapping("/test/{deviceId}")
    public Map<String, Object> testFcm(@PathVariable Long deviceId) {

        UserDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() ->
                        new RuntimeException("Device not found: " + deviceId));

        FirebaseMessagingService.SendResult result =
                firebaseMessagingService.sendTest(device);

        return Map.of(
                "deviceId", deviceId,
                "success", result.success(),
                "status", result.status(),
                "message", result.message()
        );
    }
}