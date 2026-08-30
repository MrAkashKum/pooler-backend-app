package com.akash.pooler_backend.utils;

import com.akash.pooler_backend.dto.request.ConfirmArrivalRequest;
import com.akash.pooler_backend.entity.PbRideEntity;
import com.akash.pooler_backend.enums.RideStatus;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RideHandoffUtilTest {

    @Test
    void followsUtilityClassContract() {
        ArchitectureAssertions.assertUtilityClass(RideHandoffUtil.class);
    }

    @Test
    void unlocksOnlyWhenBothArrivedAtPickup() {
        PbRideEntity ride = ride();
        ride.setPrimaryArrived(true);
        ride.setSecondaryArrived(true);
        ride.setStatus(RideStatus.AT_PICKUP);

        assertTrue(RideHandoffUtil.isHandoffUnlocked(ride));
    }

    @Test
    void doesNotUnlockWithoutBothConfirmations() {
        PbRideEntity ride = ride();
        ride.setPrimaryArrived(true);
        ride.setSecondaryArrived(false);
        ride.setStatus(RideStatus.AT_PICKUP);

        assertFalse(RideHandoffUtil.isHandoffUnlocked(ride));
    }

    @Test
    void distanceFromPickupReturnsNullWhenLocationMissing() {
        assertNull(RideHandoffUtil.distanceFromPickupKm(ride(), new ConfirmArrivalRequest()));
    }

    @Test
    void distanceFromPickupCalculatesDistance() {
        ConfirmArrivalRequest request = new ConfirmArrivalRequest(1.301, 103.801, 10.0, null);

        assertNotNull(RideHandoffUtil.distanceFromPickupKm(ride(), request));
    }

    private static PbRideEntity ride() {
        return PbRideEntity.builder()
                .entityId("ride-1")
                .primaryEntityId("user-a")
                .secondaryEntityId("user-b")
                .pickupLat(1.3)
                .pickupLng(103.8)
                .firstDropLat(1.31)
                .firstDropLng(103.81)
                .finalDropLat(1.32)
                .finalDropLng(103.82)
                .build();
    }
}
