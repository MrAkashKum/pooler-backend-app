package com.akash.pooler_backend.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecureTokenUtilTest {

    @Test
    void urlSafeTokenReturnsUrlSafeValue() {
        String token = SecureTokenUtil.urlSafeToken(32);

        assertTrue(token.matches("^[A-Za-z0-9_-]+$"));
        assertTrue(token.length() >= 40);
    }

    @Test
    void urlSafeTokenGeneratesDifferentValues() {
        String first = SecureTokenUtil.urlSafeToken(32);
        String second = SecureTokenUtil.urlSafeToken(32);

        assertNotEquals(first, second);
    }

    @Test
    void urlSafeTokenRejectsInvalidLength() {
        assertThrows(IllegalArgumentException.class, () -> SecureTokenUtil.urlSafeToken(0));
    }
}
