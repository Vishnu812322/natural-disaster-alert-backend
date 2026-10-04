package com.disasteralert.auth;

import com.disasteralert.security.JwtService;
import com.disasteralert.users.User;
import com.disasteralert.users.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {
    private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt;
    private final ConcurrentHashMap<String,String> otps = new ConcurrentHashMap<>();

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users=users; this.encoder=encoder; this.jwt=jwt;
    }

    public String login(LoginRequest req) {
        User u = users.findByEmail(req.email()).orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!u.isEnabled() || !encoder.matches(req.password(),u.getPasswordHash())) throw new RuntimeException("Invalid credentials");
        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0,1000000));
        otps.put(req.email(), otp);
        System.out.println("[DEVELOPMENT OTP] " + req.email() + " => " + otp);
        return "OTP_SENT";
    }

    public String verify(VerifyOtpRequest req) {
        String expected=otps.get(req.email());
        if (expected==null || !expected.equals(req.otp())) throw new RuntimeException("Invalid OTP");
        otps.remove(req.email());
        User u=users.findByEmail(req.email()).orElseThrow();
        return jwt.generate(u.getEmail(),u.getRole());
    }
}
