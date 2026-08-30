package com.akash.pooler_backend.enums;

/**
 * Result of running the "Overlap Rule" between two trips.
 *
 *  COMPATIBLE        - The shorter trip's drop-off is "on the way" to the
 *                      longer trip's drop-off. The cab can keep a near-
 *                      straight trajectory.
 *  REVERSE_DIRECTION - Both users are heading in opposite directions.
 *  INCOMPATIBLE      - Routes diverge by more than the allowed detour.
 *
 * @author Akash Kumar
 */
public enum RouteCompatibility {
    COMPATIBLE,
    REVERSE_DIRECTION,
    INCOMPATIBLE
}
