package com.akash.pooler_backend.enums;

import java.util.Arrays;
import java.util.Optional;

public enum InvitationStatusEnums {
    PENDING, ACCEPTED, DECLINED;

    /**
     * Java 8 way to validate and return the enum.
     * Returns an Optional to force the caller to handle invalid cases.
     */
    public static Optional<InvitationStatusEnums> parseStatus(String status) {
        return Optional.ofNullable(status)
                .map(String::trim)
                .flatMap(s -> Arrays.stream(InvitationStatusEnums.values())
                        .filter(e -> e.name().equalsIgnoreCase(s))
                        .findFirst());
    }
}
