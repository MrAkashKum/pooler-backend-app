package com.akash.pooler_backend.utils;

import com.akash.pooler_backend.dto.request.ConfirmArrivalRequest;
import com.akash.pooler_backend.entity.PbRideEntity;
import com.akash.pooler_backend.enums.RideStatus;

public final class RideHandoffUtil {

    private RideHandoffUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isHandoffUnlocked(PbRideEntity ride) {
        return ride != null
                && ride.isPrimaryArrived()
                && ride.isSecondaryArrived()
                && ride.getStatus() == RideStatus.AT_PICKUP;
    }

    public static Double distanceFromPickupKm(PbRideEntity ride, ConfirmArrivalRequest request) {
        if (ride == null || request == null || request.getLatitude() == null || request.getLongitude() == null) {
            return null;
        }
        return GeoUtil.haversineKm(
                ride.getPickupLat(), ride.getPickupLng(),
                request.getLatitude(), request.getLongitude());
    }
}
