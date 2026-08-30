package com.akash.pooler_backend.enums;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Lifecycle states for a confirmed shared ride.
 *
 * Transition diagram:
 *   REQUESTED -> MATCHED -> CONFIRMED -> CAB_DISPATCHED -> EN_ROUTE_TO_PICKUP
 *   -> AT_PICKUP -> IN_TRANSIT -> COMPLETED
 *   (CANCELLED is reachable from any non-terminal state)
 *
 * @author Akash Kumar
 */
public enum RideStatus {
    REQUESTED,
    MATCHED,
    CONFIRMED,
    CAB_DISPATCHED,
    EN_ROUTE_TO_PICKUP,
    AT_PICKUP,
    IN_TRANSIT,
    COMPLETED,
    CANCELLED;

    private static final Set<RideStatus> TERMINAL = EnumSet.of(COMPLETED, CANCELLED);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    public boolean isActive() {
        return !isTerminal();
    }

    public static Optional<RideStatus> parse(String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .flatMap(s -> Arrays.stream(values())
                        .filter(e -> e.name().equalsIgnoreCase(s))
                        .findFirst());
    }
}
