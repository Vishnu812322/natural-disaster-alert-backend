# CAP / SACHET Integration

## Real government source

The first production-oriented source configured in this project is the NDMA SACHET India CAP RSS feed:

`https://sachet.ndma.gov.in/cap_public_website/rss/rss_india.xml`

NDMA's SACHET portal states that India CAP alerts are published through its RSS feed and that subscribed agencies can consume them for dissemination. The official CAP integration guide requires consumers to cache the XML and ETag and send `If-None-Match` on subsequent requests.

## Current ingestion flow

1. `CapPollingScheduler` runs every 5 minutes by default.
2. It loads enabled `CAP_RSS` sources.
3. `CapIngestionService` requests the RSS feed.
4. If the server returns `304 Not Modified`, no feed body is downloaded or reprocessed.
5. For `200 OK`, the ETag is stored and RSS items are parsed.
6. Each item is mapped to an alert identifier.
7. The CAP XML endpoint is requested for that identifier.
8. CAP fields are normalized into `disaster_alerts`.
9. Existing `source + sourceAlertId` records are not duplicated.
10. The normalized alert is available to the admin/public map.

## Important production boundary

The application does not manufacture official warnings. A test alert created by the administrator remains marked as `testAlert=true`. Government CAP alerts are stored with `testAlert=false`.

Before public production deployment, verify the permitted consumption terms, availability, operational contact requirements and any authentication/rate limits for every external source you enable.

## Geographic handling

CAP `circle` values are currently converted into:

- center latitude
- center longitude
- radius in kilometers

CAP polygon/geocode expansion can be added as the next geographic-processing phase.
