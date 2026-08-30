package com.akash.pooler_backend.security;

import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.ErrorCode;
import com.akash.pooler_backend.utils.TraceContextUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  CustomAccessDeniedHandler                                       │
 * │                                                                  │
 * │  Invoked by Spring Security when an AUTHENTICATED user tries    │
 * │  to access a resource they are not authorised for.              │
 * │                                                                  │
 * │  Difference from CustomAuthEntryPoint:                          │
 * │    AuthEntryPoint  → user is NOT authenticated   (401)          │
 * │    AccessDenied    → user IS authenticated, wrong role (403)    │
 * │                                                                  │
 * │  Triggered when:                                                 │
 * │    • USER tries to call /api/v1/admin/** (ADMIN only)           │
 * │    • USER tries to call /api/v1/moderator/** (MOD/ADMIN only)  │
 * │    • @PreAuthorize check fails                                   │
 * │    • @RequiresAuth(roles = ADMIN) fails                         │
 * │                                                                  │
 * │  HTTP Response:  403 Forbidden                                  │
 * │  Body: { success, errorCode "AUTH-008", message, path, role }   │
 * │                                                                  │
 * │  Android/iOS clients:                                            │
 * │    AUTH-008 → show "You don't have permission" UI message       │
 * │    Do NOT redirect to login (user IS logged in)                 │
 * └─────────────────────────────────────────────────────────────────┘
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {

        // Extract authenticated user's role if available (helpful for debugging)
        String userRole = resolveUserRole(request);

        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        String traceId = TraceContextUtil.currentCorrelationId(request);
        String errorReferenceId = TraceContextUtil.attachErrorReference(request, response);

        log.warn("Security error response errorCode={} status={} path={} traceId={} errorReferenceId={} role={}",
                errorCode.getCode(), HttpStatus.FORBIDDEN.value(), request.getRequestURI(),
                traceId, errorReferenceId, userRole);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success",    false);
        body.put("errorCode",  errorCode.getCode());
        body.put("traceId", traceId);
        body.put("errorReferenceId", errorReferenceId);
        body.put("message",    errorCode.getDefaultMessage());
        body.put("path",       request.getRequestURI());
        body.put("timestamp",  Instant.now().toString());
        // Include role so mobile dev can diagnose permission issues
        if (userRole != null) {
            body.put("currentRole", userRole);
        }

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setHeader(TraceContextUtil.CORRELATION_ID_HEADER, traceId);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getOutputStream(), body);
    }

    // ─── Private helpers ──────────────────────────────────────────────

    private String resolveUserRole(HttpServletRequest request) {
        try {
            var auth = org.springframework.security.core.context
                    .SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof PbUserEntity pbUserEntity) {
                return pbUserEntity.getRole().name();
            }
        } catch (RuntimeException exception) {
            log.debug("accessDeniedRoleResolveSkipped exceptionType={}", exception.getClass().getSimpleName());
        }
        return null;
    }
}
