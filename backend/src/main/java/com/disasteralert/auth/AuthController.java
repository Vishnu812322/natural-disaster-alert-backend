package com.disasteralert.auth;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service){this.service=service;}
    @PostMapping("/login") public Map<String,String> login(@Valid @RequestBody LoginRequest r){ return Map.of("status",service.login(r)); }
    @PostMapping("/verify-otp") public Map<String,String> verify(@Valid @RequestBody VerifyOtpRequest r){ return Map.of("token",service.verify(r)); }
}
