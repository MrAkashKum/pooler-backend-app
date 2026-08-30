package com.akash.pooler_backend.security;

import com.akash.pooler_backend.config.AppProperties;
import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.exception.AuthenticationException;
import com.akash.pooler_backend.utils.JwtEcSignatureUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleIdentityTokenVerifier {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final String APPLE_JWT_ALGORITHM = "ES256";
    private static final String APPLE_JWK_KEY_TYPE = "EC";
    private static final String APPLE_JWK_CURVE = "P-256";

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private volatile JwkCache jwkCache = new JwkCache(Map.of(), Instant.EPOCH);
    private HttpClient httpClient;

    @PostConstruct
    void initializeHttpClient() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(appProperties.getAuth().getApple().getConnectTimeoutSeconds()))
                .build();
    }

    void replaceJwkCacheForTesting(Map<String, Map<String, Object>> keysById) {
        jwkCache = new JwkCache(keysById, Instant.now().plus(Duration.ofMinutes(5)));
    }

    public AppleIdentityClaims verify(String identityToken) {
        AppProperties.Auth.Apple apple = appProperties.getAuth().getApple();
        if (apple.getClientIds() == null || apple.getClientIds().isBlank()) {
            throw new AuthenticationException(ResponseMessages.APPLE_SIGN_IN_NOT_CONFIGURED);
        }

        try {
            String[] tokenParts = splitJwt(identityToken);
            Map<String, Object> header = decodeJson(tokenParts[0]);
            Map<String, Object> claims = decodeJson(tokenParts[1]);

            String keyId = requiredString(header, "kid");
            String algorithm = requiredString(header, "alg");
            if (!APPLE_JWT_ALGORITHM.equals(algorithm)) {
                throw new AuthenticationException(ResponseMessages.APPLE_ID_TOKEN_INVALID);
            }

            PublicKey publicKey = resolvePublicKey(keyId, apple);
            String signingInput = tokenParts[0] + "." + tokenParts[1];
            if (!JwtEcSignatureUtil.verifyEs256(signingInput, tokenParts[2], publicKey)) {
                throw new AuthenticationException(ResponseMessages.APPLE_ID_TOKEN_INVALID);
            }

            validateClaims(claims, apple);
            return new AppleIdentityClaims(
                    requiredString(claims, "sub"),
                    requiredString(claims, "email").toLowerCase().trim(),
                    booleanClaim(claims.get("email_verified")));
        } catch (AuthenticationException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("appleTokenVerificationInterrupted className={} methodName={}",
                    getClass().getSimpleName(), "verify");
            throw new AuthenticationException(ResponseMessages.APPLE_SIGN_IN_VERIFY_FAILED);
        } catch (Exception exception) {
            log.warn("appleTokenVerificationFailed className={} methodName={} exceptionType={}",
                    getClass().getSimpleName(), "verify", exception.getClass().getSimpleName());
            throw new AuthenticationException(ResponseMessages.APPLE_SIGN_IN_VERIFY_FAILED);
        }
    }

    private void validateClaims(Map<String, Object> claims, AppProperties.Auth.Apple apple) {
        String issuer = requiredString(claims, "iss");
        if (!apple.getIssuer().equals(issuer)) {
            throw new AuthenticationException(ResponseMessages.APPLE_TOKEN_VALIDATION_FAILED);
        }

        if (!hasAcceptedAudience(claims.get("aud"), apple.getClientIds())) {
            throw new AuthenticationException(ResponseMessages.APPLE_TOKEN_VALIDATION_FAILED);
        }

        long now = Instant.now().getEpochSecond();
        long skew = Math.max(0, apple.getAllowedClockSkewSeconds());
        long expiresAt = longClaim(claims.get("exp"));
        long issuedAt = longClaim(claims.get("iat"));
        if (expiresAt <= now - skew || issuedAt > now + skew || !booleanClaim(claims.get("email_verified"))) {
            throw new AuthenticationException(ResponseMessages.APPLE_TOKEN_VALIDATION_FAILED);
        }
    }

    private PublicKey resolvePublicKey(String keyId, AppProperties.Auth.Apple apple)
            throws Exception {
        Map<String, Map<String, Object>> keys = cachedKeys(apple);
        Map<String, Object> jwk = keys.get(keyId);
        if (jwk == null) {
            keys = refreshKeys(apple);
            jwk = keys.get(keyId);
        }
        if (jwk == null) {
            throw new AuthenticationException(ResponseMessages.APPLE_ID_TOKEN_INVALID);
        }

        validateJwk(jwk);
        byte[] x = JwtEcSignatureUtil.base64UrlDecode(requiredString(jwk, "x"));
        byte[] y = JwtEcSignatureUtil.base64UrlDecode(requiredString(jwk, "y"));
        return ecPublicKey(x, y);
    }

    private Map<String, Map<String, Object>> cachedKeys(AppProperties.Auth.Apple apple)
            throws Exception {
        JwkCache cache = jwkCache;
        if (cache.expiresAt().isAfter(Instant.now()) && !cache.keysById().isEmpty()) {
            return cache.keysById();
        }
        return refreshKeys(apple);
    }

    private synchronized Map<String, Map<String, Object>> refreshKeys(AppProperties.Auth.Apple apple)
            throws Exception {
        JwkCache current = jwkCache;
        if (current.expiresAt().isAfter(Instant.now()) && !current.keysById().isEmpty()) {
            return current.keysById();
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apple.getJwksUrl()))
                .timeout(Duration.ofSeconds(apple.getRequestTimeoutSeconds()))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new AuthenticationException(ResponseMessages.APPLE_ID_TOKEN_INVALID);
        }

        Map<String, Object> jwks = objectMapper.readValue(response.body(), MAP_TYPE);
        Object keysValue = jwks.get("keys");
        if (!(keysValue instanceof List<?> keys)) {
            throw new AuthenticationException(ResponseMessages.APPLE_ID_TOKEN_INVALID);
        }

        Map<String, Map<String, Object>> keysById = keys.stream()
                .filter(Map.class::isInstance)
                .map(key -> (Map<?, ?>) key)
                .filter(key -> key.get("kid") instanceof String)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        key -> (String) key.get("kid"),
                        key -> key.entrySet().stream()
                                .filter(entry -> entry.getKey() instanceof String)
                                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                                        entry -> (String) entry.getKey(),
                                        Map.Entry::getValue)),
                        (first, ignored) -> first));
        jwkCache = new JwkCache(keysById, Instant.now().plus(Duration.ofMinutes(apple.getJwksCacheMinutes())));
        return keysById;
    }

    private static void validateJwk(Map<String, Object> jwk) {
        if (!APPLE_JWK_KEY_TYPE.equals(requiredString(jwk, "kty"))
                || !APPLE_JWT_ALGORITHM.equals(requiredString(jwk, "alg"))
                || !APPLE_JWK_CURVE.equals(requiredString(jwk, "crv"))) {
            throw new AuthenticationException(ResponseMessages.APPLE_ID_TOKEN_INVALID);
        }
    }

    private static ECPublicKey ecPublicKey(byte[] x, byte[] y) throws GeneralSecurityException {
        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec ecParameterSpec = parameters.getParameterSpec(ECParameterSpec.class);
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(
                new ECPoint(new BigInteger(1, x), new BigInteger(1, y)),
                ecParameterSpec);
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
    }

    private Map<String, Object> decodeJson(String base64Url) throws java.io.IOException {
        return objectMapper.readValue(JwtEcSignatureUtil.base64UrlDecode(base64Url), MAP_TYPE);
    }

    private static String[] splitJwt(String identityToken) {
        if (identityToken == null || identityToken.isBlank()) {
            throw new AuthenticationException(ResponseMessages.APPLE_ID_TOKEN_INVALID);
        }
        String[] parts = identityToken.trim().split("\\.", -1);
        if (parts.length != 3 || Arrays.stream(parts).anyMatch(String::isBlank)) {
            throw new AuthenticationException(ResponseMessages.APPLE_ID_TOKEN_INVALID);
        }
        return parts;
    }

    private static boolean hasAcceptedAudience(Object audienceClaim, String acceptedClientIds) {
        List<String> accepted = Arrays.stream(acceptedClientIds.split(","))
                .map(String::trim)
                .filter(clientId -> !clientId.isBlank())
                .toList();
        if (audienceClaim instanceof String audience) {
            return accepted.contains(audience);
        }
        if (audienceClaim instanceof List<?> audiences) {
            return audiences.stream().filter(Objects::nonNull).map(Object::toString).anyMatch(accepted::contains);
        }
        return false;
    }

    private static String requiredString(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new AuthenticationException(ResponseMessages.APPLE_TOKEN_VALIDATION_FAILED);
        }
        return value.toString();
    }

    private static long longClaim(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new AuthenticationException(ResponseMessages.APPLE_TOKEN_VALIDATION_FAILED);
        }
    }

    private static boolean booleanClaim(Object value) {
        return value instanceof Boolean bool ? bool : "true".equalsIgnoreCase(String.valueOf(value));
    }

    private record JwkCache(Map<String, Map<String, Object>> keysById, Instant expiresAt) {
    }
}
