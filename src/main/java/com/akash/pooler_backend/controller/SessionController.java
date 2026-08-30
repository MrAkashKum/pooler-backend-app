package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.dto.response.SessionListResponse;
import com.akash.pooler_backend.dto.response.TokenInfoResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.entity.PbUserSessionEntity;
import com.akash.pooler_backend.enums.TokenStatus;
import com.akash.pooler_backend.exception.SessionExpiredException;
import com.akash.pooler_backend.interceptors.annotation.CurrentUser;
import com.akash.pooler_backend.interceptors.annotation.ValidSession;
import com.akash.pooler_backend.repository.PbUserSessionRepository;
import com.akash.pooler_backend.security.JwtUtil;
import com.akash.pooler_backend.utils.RequestUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Session management endpoints — essential for mobile apps.
 *
 * Lets users see which devices are logged in and revoke individual sessions.
 * Uses @ValidSession on sensitive operations (revoking other sessions).
 */
@RestController
@RequestMapping(ApiMapping.SESSIONS_API)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Sessions", description = "Device session management for mobile apps")
public class SessionController {

    private final PbUserSessionRepository sessionTokenRepo;
    private final JwtUtil jwtUtil;

    @GetMapping
    @Operation(summary = "List all active sessions (logged-in devices)")
    public ResponseEntity<ApiResponse<List<SessionListResponse>>> listSessions(
            @CurrentUser PbUserEntity pbUserEntity,
            HttpServletRequest req) {
        String currentSessionToken = req.getHeader("X-Session-Token");
        List<PbUserSessionEntity> sessions = sessionTokenRepo
                .findAllByEntityIdAndStatus(pbUserEntity.getEntityId(), TokenStatus.ACTIVE);

        List<SessionListResponse> response = new ArrayList<>();
        for (PbUserSessionEntity s : sessions) {
            if (s.isActive()) {
                SessionListResponse from = SessionListResponse.from(s,
                        currentSessionToken != null && currentSessionToken.equals(s.getToken()));
                response.add(from);
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping(ApiMapping.SESSION_ID)
    @ValidSession(reason = "Revoking another device session requires an active session")
    @Operation(summary = "Revoke a specific session (log out a device)")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @CurrentUser PbUserEntity pbUserEntity,
            @PathVariable Long sessionId) {
        PbUserSessionEntity session = sessionTokenRepo.findById(sessionId)
                .orElseThrow(SessionExpiredException::new);

        // Security: users can only revoke their own sessions
        if (!session.getEntityId().equals(pbUserEntity.getEntityId())) {
            throw new SessionExpiredException();
        }

        session.setStatus(TokenStatus.REVOKED);
        sessionTokenRepo.save(session);

        return ResponseEntity.ok(ApiResponse.message(ResponseMessages.SESSION_REVOKED));
    }

    @GetMapping(ApiMapping.TOKEN_INFO)
    @Operation(summary = "Decode access token metadata (expiry, role, type)")
    public ResponseEntity<ApiResponse<TokenInfoResponse>> tokenInfo(HttpServletRequest req) {
        String token = RequestUtil.extractBearerToken(req);
        Date expiresAt = jwtUtil.extractExpiration(token);
        long expiresInSeconds = (expiresAt.getTime() - Instant.now().toEpochMilli()) / 1000;

        TokenInfoResponse info = TokenInfoResponse.builder()
                .subject(jwtUtil.extractSubject(token))
                .tokenType(jwtUtil.extractTokenType(token))
                .expiresAt(expiresAt)
                .expired(expiresInSeconds < 0)
                .expiresInSeconds(Math.max(0, expiresInSeconds))
                .build();

        return ResponseEntity.ok(ApiResponse.ok(info));
    }
}
