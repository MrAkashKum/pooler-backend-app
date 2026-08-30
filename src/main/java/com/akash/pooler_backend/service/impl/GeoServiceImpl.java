package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.dto.response.CommonPickupPointResponse;
import com.akash.pooler_backend.dto.response.DistanceResponse;
import com.akash.pooler_backend.dto.response.RouteCompatibilityResponse;
import com.akash.pooler_backend.enums.RouteCompatibility;
import com.akash.pooler_backend.service.GeoService;
import com.akash.pooler_backend.utils.GeoUtil;
import org.springframework.stereotype.Service;

/**
 * Pure-Java implementation of {@link GeoService}.
 *
 * No external map provider is required for the MVP — all calculations
 * are great-circle approximations that are accurate enough for the
 * "walk a few hundred metres to a common point" use case.
 *
 * @author Akash Kumar
 */
@Service
public class GeoServiceImpl implements GeoService {

    /** Average human walking speed in km/h. */
    private static final double WALK_SPEED_KMH = 5.0;

    /** Bearing delta above which two trips count as REVERSE_DIRECTION. */
    private static final double REVERSE_BEARING_THRESHOLD = 135.0;

    /** Bearing delta below which two trips are COMPATIBLE. */
    private static final double COMPATIBLE_BEARING_THRESHOLD = 45.0;

    /** Maximum permitted detour percentage for a compatible match. */
    private static final double MAX_DETOUR_PERCENT = 25.0;

    @Override
    public DistanceResponse distance(double lat1, double lng1, double lat2, double lng2) {
        double km = GeoUtil.haversineKm(lat1, lng1, lat2, lng2);
        double bearing = GeoUtil.bearingDegrees(lat1, lng1, lat2, lng2);
        return DistanceResponse.builder()
                .distanceKm(round(km, 4))
                .bearingDegrees(round(bearing, 2))
                .build();
    }

    @Override
    public CommonPickupPointResponse computeCommonPickup(
            double userALat, double userALng,
            double userBLat, double userBLng) {

        double[] mid = GeoUtil.midpoint(userALat, userALng, userBLat, userBLng);
        double dA = GeoUtil.haversineKm(userALat, userALng, mid[0], mid[1]);
        double dB = GeoUtil.haversineKm(userBLat, userBLng, mid[0], mid[1]);

        return CommonPickupPointResponse.builder()
                .pickupLatitude(round(mid[0], 6))
                .pickupLongitude(round(mid[1], 6))
                .distanceFromUserAKm(round(dA, 4))
                .distanceFromUserBKm(round(dB, 4))
                .estimatedWalkMinutesUserA(round((dA / WALK_SPEED_KMH) * 60.0, 2))
                .estimatedWalkMinutesUserB(round((dB / WALK_SPEED_KMH) * 60.0, 2))
                .build();
    }

    @Override
    public RouteCompatibilityResponse computeCompatibility(
            double userAOriginLatitude, double userAOriginLongitude,
            double userADestinationLatitude, double userADestinationLongitude,
            double userBOriginLatitude, double userBOriginLongitude,
            double userBDestinationLatitude, double userBDestinationLongitude) {

        // Trip distances
        double userATripKm = GeoUtil.haversineKm(
                userAOriginLatitude, userAOriginLongitude,
                userADestinationLatitude, userADestinationLongitude);
        double userBTripKm = GeoUtil.haversineKm(
                userBOriginLatitude, userBOriginLongitude,
                userBDestinationLatitude, userBDestinationLongitude);

        // Common pickup hub for the two origins
        double[] hub = GeoUtil.midpoint(
                userAOriginLatitude, userAOriginLongitude,
                userBOriginLatitude, userBOriginLongitude);

        // Bearing of each trip from the common hub toward its drop-off
        double userABearing = GeoUtil.bearingDegrees(
                hub[0], hub[1], userADestinationLatitude, userADestinationLongitude);
        double userBBearing = GeoUtil.bearingDegrees(
                hub[0], hub[1], userBDestinationLatitude, userBDestinationLongitude);
        double bearingDelta = GeoUtil.bearingDelta(userABearing, userBBearing);

        // Identify primary (longer trip) and secondary (shorter)
        boolean userAIsPrimary = userATripKm >= userBTripKm;
        String primary = userAIsPrimary ? "A" : "B";
        String secondary = userAIsPrimary ? "B" : "A";
        double primaryTripKm = userAIsPrimary ? userATripKm : userBTripKm;
        double secondaryTripKm = userAIsPrimary ? userBTripKm : userATripKm;

        // Cab path: hub -> shorter drop -> longer drop
        double legHubToShortDropKm = userAIsPrimary
                ? GeoUtil.haversineKm(hub[0], hub[1], userBDestinationLatitude, userBDestinationLongitude)
                : GeoUtil.haversineKm(hub[0], hub[1], userADestinationLatitude, userADestinationLongitude);
        double legShortDropToLongDropKm = userAIsPrimary
                ? GeoUtil.haversineKm(
                        userBDestinationLatitude, userBDestinationLongitude,
                        userADestinationLatitude, userADestinationLongitude)
                : GeoUtil.haversineKm(
                        userADestinationLatitude, userADestinationLongitude,
                        userBDestinationLatitude, userBDestinationLongitude);
        double cabPathKm = legHubToShortDropKm + legShortDropToLongDropKm;

        double detourPercent = primaryTripKm <= 0
                ? 0
                : ((cabPathKm - primaryTripKm) / primaryTripKm) * 100.0;

        RouteCompatibility compat;
        if (bearingDelta >= REVERSE_BEARING_THRESHOLD) {
            compat = RouteCompatibility.REVERSE_DIRECTION;
        } else if (bearingDelta <= COMPATIBLE_BEARING_THRESHOLD
                && detourPercent <= MAX_DETOUR_PERCENT) {
            compat = RouteCompatibility.COMPATIBLE;
        } else {
            compat = RouteCompatibility.INCOMPATIBLE;
        }

        return RouteCompatibilityResponse.builder()
                .compatibility(compat)
                .bearingDeltaDegrees(round(bearingDelta, 2))
                .userATripKm(round(userATripKm, 4))
                .userBTripKm(round(userBTripKm, 4))
                .sharedKm(round(secondaryTripKm, 4))
                .detourPercent(round(detourPercent, 2))
                .primaryRoute(primary)
                .secondaryRoute(secondary)
                .build();
    }

    // ─── helpers ──────────────────────────────────────────────────────

    private static double round(double value, int decimals) {
        double f = Math.pow(10, decimals);
        return Math.round(value * f) / f;
    }
}
