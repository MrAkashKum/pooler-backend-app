package com.akash.pooler_backend.security.filter;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.exception.TokenExpiredException;
import com.akash.pooler_backend.exception.TokenInvalidException;
import com.akash.pooler_backend.repository.PbUserRepository;
import com.akash.pooler_backend.security.JwtUtil;
import com.akash.pooler_backend.utils.RequestUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  JWT Authentication Filter                                       │
 * │  Runs ONCE per request — extends OncePerRequestFilter            │
 * │                                                                  │
 * │  Request flow:                                                   │
 * │  ① Extract "Bearer <token>" from Authorization header           │
 * │  ② Validate JWT signature + expiry via JwtUtil                  │
 * │  ③ Load UserDetails from DB (email from JWT claim)              │
 * │  ④ Verify token is valid for that specific user                 │
 * │  ⑤ Set Authentication in SecurityContext → request is authed    │
 * │                                                                  │
 * │  If header is absent  → skip (public route or 401 downstream)   │
 * │  If token is invalid  → clear context, continue chain (→ 401)   │
 * │  If token is expired  → clear context, continue chain (→ 401)   │
 * └─────────────────────────────────────────────────────────────────┘
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final PbUserRepository userRepository;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // ─── ① Extract Bearer token ──────────────────────────────────────
        final String token = RequestUtil.extractBearerToken(request);

        if (StringUtils.isBlank(token)) {
            // No Authorization header — let the chain decide (public / 401)
            filterChain.doFilter(request, response);
            return;
        }

        // ─── ② + ③ Parse + load user ────────────────────────────────────
        try {
            final String entityId = jwtUtil.extractSubject(token);

            if (StringUtils.isNotBlank(entityId)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                PbUserEntity user = userRepository.findByEntityId(entityId)
                        .orElseThrow(() -> new TokenInvalidException(ResponseMessages.JWT_TOKEN_INVALID));
                if (!user.isEnabled() || !user.isAccountNonLocked()) {
                    throw new TokenInvalidException(ResponseMessages.JWT_TOKEN_INVALID);
                }

                // ─── ④ Validate token against this specific user ─────────
                if (jwtUtil.isTokenValid(token, user)) {

                    // ─── ⑤ Build Spring Security auth object ─────────────
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    user.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("JWT auth OK — userId={} role={}", user.getEntityId(), user.getRole());
                } else {
                    log.debug("JWT validation failed for resolved user");
                }
            }

        } catch (TokenExpiredException ex) {
            log.debug("JWT expired");
            SecurityContextHolder.clearContext();
            // Don't write response here — let Spring Security's AuthEntryPoint handle it

        } catch (TokenInvalidException ex) {
            log.debug("JWT invalid");
            SecurityContextHolder.clearContext();

        } catch (Exception ex) {
            log.error("Unexpected JWT filter error: {}", ex.getClass().getSimpleName());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Skip this filter entirely for paths that never carry tokens.
     * Reduces DB load — UserDetails won't be loaded for public routes.
     */
    /**
     * Skip JWT validation for paths that never carry tokens.
     * NOTE: getServletPath() returns path WITHOUT the context-path (/api/v1).
     * So  public/health is "/public/health".
     *
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith(ApiMapping.H2_CONSOLE_API)
                || path.equals(ApiMapping.PUBLIC_HEALTH_SERVLET_PATH)
                || path.equals(ApiMapping.PUBLIC_VERSION_SERVLET_PATH);
    }
}
