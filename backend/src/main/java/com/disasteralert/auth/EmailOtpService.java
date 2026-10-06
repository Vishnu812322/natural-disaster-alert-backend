package com.disasteralert.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
public class EmailOtpService {

    private final RestTemplate restTemplate;

    @Value("${brevo.api-key}")
    private String apiKey;

    @Value("${brevo.sender-email}")
    private String senderEmail;

    @Value("${brevo.sender-name:Natural Disaster Alert System}")
    private String senderName;

    public EmailOtpService() {
        this.restTemplate = new RestTemplate();
    }

    public void sendOtp(
            String recipientEmail,
            String otp,
            String purpose
    ) {

        String url = "https://api.brevo.com/v3/smtp/email";

        System.out.println("========================================");
        System.out.println("BREVO OTP EMAIL");
        System.out.println("Recipient = " + recipientEmail);
        System.out.println("Sender    = " + senderEmail);
        System.out.println("API Key configured = "
                + (apiKey != null && !apiKey.isBlank()));
        System.out.println("========================================");

        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException(
                    "BREVO_API_KEY is not configured"
            );
        }

        if (senderEmail == null || senderEmail.isBlank()) {
            throw new RuntimeException(
                    "BREVO_SENDER_EMAIL is not configured"
            );
        }

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(
                List.of(MediaType.APPLICATION_JSON)
        );

        headers.set("api-key", apiKey);

        Map<String, Object> sender = new HashMap<>();
        sender.put("name", senderName);
        sender.put("email", senderEmail);

        Map<String, Object> recipient = new HashMap<>();
        recipient.put("email", recipientEmail);

        Map<String, Object> requestBody = new HashMap<>();

        requestBody.put("sender", sender);
        requestBody.put("to", List.of(recipient));

        requestBody.put(
                "subject",
                "Natural Disaster Alert System - OTP"
        );

        String text =
                "Dear User,\n\n" +
                "Your OTP for " + purpose + " is:\n\n" +
                otp + "\n\n" +
                "This OTP is valid for 10 minutes.\n\n" +
                "Please do not share this OTP with anyone.\n\n" +
                "Regards,\n" +
                "Natural Disaster Alert System";

        requestBody.put("textContent", text);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(requestBody, headers);

        try {

            var response = restTemplate.postForEntity(
                    url,
                    request,
                    String.class
            );

            System.out.println(
                    "BREVO HTTP STATUS = "
                            + response.getStatusCode()
            );

            System.out.println(
                    "BREVO RESPONSE = "
                            + response.getBody()
            );

        } catch (HttpStatusCodeException e) {

            System.err.println(
                    "BREVO HTTP ERROR = "
                            + e.getStatusCode()
            );

            System.err.println(
                    "BREVO RESPONSE = "
                            + e.getResponseBodyAsString()
            );

            throw new RuntimeException(
                    "Brevo email error: "
                            + e.getStatusCode()
                            + " - "
                            + e.getResponseBodyAsString()
            );

        } catch (Exception e) {

            System.err.println(
                    "BREVO CONNECTION ERROR = "
                            + e.getClass().getName()
            );

            System.err.println(
                    "BREVO ERROR = "
                            + e.getMessage()
            );

            throw new RuntimeException(
                    "Unable to send OTP email: "
                            + e.getMessage(),
                    e
            );
        }
    }
}