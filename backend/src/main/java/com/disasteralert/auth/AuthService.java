package com.disasteralert.auth;

import java.time.LocalDateTime;
import java.util.Map;
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
    private final EmailOtpService emailOtpService;
    private final OtpVerificationRepository otpRepository;

    public AuthService(
            UserRepository users,
            PasswordEncoder encoder,
            JwtService jwt,
            EmailOtpService emailOtpService,
            OtpVerificationRepository otpRepository
    ) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.emailOtpService = emailOtpService;
        this.otpRepository = otpRepository;
    }

    // =========================================================
    // REGISTER
    // =========================================================

    public String register(RegisterRequest req) {

        if (users.findByEmail(req.email()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        if (users.findByPhone(req.phone()).isPresent()) {
            throw new RuntimeException("Phone number already registered");
        }

        User user = new User();

        user.setName(req.name());
        user.setEmail(req.email());
        user.setPhone(req.phone());

        user.setPasswordHash(
                encoder.encode(req.password())
        );

        user.setRole("USER");
        user.setEnabled(true);

        user.setLatitude(null);
        user.setLongitude(null);

        users.save(user);

        // Generate OTP
        String otp = generateOtp();

        // Save OTP in MySQL
        saveOtp(req.email(), otp);

        // Send OTP through Brevo
        emailOtpService.sendOtp(
                req.email(),
                otp,
                "new account registration"
        );

        return "OTP_SENT";
    }

    // =========================================================
    // LOGIN
    // =========================================================

    public String login(LoginRequest req) {

        User user = users.findByEmail(req.email())
                .orElseThrow(() ->
                        new RuntimeException("Invalid credentials")
                );

        if (!user.isEnabled()) {
            throw new RuntimeException("Account is disabled");
        }

        if (!encoder.matches(
                req.password(),
                user.getPasswordHash()
        )) {
            throw new RuntimeException("Invalid credentials");
        }

        // Generate OTP
        String otp = generateOtp();

        // Save OTP in MySQL
        saveOtp(req.email(), otp);

        // Send OTP through Brevo
        emailOtpService.sendOtp(
                req.email(),
                otp,
                "login"
        );

        return "OTP_SENT";
    }

    // =========================================================
    // VERIFY OTP
    // =========================================================

    public String verify(VerifyOtpRequest req) {

        validateOtp(
                req.email(),
                req.otp()
        );

        User user = users.findByEmail(req.email())
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        return jwt.generate(
                user.getEmail(),
                user.getRole()
        );
    }

    // =========================================================
    // VERIFY OTP + USER INFORMATION
    // =========================================================

    public Map<String, String> verifyWithUserInfo(
            VerifyOtpRequest req
    ) {

        validateOtp(
                req.email(),
                req.otp()
        );

        User user = users.findByEmail(req.email())
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        String token = jwt.generate(
                user.getEmail(),
                user.getRole()
        );

        return Map.of(
                "token", token,
                "userId", String.valueOf(user.getId()),
                "name", user.getName(),
                "email", user.getEmail()
        );
    }

    // =========================================================
    // GENERATE OTP
    // =========================================================

    private String generateOtp() {

        return String.format(
                "%06d",
                ThreadLocalRandom.current()
                        .nextInt(0, 1_000_000)
        );
    }

    // =========================================================
    // SAVE OTP TO MYSQL
    // =========================================================

    private void saveOtp(
            String email,
            String otp
    ) {

        // Remove previous OTPs for this email
        otpRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .ifPresent(otpRepository::delete);

        OtpVerification verification =
                new OtpVerification();

        verification.setEmail(email);
        verification.setOtp(otp);

        verification.setCreatedAt(
                LocalDateTime.now()
        );

        verification.setExpiresAt(
                LocalDateTime.now().plusMinutes(10)
        );

        otpRepository.save(verification);
    }

    // =========================================================
    // VALIDATE OTP
    // =========================================================

    private void validateOtp(
            String email,
            String otp
    ) {

        OtpVerification verification =
                otpRepository
                        .findTopByEmailOrderByCreatedAtDesc(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "OTP not found. Please request a new OTP."
                                )
                        );

        // Check expiration
        if (LocalDateTime.now()
                .isAfter(verification.getExpiresAt())) {

            otpRepository.delete(verification);

            throw new RuntimeException(
                    "OTP expired. Please request a new OTP."
            );
        }

        // Check OTP
        if (!verification.getOtp().equals(otp)) {

            throw new RuntimeException(
                    "Invalid OTP"
            );
        }

        // OTP can only be used once
        otpRepository.delete(verification);
    }
}