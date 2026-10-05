package com.disasteralert.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.disasteralert.security.JwtService;
import com.disasteralert.users.User;
import com.disasteralert.users.UserRepository;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    private final ConcurrentHashMap<String, String> otps =
            new ConcurrentHashMap<>();

    public AuthService(
            UserRepository users,
            PasswordEncoder encoder,
            JwtService jwt) {

        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    // =========================================================
    // USER REGISTRATION
    // =========================================================

    public String register(RegisterRequest req) {

        // Check email
        if (users.findByEmail(req.email()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        // Check phone
        if (users.findByPhone(req.phone()).isPresent()) {
            throw new RuntimeException("Phone number already registered");
        }

        // Create new user
        User user = new User();

        user.setName(req.name());
        user.setEmail(req.email());
        user.setPhone(req.phone());

        // Never store the plain password
        user.setPasswordHash(
                encoder.encode(req.password())
        );

        // Normal Android user
        user.setRole("USER");

        // Account enabled
        user.setEnabled(true);

        // Location will be added after Android permission
        user.setLatitude(null);
        user.setLongitude(null);

        users.save(user);

        // Generate registration OTP
        String otp = generateOtp();

        otps.put(req.email(), otp);

        System.out.println(
                "[DEVELOPMENT OTP] Registration: "
                        + req.email()
                        + " => "
                        + otp
        );

        return "OTP_SENT";
    }


    // =========================================================
    // LOGIN
    // =========================================================

    public String login(LoginRequest req) {

        User u = users.findByEmail(req.email())
                .orElseThrow(() ->
                        new RuntimeException("Invalid credentials"));

        if (!u.isEnabled()
                || !encoder.matches(
                        req.password(),
                        u.getPasswordHash())) {

            throw new RuntimeException("Invalid credentials");
        }

        String otp = generateOtp();

        otps.put(req.email(), otp);

        System.out.println(
                "[DEVELOPMENT OTP] Login: "
                        + req.email()
                        + " => "
                        + otp
        );

        return "OTP_SENT";
    }


    // =========================================================
    // VERIFY OTP
    // =========================================================

    public String verify(VerifyOtpRequest req) {

        String expected = otps.get(req.email());

        if (expected == null
                || !expected.equals(req.otp())) {

            throw new RuntimeException("Invalid OTP");
        }

        // OTP can only be used once
        otps.remove(req.email());

        User u = users.findByEmail(req.email())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return jwt.generate(
                u.getEmail(),
                u.getRole()
        );
    }



    public Map<String, String> verifyWithUserInfo(
        VerifyOtpRequest req) {

    String expected = otps.get(req.email());

    if (expected == null
            || !expected.equals(req.otp())) {

        throw new RuntimeException("Invalid OTP");
    }

    otps.remove(req.email());

    User u = users.findByEmail(req.email())
            .orElseThrow(() ->
                    new RuntimeException("User not found"));

    String token = jwt.generate(
            u.getEmail(),
            u.getRole()
    );

    return Map.of(
            "token", token,
            "userId", String.valueOf(u.getId()),
            "name", u.getName(),
            "email", u.getEmail()
    );
}

    // =========================================================
    // OTP GENERATOR
    // =========================================================

    private String generateOtp() {

        return String.format(
                "%06d",
                ThreadLocalRandom.current()
                        .nextInt(0, 1_000_000)
        );
    }
}