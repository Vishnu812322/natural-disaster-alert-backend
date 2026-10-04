package com.disasteralert.disaster;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "disaster_alerts")
@Getter
@Setter
@NoArgsConstructor
public class DisasterAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(nullable = false, length = 10000)
    String description;

    @Column(nullable = false)
    String disasterType;

    @Column(nullable = false)
    String severity;

    @Column(nullable = false)
    String status;

    @Column(nullable = false)
    String source;

    String sourceAlertId;

    Double centerLatitude;

    Double centerLongitude;

    Double radiusKm;

    @Column(length = 20000)
    String areaText;

    @Column(length = 20000)
    String instructions;

    // -------------------------------------------------
    // District fallback geographic targeting
    // -------------------------------------------------

    /**
     * District names obtained from the official CAP areaDesc.
     *
     * Example:
     * Kolhapur, Sindhudurg
     */
    @Column(length = 20000)
    String districtNames;

    /**
     * Official LGD district codes obtained from CAP geocode.
     *
     * Example:
     * 480,495
     */
    @Column(length = 5000)
    String lgdDistrictCodes;

    // -------------------------------------------------

    boolean testAlert;

    Instant effectiveAt;

    Instant expiresAt;

    Instant createdAt = Instant.now();

    Instant updatedAt = Instant.now();
}