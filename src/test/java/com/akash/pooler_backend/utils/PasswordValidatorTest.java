package com.akash.pooler_backend.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordValidatorTest {

    private final PasswordValidator validator = new PasswordValidator();

    @Test
    void acceptsStrongPassword() {
        assertTrue(validator.isValid("Strong@123"));
    }

    @Test
    void rejectsWeakPasswordWithReadableReasons() {
        var violations = validator.validate("weak");

        assertFalse(violations.isEmpty());
        assertTrue(violations.contains("Minimum 8 characters"));
    }
}
