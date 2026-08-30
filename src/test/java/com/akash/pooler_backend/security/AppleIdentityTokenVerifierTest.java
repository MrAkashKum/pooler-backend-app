package com.akash.pooler_backend.security;

import com.akash.pooler_backend.config.AppProperties;
import com.akash.pooler_backend.exception.AuthenticationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppleIdentityTokenVerifierTest {

    private static final String CLIENT_ID = "com.hoppo.ios";
    private static final String KEY_ID = "apple-test-key";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ECPublicKey publicKey;
    private ECPrivateKey privateKey;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = generator.generateKeyPair();
        publicKey = (ECPublicKey) keyPair.getPublic();
        privateKey = (ECPrivateKey) keyPair.getPrivate();

    }

    @Test
    void verifyAcceptsValidAppleIdentityToken() throws Exception {
        AppleIdentityTokenVerifier verifier = verifier(CLIENT_ID);

        AppleIdentityClaims claims = verifier.verify(identityToken(CLIENT_ID, true, 300));

        assertEquals("apple-user-123", claims.subject());
        assertEquals("rider@privaterelay.appleid.com", claims.email());
        assertTrue(claims.emailVerified());
    }

    @Test
    void verifyRejectsUnexpectedAudience() throws Exception {
        AppleIdentityTokenVerifier verifier = verifier("com.hoppo.other");

        String token = identityToken(CLIENT_ID, true, 300);

        assertThrows(AuthenticationException.class, () -> verifier.verify(token));
    }

    @Test
    void verifyRejectsUnverifiedEmail() throws Exception {
        AppleIdentityTokenVerifier verifier = verifier(CLIENT_ID);

        String token = identityToken(CLIENT_ID, false, 300);

        assertThrows(AuthenticationException.class, () -> verifier.verify(token));
    }

    @Test
    void verifyRejectsExpiredToken() throws Exception {
        AppleIdentityTokenVerifier verifier = verifier(CLIENT_ID);

        String token = identityToken(CLIENT_ID, true, -300);

        assertThrows(AuthenticationException.class, () -> verifier.verify(token));
    }

    @Test
    void verifyRejectsMalformedToken() {
        AppleIdentityTokenVerifier verifier = verifier(CLIENT_ID);

        assertThrows(AuthenticationException.class, () -> verifier.verify("not-a-jwt"));
    }

    private AppleIdentityTokenVerifier verifier(String clientIds) {
        AppProperties properties = new AppProperties();
        AppProperties.Auth.Apple apple = properties.getAuth().getApple();
        apple.setClientIds(clientIds);
        apple.setJwksUrl("https://appleid.apple.com/auth/keys");
        apple.setRequestTimeoutSeconds(2);
        apple.setConnectTimeoutSeconds(2);
        apple.setJwksCacheMinutes(5);

        AppleIdentityTokenVerifier verifier = new AppleIdentityTokenVerifier(properties, objectMapper);
        verifier.initializeHttpClient();
        verifier.replaceJwkCacheForTesting(Map.of(KEY_ID, jwk()));
        return verifier;
    }

    private String identityToken(String audience, boolean emailVerified, long expiresInSeconds) throws Exception {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> header = Map.of(
                "alg", "ES256",
                "kid", KEY_ID);
        Map<String, Object> claims = Map.of(
                "iss", "https://appleid.apple.com",
                "aud", audience,
                "sub", "apple-user-123",
                "email", "rider@privaterelay.appleid.com",
                "email_verified", emailVerified,
                "iat", now,
                "exp", now + expiresInSeconds);

        String signingInput = base64Url(objectMapper.writeValueAsBytes(header))
                + "." + base64Url(objectMapper.writeValueAsBytes(claims));
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(privateKey);
        signer.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        return signingInput + "." + base64Url(derToRaw(signer.sign()));
    }

    private Map<String, Object> jwk() {
        return Map.of(
                "kty", "EC",
                "kid", KEY_ID,
                "use", "sig",
                "alg", "ES256",
                "crv", "P-256",
                "x", base64Url(fixedLength(publicKey.getW().getAffineX())),
                "y", base64Url(fixedLength(publicKey.getW().getAffineY())));
    }

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static byte[] fixedLength(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length == 32) {
            return bytes;
        }
        byte[] fixed = new byte[32];
        int sourcePos = Math.max(0, bytes.length - 32);
        int length = Math.min(bytes.length, 32);
        System.arraycopy(bytes, sourcePos, fixed, 32 - length, length);
        return fixed;
    }

    private static byte[] derToRaw(byte[] derSignature) {
        int firstLength = derSignature[3];
        int firstStart = 4;
        int secondLengthIndex = firstStart + firstLength + 1;
        int secondLength = derSignature[secondLengthIndex];
        int secondStart = secondLengthIndex + 1;

        byte[] raw = new byte[64];
        copyDerIntegerToRaw(derSignature, firstStart, firstLength, raw, 0);
        copyDerIntegerToRaw(derSignature, secondStart, secondLength, raw, 32);
        return raw;
    }

    private static void copyDerIntegerToRaw(byte[] der, int start, int length, byte[] raw, int rawOffset) {
        int valueStart = start;
        int valueLength = length;
        if (valueLength > 32 && der[valueStart] == 0) {
            valueStart++;
            valueLength--;
        }
        System.arraycopy(der, valueStart, raw, rawOffset + 32 - valueLength, valueLength);
    }
}
