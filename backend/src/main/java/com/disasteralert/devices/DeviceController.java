package com.disasteralert.devices;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final UserDeviceRepository repo;

    public DeviceController(UserDeviceRepository repo) {
        this.repo = repo;
    }

    public record RegisterRequest(
            @NotNull Long userId,
            @NotBlank String fcmToken,
            String deviceName,
            String platform
    ) {}

    @GetMapping("/user/{userId}")
    public List<UserDevice> byUser(@PathVariable Long userId) {
        return repo.findByUserId(userId);
    }

    @GetMapping
    public List<UserDevice> all() {
        return repo.findAll();
    }

    @PostMapping
    public Map<String, Object> register(@Valid RegisterRequest r) {
        UserDevice d = repo.findByFcmToken(r.fcmToken()).orElseGet(UserDevice::new);
        d.setUserId(r.userId());
        d.setFcmToken(r.fcmToken());
        d.setDeviceName(r.deviceName());
        d.setPlatform(r.platform());
        d.setLastSeen(Instant.now());
        repo.save(d);
        return Map.of("status", "registered", "id", d.getId());
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> remove(@PathVariable Long id) {
        repo.deleteById(id);
        return Map.of("status", "removed");
    }

}
