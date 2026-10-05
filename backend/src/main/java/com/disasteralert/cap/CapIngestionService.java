package com.disasteralert.cap;



import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.disasteralert.disaster.DisasterAlert;
import com.disasteralert.disaster.DisasterAlertRepository;
import com.disasteralert.notification.NotificationService;
import com.disasteralert.sources.FeedSource;
import com.disasteralert.sources.FeedSourceRepository;



@Service

public class CapIngestionService {

    private final RssFeedParser rssFeedParser;

    private final CapParser capParser;

    private final DisasterAlertRepository alertRepository;

    private final FeedSourceRepository sourceRepository;

    private final NotificationService notificationService;



    private final HttpClient httpClient = HttpClient.newBuilder()

            .connectTimeout(Duration.ofSeconds(15))

            .followRedirects(HttpClient.Redirect.NORMAL)

            .build();



    public CapIngestionService(

            RssFeedParser rssFeedParser,

            CapParser capParser,

            DisasterAlertRepository alertRepository,

            FeedSourceRepository sourceRepository,

            NotificationService notificationService) {

        this.rssFeedParser = rssFeedParser;

        this.capParser = capParser;

        this.alertRepository = alertRepository;

        this.sourceRepository = sourceRepository;

        this.notificationService = notificationService;

    }



    public synchronized void syncSource(FeedSource source) {

        if (!source.isEnabled()) return;



        source.setLastCheckedAt(Instant.now());

        source.setLastError(null);



        try {

            HttpRequest.Builder builder = HttpRequest.newBuilder()

                    .uri(URI.create(source.getUrl()))

                    .timeout(Duration.ofSeconds(30))

                    .header("Accept", "application/rss+xml, application/xml, text/xml;q=0.9, */*;q=0.5")

                    .GET();



            // Development: force SACHET feed download so existing CAP

// alerts are reprocessed after parser changes.

//

// if (source.getEtag() != null && !source.getEtag().isBlank()) {

//     builder.header("If-None-Match", source.getEtag());

// }



            HttpResponse<byte[]> response = httpClient.send(

                    builder.build(),

                    HttpResponse.BodyHandlers.ofByteArray()

            );



            source.setLastHttpStatus(response.statusCode());



            if (response.statusCode() == 304) {

                source.setLastSuccessAt(Instant.now());

                source.setLastItemCount(source.getLastItemCount() == null ? 0 : source.getLastItemCount());

                source.setLastError("OK: feed unchanged (304 Not Modified).");

                sourceRepository.save(source);

                return;

            }



            if (response.statusCode() < 200 || response.statusCode() >= 300) {

                throw new IllegalStateException("Feed returned HTTP " + response.statusCode());

            }



            response.headers().firstValue("ETag")

                    .filter(v -> !v.isBlank())

                    .ifPresent(source::setEtag);



            List<RssItem> items = rssFeedParser.parse(new ByteArrayInputStream(response.body()));

            source.setLastItemCount(items.size());



            int processed = 0;

            for (RssItem item : items) {

                if (importItem(source, item)) processed++;

            }



            source.setLastSuccessAt(Instant.now());

            source.setLastError("OK: processed " + processed + " CAP item(s).");

            sourceRepository.save(source);

        } catch (Exception ex) {

            source.setLastError(rootMessage(ex));

            sourceRepository.save(source);

        }

    }



    private boolean importItem(FeedSource source, RssItem item) {

        String identifier = extractIdentifier(item);

System.out.println("========================================");
System.out.println("SACHET RSS ITEM");
System.out.println("GUID       = " + item.guid());
System.out.println("LINK       = " + item.link());
System.out.println("TITLE      = " + item.title());
System.out.println("IDENTIFIER = " + identifier);
System.out.println("========================================");

if (identifier == null || identifier.isBlank()) {
    System.out.println("SACHET SKIPPED: identifier could not be extracted.");
    return false;
}

        if (identifier == null || identifier.isBlank()) return false;



        String capUrl = buildCapUrl(item.link(), identifier);
        System.out.println(
        "SACHET CAP URL = " + capUrl
);



        try {

            HttpRequest request = HttpRequest.newBuilder()

                    .uri(URI.create(capUrl))

                    .timeout(Duration.ofSeconds(20))

                    .header("Accept", "application/xml, text/xml;q=0.9, */*;q=0.5")

                    .GET()

                    .build();



            HttpResponse<byte[]> response = httpClient.send(

                    request,

                    HttpResponse.BodyHandlers.ofByteArray()

            );



            if (response.statusCode() < 200
        || response.statusCode() >= 300) {

    System.out.println(
            "SACHET CAP DOWNLOAD FAILED: HTTP "
                    + response.statusCode()
                    + " URL="
                    + capUrl
    );

    return false;
}



            CapParser.ParsedCap cap = capParser.parse(new ByteArrayInputStream(response.body()));

            String effectiveIdentifier = firstNonBlank(cap.identifier(), identifier);



            System.out.println("SACHET IMPORT DEBUG:");

System.out.println("  RSS identifier       = " + identifier);

System.out.println("  CAP identifier       = " + cap.identifier());

System.out.println("  Effective identifier = " + effectiveIdentifier);

System.out.println("  Source                = " + source.getCode());

System.out.println("  Polygon URL           = " + cap.polygonUrl());

System.out.println("  Circle                = " + cap.circle());

System.out.println("  LGD District Codes = " + cap.lgdDistrictCodes());
System.out.println("  District Names = " + extractDistrictNames(cap.areaDesc()));



            DisasterAlert existing = alertRepository

                    .findBySourceAndSourceAlertId(source.getCode(), effectiveIdentifier)

                    .orElse(null);



                    System.out.println(

        "  Existing alert       = "

                + (existing == null ? "NONE" : existing.getId())

);



            String msgType = normalize(cap.msgType());



            if ("CANCEL".equals(msgType)) {

                if (existing == null) return false;

                existing.setStatus("RESOLVED");

                existing.setUpdatedAt(Instant.now());

                alertRepository.save(existing);

                return true;

            }



            DisasterAlert alert = existing == null

                    ? new DisasterAlert()

                    : existing;



            mapToAlert(source, effectiveIdentifier, cap, alert);

            boolean wasNew = existing == null;

            System.out.println("BEFORE DB SAVE:");
System.out.println("  Alert ID = " + alert.getId());
System.out.println("  District Names = " + alert.getDistrictNames());
System.out.println("  LGD District Codes = " + alert.getLgdDistrictCodes());



            DisasterAlert saved = alertRepository.save(alert);



            if (wasNew && "ACTIVE".equals(saved.getStatus())) {

                notificationService.prepareAlert(saved);

            }



            return true;

        } catch (Exception ex) {

    System.out.println(

        "SACHET CAP import failed: " + rootMessage(ex)

    );

    ex.printStackTrace();

    return false;

}

    }



    private void mapToAlert(

            FeedSource source,

            String identifier,

            CapParser.ParsedCap cap,

            DisasterAlert alert) {



        alert.setTitle(firstNonBlank(cap.headline(), cap.event(), "Government disaster alert"));

        alert.setDescription(firstNonBlank(

                cap.description(),

                "See the authoritative source for the complete alert."

        ));

        alert.setDisasterType(normalizeType(cap.event()));

        alert.setSeverity(normalizeSeverity(cap.severity()));

        alert.setStatus(isOperationalAlert(cap) ? "ACTIVE" : "RESOLVED");

        alert.setSource(source.getCode());

        alert.setSourceAlertId(identifier);

        alert.setAreaText(cap.areaDesc());

alert.setInstructions(cap.instruction());



// -------------------------------------------------

// District fallback geographic information

// -------------------------------------------------



alert.setDistrictNames(

        extractDistrictNames(cap.areaDesc())

);



alert.setLgdDistrictCodes(

        cap.lgdDistrictCodes() == null

                ? null

                : String.join(",", cap.lgdDistrictCodes())

);



String capStatus = normalize(cap.status());



alert.setTestAlert(

        "TEST".equals(capStatus)

                || "EXERCISE".equals(capStatus)

                || "SYSTEM".equals(capStatus)

);












        parseCircle(cap.circle(), alert);



if (alert.getCenterLatitude() == null

        || alert.getCenterLongitude() == null

        || alert.getRadiusKm() == null) {



    System.out.println(

            "SACHET geometry: circle=" + cap.circle()

                    + ", polygon=" + cap.polygon()

                    + ", polygonUrl=" + cap.polygonUrl()

    );



    // First try polygon directly contained in CAP XML

    if (cap.polygon() != null && !cap.polygon().isBlank()) {



        System.out.println("SACHET geometry: using polygon from CAP XML");



        parsePolygon(cap.polygon(), alert);



    } else if (cap.polygonUrl() != null && !cap.polygonUrl().isBlank()) {



        // Only fall back to the external polygon URL

        System.out.println(

                "SACHET geometry: polygon not present in CAP XML; "

                        + "trying polygon URL"

        );



        parsePolygonUrl(cap.polygonUrl(), alert);



    } else {



        System.out.println(

                "SACHET geometry: no circle, polygon, or polygon URL available"

        );

    }

}



        Instant effective = parseInstant(cap.sent());

        alert.setEffectiveAt(effective == null ? Instant.now() : effective);



        Instant expires = parseInstant(cap.expires());

        alert.setExpiresAt(expires);

        alert.setUpdatedAt(Instant.now());

    }







    private boolean isOperationalAlert(CapParser.ParsedCap cap) {

        String msgType = normalize(cap.msgType());

        String status = normalize(cap.status());



        if ("CANCEL".equals(msgType)) return false;

        if ("ACTUAL".equals(status)) return true;

        if ("TEST".equals(status) || "EXERCISE".equals(status) || "SYSTEM".equals(status)) return true;

        return "ALERT".equals(msgType) || "UPDATE".equals(msgType);

    }



    private String normalizeType(String event) {

        String v = normalize(event);

        if (v.contains("FLOOD")) return "FLOOD";

        if (v.contains("CYCLONE") || v.contains("HURRICANE") || v.contains("TROPICAL")) return "CYCLONE";

        if (v.contains("EARTHQUAKE")) return "EARTHQUAKE";

        if (v.contains("TSUNAMI")) return "TSUNAMI";

        if (v.contains("LANDSLIDE")) return "LANDSLIDE";

        if (v.contains("WILDFIRE") || v.contains("FOREST FIRE")) return "WILDFIRE";

        if (v.contains("LIGHTNING") || v.contains("THUNDER")) return "EXTREME_WEATHER";

        if (v.contains("WEATHER") || v.contains("RAIN") || v.contains("STORM")) return "EXTREME_WEATHER";

        return "OTHER";

    }



    private String normalizeSeverity(String severity) {

        String v = normalize(severity);

        return switch (v) {

            case "EXTREME" -> "EXTREME";

            case "SEVERE" -> "SEVERE";

            case "MODERATE" -> "HIGH";

            case "MINOR" -> "MINOR";

            default -> "MODERATE";

        };

    }



    private void parseCircle(String circle, DisasterAlert alert) {

        if (circle == null || circle.isBlank()) return;

        String[] parts = circle.trim().split("\\s+");

        if (parts.length < 2) return;



        String[] coordinates = parts[0].split(",");

        if (coordinates.length != 2) return;



        try {

            alert.setCenterLatitude(Double.parseDouble(coordinates[0]));

            alert.setCenterLongitude(Double.parseDouble(coordinates[1]));

            alert.setRadiusKm(Double.parseDouble(parts[1]));

        } catch (NumberFormatException ignored) {

        }

    }



    private Instant parseInstant(String value) {

        if (value == null || value.isBlank()) return null;

        try {

            return Instant.parse(value);

        } catch (Exception ignored) {

            return null;

        }

    }



    private String extractIdentifier(RssItem item) {

        String[] candidates = {item.guid(), item.link()};

        for (String candidate : candidates) {

            if (candidate == null || candidate.isBlank()) continue;

            int index = candidate.indexOf("identifier=");

            if (index >= 0) {

                String value = candidate.substring(index + "identifier=".length());

                int amp = value.indexOf('&');

                if (amp >= 0) value = value.substring(0, amp);

                return value.trim();

            }

        }

        return item.guid() == null || item.guid().isBlank() ? null : item.guid().trim();

    }



    private String buildCapUrl(String link, String identifier) {

        if (link != null && link.contains("FetchXMLFile")) return link;

        return "https://sachet.ndma.gov.in/cap_public_website/FetchXMLFile?identifier="

                + URLEncoder.encode(identifier, StandardCharsets.UTF_8);

    }



    private String extractDistrictNames(String areaDesc) {

        if (areaDesc == null || areaDesc.isBlank()) {
            return null;
        }

        String value = areaDesc.trim();
        String lower = value.toLowerCase(Locale.ROOT);

        int districtsIndex = lower.indexOf(" districts of " );
        if (districtsIndex >= 0) {
            String districts = value.substring(0, districtsIndex).trim();
            if (!districts.isBlank()) return cleanDistrictNames(districts);
        }

        int districtIndex = lower.indexOf(" district of " );
        if (districtIndex >= 0) {
            String district = value.substring(0, districtIndex).trim();
            if (!district.isBlank()) return cleanDistrictNames(district);
        }

        String[] states = {
                "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar",
                "Chhattisgarh", "Goa", "Gujarat", "Haryana",
                "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala",
                "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya",
                "Mizoram", "Nagaland", "Odisha", "Punjab", "Rajasthan",
                "Sikkim", "Tamil Nadu", "Telangana", "Tripura",
                "Uttar Pradesh", "Uttarakhand", "West Bengal"
        };

        for (String state : states) {
            String suffix = ", " + state;
            if (value.endsWith(suffix)) {
                String districts = value.substring(0, value.length() - suffix.length()).trim();
                if (!districts.isBlank()) return cleanDistrictNames(districts);
            }
        }

        if (value.matches("\\d+\\s+Mandals?")) return null;
        if (value.matches("\\d+")) return null;

        return cleanDistrictNames(value);
    }

    private String cleanDistrictNames(String value) {

        if (value == null || value.isBlank()) return null;

        String cleaned = value.replaceAll("\\s+", " ").trim();

        return cleaned.isBlank() ? null : cleaned;
    }

    private String normalize(String value) {

        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);

    }



    private String firstNonBlank(String... values) {

        for (String value : values) {

            if (value != null && !value.isBlank()) return value.trim();

        }

        return "";

    }



    private String rootMessage(Exception ex) {

        Throwable current = ex;

        while (current.getCause() != null) current = current.getCause();

        return current.getMessage() == null

                ? current.getClass().getSimpleName()

                : current.getMessage();

    }

    private void parsePolygon(String polygon, DisasterAlert alert) {



    if (polygon == null || polygon.isBlank()) {

        return;

    }



    String[] points = polygon.trim().split("\\s+");



    if (points.length == 0) {

        return;

    }



    double sumLat = 0.0;

    double sumLng = 0.0;

    int count = 0;



    for (String point : points) {



        String[] coordinates = point.split(",");



        if (coordinates.length != 2) {

            continue;

        }



        try {



            double lat = Double.parseDouble(coordinates[0]);

            double lng = Double.parseDouble(coordinates[1]);



            sumLat += lat;

            sumLng += lng;



            count++;



        } catch (NumberFormatException ignored) {

            // Ignore invalid polygon point

        }

    }



    if (count == 0) {

        return;

    }



    double centerLat = sumLat / count;

    double centerLng = sumLng / count;



    double maxRadiusKm = 0.0;



    for (String point : points) {



        String[] coordinates = point.split(",");



        if (coordinates.length != 2) {

            continue;

        }



        try {



            double lat = Double.parseDouble(coordinates[0]);

            double lng = Double.parseDouble(coordinates[1]);



            double distance =

                    distanceKm(

                            centerLat,

                            centerLng,

                            lat,

                            lng

                    );



            maxRadiusKm =

                    Math.max(maxRadiusKm, distance);



        } catch (NumberFormatException ignored) {

            // Ignore invalid polygon point

        }

    }



    if (maxRadiusKm > 0) {



        alert.setCenterLatitude(centerLat);

        alert.setCenterLongitude(centerLng);



        // Add a small safety margin around the polygon.

        alert.setRadiusKm(maxRadiusKm * 1.10);

    }

}



private void parsePolygonUrl(String polygonUrl, DisasterAlert alert) {



    System.out.println(

    "SACHET polygon fetch started: " + polygonUrl

);



    if (polygonUrl == null || polygonUrl.isBlank()) {

        return;

    }



    try {

        HttpRequest request = HttpRequest.newBuilder()

                .uri(URI.create(polygonUrl.trim()))

                .timeout(Duration.ofSeconds(20))

                .header("Accept", "application/xml, text/xml;q=0.9, */*;q=0.5")

                .header("User-Agent", "NaturalDisasterAlert/1.0")

                .GET()

                .build();



        HttpResponse<byte[]> response = httpClient.send(

                request,

                HttpResponse.BodyHandlers.ofByteArray()

        );



        if (response.statusCode() < 200

                || response.statusCode() >= 300) {



            System.out.println(

                    "SACHET Polygon URL returned HTTP "

                            + response.statusCode()

                            + ": "

                            + polygonUrl

            );



            return;

        }



        String xml = new String(

                response.body(),

                StandardCharsets.UTF_8

        );



        parsePolygonXml(xml, alert);



    } catch (Exception ex) {



        System.out.println(

                "Unable to fetch SACHET polygon: "

                        + ex.getMessage()

        );

    }

}



private void parsePolygonXml(String xml, DisasterAlert alert) {

    if (xml == null || xml.isBlank()) {

        return;

    }



    try {

        DocumentBuilderFactory factory =

                DocumentBuilderFactory.newInstance();



        factory.setNamespaceAware(true);



        Document document =

                factory.newDocumentBuilder()

                        .parse(new ByteArrayInputStream(

                                xml.getBytes(StandardCharsets.UTF_8)

                        ));



        NodeList polygonNodes =

                document.getElementsByTagName("polygon");



        System.out.println(

                "SACHET polygon XML: found "

                        + polygonNodes.getLength()

                        + " polygon element(s)"

        );



        if (polygonNodes.getLength() == 0) {

            System.out.println(

                    "SACHET polygon XML: no <polygon> element found"

            );

            return;

        }



        String polygonText =

                polygonNodes.item(0)

                        .getTextContent();



        if (polygonText == null || polygonText.isBlank()) {

            System.out.println(

                    "SACHET polygon XML: polygon is empty"

            );

            return;

        }



        System.out.println(

                "SACHET polygon XML received successfully"

        );



        parsePolygon(polygonText.trim(), alert);



        System.out.println(

                "SACHET polygon parsed: center="

                        + alert.getCenterLatitude()

                        + ","

                        + alert.getCenterLongitude()

                        + ", radius="

                        + alert.getRadiusKm()

        );



    } catch (Exception ex) {

        System.out.println(

                "Unable to parse SACHET polygon XML: "

                        + ex.getMessage()

        );

        ex.printStackTrace();

    }

}



private double distanceKm(

        double lat1,

        double lon1,

        double lat2,

        double lon2) {



    double earthRadiusKm = 6371.0;



    double dLat = Math.toRadians(lat2 - lat1);

    double dLon = Math.toRadians(lon2 - lon1);



    double a =

            Math.sin(dLat / 2) * Math.sin(dLat / 2)

                    +

            Math.cos(Math.toRadians(lat1))

                    *

            Math.cos(Math.toRadians(lat2))

                    *

            Math.sin(dLon / 2) * Math.sin(dLon / 2);



    double c =

            2 * Math.atan2(

                    Math.sqrt(a),

                    Math.sqrt(1 - a)

            );



    return earthRadiusKm * c;

}

}
