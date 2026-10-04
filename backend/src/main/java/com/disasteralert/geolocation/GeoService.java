package com.disasteralert.geolocation;

import org.springframework.stereotype.Service;

@Service
public class GeoService {
    private static final double EARTH_RADIUS_KM = 6371.0088;

    public double distanceKm(double lat, double lng, double centerLat, double centerLng) {
        double dLat = Math.toRadians(centerLat - lat);
        double dLon = Math.toRadians(centerLng - lng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat))
                * Math.cos(Math.toRadians(centerLat))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public boolean insideCircle(double lat, double lng, double centerLat, double centerLng, double radiusKm) {
        return distanceKm(lat, lng, centerLat, centerLng) <= radiusKm;
    }
}
