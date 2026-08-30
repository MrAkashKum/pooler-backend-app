package com.akash.pooler_backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MatchPreference {
    ANY,
    /** Legacy value kept so older stored rows and clients remain compatible. */
    BOTH,
    MALE,
    FEMALE;

    @JsonCreator
    public static MatchPreference fromJson(String value) {
        if (value == null || value.isBlank()) {
            return ANY;
        }
        return switch (value.trim().toUpperCase()) {
            case "MALE", "MEN", "M" -> MALE;
            case "FEMALE", "WOMEN", "F" -> FEMALE;
            case "BOTH", "ANY", "ALL", "EVERYONE" -> ANY;
            default -> ANY;
        };
    }

    public static MatchPreference normalized(MatchPreference preference) {
        return preference == null || preference == BOTH ? ANY : preference;
    }
}
