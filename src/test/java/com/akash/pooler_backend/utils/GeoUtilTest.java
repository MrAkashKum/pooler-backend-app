package com.akash.pooler_backend.utils;

import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GeoUtilTest {

    @Test
    void followsUtilityClassContract() {
        ArchitectureAssertions.assertUtilityClass(GeoUtil.class);
    }

    @Test
    void haversineReturnsZeroForSameCoordinate() {
        assertEquals(0.0, GeoUtil.haversineKm(1.3, 103.8, 1.3, 103.8), 0.0001);
    }

    @Test
    void bearingDeltaUsesSmallestAngle() {
        assertEquals(20.0, GeoUtil.bearingDelta(350, 10), 0.0001);
    }

    @Test
    void midpointOfSameCoordinateReturnsSameCoordinate() {
        assertArrayEquals(new double[] {1.3, 103.8}, GeoUtil.midpoint(1.3, 103.8, 1.3, 103.8), 0.0001);
    }
}
