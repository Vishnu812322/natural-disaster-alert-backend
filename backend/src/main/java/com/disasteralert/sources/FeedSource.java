package com.disasteralert.sources;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "feed_sources")
@Getter
@Setter
@NoArgsConstructor
public class FeedSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(nullable = false)
    private boolean enabled = true;

    private Instant lastCheckedAt;
    private Instant lastSuccessAt;
    private Integer lastHttpStatus;
    private Integer lastItemCount;

    @Column(length = 1000)
    private String etag;

    @Column(length = 4000)
    private String lastError;

    private Instant updatedAt = Instant.now();
}
