package com.akash.pooler_backend.security;

import com.akash.pooler_backend.enums.ErrorCode;
import com.akash.pooler_backend.utils.TraceContextUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  CustomAuthEntryPoint                                            │
 * │                                                                  │
 * │  Invoked by Spring Security when an unauthenticated request     │
 * │  reaches a protected resource (i.e., no valid JWT / session).   │
 * │                                                                  │
 * │  Default Spring behaviour: redirects to /login (HTML form)      │
 * │  This replaces it with: JSON error response for mobile clients  │
 * │                                                                  │
 * │  Triggered when:                                                 │
 * │    • No Authorization header present                            │
 * │    • JWT filter cleared SecurityContext (expired/invalid token) │
 * │    • Session token was missing or revoked                       │
 * │                                                                  │
 * │  HTTP Response:  401 Unauthorized                               │
 * │  Body:  { success, errorCode, message, path, timestamp }        │
 * │                                                                  │
 * │  Android/iOS clients switch on `errorCode`:                     │
 * │    AUTH-012 → redirect to login screen                          │
 * │    AUTH-002 → try token refresh before logging out              │
 * └─────────────────────────────────────────────────────────────────┘
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;


    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        // Determine the most specific error code based on request state
        ErrorCode errorCode = resolveErrorCode(request, authException);
        String traceId = TraceContextUtil.currentCorrelationId(request);
        String errorReferenceId = TraceContextUtil.attachErrorReference(request, response);

        log.warn("Security error response errorCode={} status={} path={} traceId={} errorReferenceId={} type={}",
                errorCode.getCode(), HttpStatus.UNAUTHORIZED.value(), request.getRequestURI(),
                traceId, errorReferenceId, authException.getClass().getSimpleName());

        // Build mobile-friendly JSON body
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("errorCode", errorCode.getCode());
        body.put("traceId", traceId);
        body.put("errorReferenceId", errorReferenceId);
        body.put("message", errorCode.getDefaultMessage());
        body.put("path", request.getRequestURI());
        body.put("timestamp", Instant.now().toString());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(TraceContextUtil.CORRELATION_ID_HEADER, traceId);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getOutputStream(), body);
    }

    // ─── Private helpers ──────────────────────────────────────────────

    /**
     * Picks the most descriptive ErrorCode:
     *   - If a specific code was stored as a request attribute by a filter → use it
     *   - If the request had an Authorization header → token was bad or expired
     *   - Otherwise → header was missing entirely
     */
    private ErrorCode resolveErrorCode(HttpServletRequest request, AuthenticationException ex) {
        // Check if a filter stored a specific error code on the request
        Object stored = request.getAttribute("auth.errorCode");
        if (stored instanceof ErrorCode ec) return ec;

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
            if (msg.contains("expired")) return ErrorCode.TOKEN_EXPIRED;
            if (msg.contains("revoked")) return ErrorCode.TOKEN_REVOKED;
            return ErrorCode.TOKEN_INVALID;
        }
        return ErrorCode.MISSING_AUTH_HEADER;
    }
}
