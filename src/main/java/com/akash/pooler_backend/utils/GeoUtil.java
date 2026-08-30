package com.akash.pooler_backend.utils;

import com.akash.pooler_backend.constants.ResponseMessages;

/**
 * Lightweight, dependency-free geographic utility helpers used by the
 * "Meet-in-the-Middle" engine and the route-compatibility checker.
 *
 * All distance values are in kilometres. All angles are in degrees.
 *
 * @author Akash Kumar
 */
public final class GeoUtil {

    /** Mean Earth radius (km) — sufficient for taxi-scale distances. */
    public static final double EARTH_RADIUS_KM = 6371.0088;

    private GeoUtil() {
        throw new IllegalStateException(ResponseMessages.UTILITY_CLASS);
    }

    /**
     * Great-circle distance between two coordinates using the Haversine formula.
     *
     * @return distance in kilometres
     */
    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Geographic midpoint of two coordinates calculated on a sphere
     * (correct on the great circle, not just the Cartesian average).
     *
     * @return [latitude, longitude] in degrees
     */
    public static double[] midpoint(double lat1, double lng1, double lat2, double lng2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double lambda1 = Math.toRadians(lng1);
        double dLambda = Math.toRadians(lng2 - lng1);

        double bx = Math.cos(phi2) * Math.cos(dLambda);
        double by = Math.cos(phi2) * Math.sin(dLambda);

        double midLat = Math.atan2(
                Math.sin(phi1) + Math.sin(phi2),
                Math.sqrt((Math.cos(phi1) + bx) * (Math.cos(phi1) + bx) + by * by)
        );
        double midLng = lambda1 + Math.atan2(by, Math.cos(phi1) + bx);

        // Normalise longitude to [-180, +180]
        midLng = ((midLng + 3 * Math.PI) % (2 * Math.PI)) - Math.PI;

        return new double[] { Math.toDegrees(midLat), Math.toDegrees(midLng) };
    }

    /**
     * Initial bearing (forward azimuth) from point 1 to point 2, in degrees [0, 360).
     */
    public static double bearingDegrees(double lat1, double lng1, double lat2, double lng2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dLambda = Math.toRadians(lng2 - lng1);

        double y = Math.sin(dLambda) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2)
                - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLambda);
        double theta = Math.atan2(y, x);
        return (Math.toDegrees(theta) + 360.0) % 360.0;
    }

    /**
     * Smallest absolute difference between two bearings, in degrees [0, 180].
     */
    public static double bearingDelta(double bearingA, double bearingB) {
        double diff = Math.abs(bearingA - bearingB) % 360.0;
        return diff > 180.0 ? 360.0 - diff : diff;
    }
}
