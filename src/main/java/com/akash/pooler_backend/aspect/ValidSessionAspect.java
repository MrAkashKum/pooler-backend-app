package com.akash.pooler_backend.aspect;

import com.akash.pooler_backend.exception.SessionExpiredException;
import com.akash.pooler_backend.interceptors.annotation.ValidSession;
import com.akash.pooler_backend.repository.PbUserSessionRepository;
import com.akash.pooler_backend.utils.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect — enforces {@link @ValidSession} on annotated controller methods.
 *
 * This is the programmatic counterpart of AuthInterceptor's header check.
 * The interceptor handles all /api/** routes broadly; this aspect enforces
 * @ValidSession at the specific method level for maximum granularity.
 *
 * Execution order:
 *   JwtAuthenticationFilter → AuthInterceptor → ValidSessionAspect → Controller
 *
 * If X-Session-Token is missing or invalid on a @ValidSession endpoint,
 * a SessionExpiredException is thrown which maps to HTTP 401 via GlobalExceptionHandler.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ValidSessionAspect {

    private final PbUserSessionRepository pbUserSessionRepository;
    private static final String SESSION_HEADER = "X-Session-Token";

    @Before("@annotation(validSession)")
    public void enforceValidSession(JoinPoint jp, ValidSession validSession) {
        HttpServletRequest req;
        try {
            req = RequestUtil.currentRequest();
        } catch (Exception e) {
            log.warn("ValidSessionAspect: could not obtain current request — skipping check");
            return;
        }

        String sessionToken = req.getHeader(SESSION_HEADER);

        if (sessionToken == null || sessionToken.isBlank()) {
            log.warn("@ValidSession required but X-Session-Token header missing. " +
                    "Reason: {} | URI: {}", validSession.reason(), req.getRequestURI());
            throw new SessionExpiredException();
        }

        var session = pbUserSessionRepository.findByToken(sessionToken)
                .orElseThrow(() -> {
                    log.warn("@ValidSession: token not found in DB. URI={}", req.getRequestURI());
                    return new SessionExpiredException();
                });

        if (!session.isActive()) {
            log.warn("@ValidSession: session inactive/expired. userId={} URI={}",
                    session.getEntityId(), req.getRequestURI());
            throw new SessionExpiredException();
        }

        // Sliding session — update last accessed timestamp
        if (validSession.sliding()) {
            session.touch();
            pbUserSessionRepository.save(session);
        }

        log.debug("@ValidSession ✔ userId={} URI={}", session.getEntityId(), req.getRequestURI());
    }
}
