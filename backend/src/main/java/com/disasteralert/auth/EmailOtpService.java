package com.disasteralert.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailOtpService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailOtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String recipientEmail, String otp, String purpose) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(recipientEmail);
        message.setSubject(
                "Natural Disaster Alert System - OTP"
        );

        message.setText(
                "Dear User,\n\n" +
                "Your OTP for " + purpose + " is:\n\n" +
                otp + "\n\n" +
                "This OTP is valid for 10 minutes.\n\n" +
                "Do not share this OTP with anyone.\n\n" +
                "Regards,\n" +
                "Natural Disaster Alert System"
        );

        mailSender.send(message);
    }
}