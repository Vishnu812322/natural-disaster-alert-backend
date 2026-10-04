package com.disasteralert.cap;

public record RssItem(
        String title,
        String description,
        String link,
        String guid,
        String pubDate
) {}
