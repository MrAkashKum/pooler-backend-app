package com.akash.pooler_backend.utils;

import java.security.SecureRandom;
import java.util.Base64;

public final class SecureTokenUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SecureTokenUtil() {
    }

    public static String urlSafeToken(int byteLength) {
        if (byteLength <= 0) {
            throw new IllegalArgumentException("Token byte length must be positive");
        }
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
