package com.akash.pooler_backend.enums;

import com.akash.pooler_backend.constants.ResponseMessages;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Whether the user is broadcasting their availability for ride share.
 *
 * @author Akash Kumar
 */
public enum DiscoveryMode {
    OFF,
    ON;

    @JsonCreator
    public static DiscoveryMode fromJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return switch (value.trim().toUpperCase()) {
            case "ON", "VISIBLE" -> ON;
            case "OFF", "HIDDEN" -> OFF;
            default -> throw new IllegalArgumentException(ResponseMessages.unsupportedDiscoveryMode(value));
        };
    }
}
