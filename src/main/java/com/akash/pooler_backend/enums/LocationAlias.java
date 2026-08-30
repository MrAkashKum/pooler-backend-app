package com.akash.pooler_backend.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * User saved-location alias type.
 * HOME / WORK are unique per user — used by the "Common Location" engine
 * to short-circuit pickup point computation when both users have the same
 * destination alias (e.g. both heading HOME).
 *
 * @author Akash Kumar
 */
public enum LocationAlias {
    HOME,
    WORK,
    FAVORITE,
    CUSTOM;

    public static Optional<LocationAlias> parse(String alias) {
        return Optional.ofNullable(alias)
                .map(String::trim)
                .flatMap(s -> Arrays.stream(values())
                        .filter(e -> e.name().equalsIgnoreCase(s))
                        .findFirst());
    }

    public boolean isUnique() {
        return this == HOME || this == WORK;
    }
}
