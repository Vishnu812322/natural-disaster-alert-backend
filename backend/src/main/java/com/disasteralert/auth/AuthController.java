package com.disasteralert.auth;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public Map<String, String> register(
            @Valid @RequestBody RegisterRequest request) {

        return Map.of(
                "status",
                service.register(request)
        );
    }

    @PostMapping("/login")
    public Map<String, String> login(
            @Valid @RequestBody LoginRequest request) {

        return Map.of(
                "status",
                service.login(request)
        );
    }

    @PostMapping("/verify-otp")
    public Map<String, String> verify(
            @Valid @RequestBody VerifyOtpRequest request) {

        return service.verifyWithUserInfo(request);
    }
}