package com.disasteralert.sources;

import java.time.Instant;

public record SourceStatus(
        Long id,
        String code,
        String name,
        String type,
        String url,
        boolean enabled,
        Instant lastCheckedAt,
        Instant lastSuccessAt,
        Integer lastHttpStatus,
        Integer lastItemCount,
        String etag,
        String lastError
) {
    public static SourceStatus of(FeedSource s) {
        return new SourceStatus(
                s.getId(), s.getCode(), s.getName(), s.getType(), s.getUrl(),
                s.isEnabled(), s.getLastCheckedAt(), s.getLastSuccessAt(),
                s.getLastHttpStatus(), s.getLastItemCount(), s.getEtag(), s.getLastError()
        );
    }
}
