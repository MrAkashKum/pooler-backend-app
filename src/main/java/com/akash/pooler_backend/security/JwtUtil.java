package com.akash.pooler_backend.security;

import com.akash.pooler_backend.config.AppProperties;
import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.TokenType;
import com.akash.pooler_backend.exception.TokenExpiredException;
import com.akash.pooler_backend.exception.TokenInvalidException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Stateless JWT utility — all token operations.
 *
 * Token anatomy:
 *   Header: { alg: HS256 }
 *   Payload: { sub, iss, aud, iat, exp, jti, role, tokenType }
 *   Signature: HMAC-SHA256(secret)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final AppProperties props;

    // ── Token Generation ─────────────────────────────────────────────

    public String generateAccessToken(PbUserEntity pbUserEntity) {
        return buildToken(pbUserEntity, TokenType.ACCESS, props.getJwt().getAccessTokenExpiryMs(),
                Map.of("role", pbUserEntity.getRole().name()));
    }

    public String generateRefreshToken(PbUserEntity pbUserEntity) {
        return buildToken(pbUserEntity, TokenType.REFRESH, props.getJwt().getRefreshTokenExpiryMs(), Map.of());
    }

    public String generateSessionToken(PbUserEntity pbUserEntity) {
        return buildToken(pbUserEntity, TokenType.SESSION, props.getJwt().getSessionTokenExpiryMs(), Map.of());
    }

    private String buildToken(PbUserEntity user, TokenType type, long expiryMs, Map<String, Object> extra) {
        Instant now = Instant.now();
        JwtBuilder builder = Jwts.builder()
                .subject(user.getEntityId())
                .issuer(props.getJwt().getIssuer())
                .audience().add(props.getJwt().getAudience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiryMs)))
                .id(UUID.randomUUID().toString())
                .claim("tokenType", type.name())
                .signWith(secretKey());

        extra.forEach(builder::claim);
        return builder.compact();
    }

    // ── Token Validation ─────────────────────────────────────────────

    public Claims validateAndExtract(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            log.debug("JWT expired");
            throw new TokenExpiredException(ResponseMessages.JWT_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT invalid");
            throw new TokenInvalidException(ResponseMessages.JWT_TOKEN_INVALID);
        }
    }

    public boolean isTokenValid(String token, PbUserEntity pbUserEntity) {
        try {
            Claims claims = validateAndExtract(token);
            return claims.getSubject().equals(pbUserEntity.getEntityId()) && !isExpired(claims);
        } catch (Exception e) {
            return false;
        }
    }

    // ── Claims Extraction ────────────────────────────────────────────

    public String extractSubject(String token)  { return validateAndExtract(token).getSubject(); }
    public String extractTokenType(String token) { return validateAndExtract(token).get("tokenType", String.class); }
    public String extractJti(String token)       { return validateAndExtract(token).getId(); }
    public Date   extractExpiration(String token){ return validateAndExtract(token).getExpiration(); }

    public long getAccessTokenExpiryMs()   { return props.getJwt().getAccessTokenExpiryMs(); }
    public long getRefreshTokenExpiryMs()  { return props.getJwt().getRefreshTokenExpiryMs(); }
    public long getSessionTokenExpiryMs()  { return props.getJwt().getSessionTokenExpiryMs(); }

    // ── Private ──────────────────────────────────────────────────────

    private boolean isExpired(Claims claims) {
        return claims.getExpiration().before(Date.from(Instant.now()));
    }

    private SecretKey secretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(props.getJwt().getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
