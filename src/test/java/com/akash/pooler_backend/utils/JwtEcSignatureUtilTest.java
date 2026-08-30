package com.akash.pooler_backend.utils;

import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class JwtEcSignatureUtilTest {

    @Test
    void followsUtilityClassContract() {
        ArchitectureAssertions.assertUtilityClass(JwtEcSignatureUtil.class);
    }

    @Test
    void base64UrlDecodeUsesUrlDecoder() {
        byte[] value = "hoppo".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(value);

        assertArrayEquals(value, JwtEcSignatureUtil.base64UrlDecode(encoded));
    }
}
