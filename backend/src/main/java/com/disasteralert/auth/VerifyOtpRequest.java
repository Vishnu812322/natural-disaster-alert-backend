package com.disasteralert.auth;
import jakarta.validation.constraints.*;
public record VerifyOtpRequest(@Email @NotBlank String email, @NotBlank String otp) {}
