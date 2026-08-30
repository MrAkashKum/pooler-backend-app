package com.akash.pooler_backend.service;

import com.akash.pooler_backend.dto.response.CommonPickupPointResponse;
import com.akash.pooler_backend.dto.response.DistanceResponse;
import com.akash.pooler_backend.dto.response.RouteCompatibilityResponse;

/**
 * Geographic services backing the "Meet-in-the-Middle" engine and the
 * "Overlap Rule" route-compatibility check.
 *
 * @author Akash Kumar
 */
public interface GeoService {

    DistanceResponse distance(double lat1, double lng1, double lat2, double lng2);

    CommonPickupPointResponse computeCommonPickup(
            double userALat, double userALng,
            double userBLat, double userBLng);

    RouteCompatibilityResponse computeCompatibility(
            double userAOriginLat, double userAOriginLng,
            double userADestLat,   double userADestLng,
            double userBOriginLat, double userBOriginLng,
            double userBDestLat,   double userBDestLng);
}
