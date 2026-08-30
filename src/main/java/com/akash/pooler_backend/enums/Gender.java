package com.akash.pooler_backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Gender {
    UNKNOWN,
    MALE,
    FEMALE,
    OTHER;

    @JsonCreator
    public static Gender fromJson(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return switch (value.trim().toUpperCase()) {
            case "MALE", "M" -> MALE;
            case "FEMALE", "F", "WOMAN", "WOMEN" -> FEMALE;
            case "OTHER", "NON_BINARY", "NON-BINARY" -> OTHER;
            default -> UNKNOWN;
        };
    }
}
