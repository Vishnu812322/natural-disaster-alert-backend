package com.disasteralert.disaster;
import jakarta.validation.constraints.*;
import java.time.Instant;
public record CreateAlertRequest(
 @NotBlank String title, @NotBlank String description, @NotBlank String disasterType,
 @NotBlank String severity, @NotBlank String source, String sourceAlertId,
 Double centerLatitude, Double centerLongitude, Double radiusKm,
 String areaText, String instructions, boolean testAlert, Instant effectiveAt, Instant expiresAt) {}
