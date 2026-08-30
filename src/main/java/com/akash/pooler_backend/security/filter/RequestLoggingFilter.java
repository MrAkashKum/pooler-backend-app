package com.akash.pooler_backend.security.filter;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.utils.TraceContextUtil;
import com.akash.pooler_backend.utils.RequestUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Structured request/response logging.
 * Attaches a correlation ID to every request — extremely useful for
 * tracing mobile client issues across microservices.
 */
@Slf4j
@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String correlationId = TraceContextUtil.resolveOrCreateCorrelationId(request);
        response.setHeader(TraceContextUtil.CORRELATION_ID_HEADER, correlationId);

        long start = System.currentTimeMillis();
        String platform = RequestUtil.getPlatform(request);
        String appVersion = request.getHeader("X-App-Version");

        log.info("→ [{} {}] [correlationId={}] [platform={}] [appVersion={}]",
                request.getMethod(), request.getRequestURI(),
                correlationId, platform, appVersion);

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("← [{} {}] status={} duration={}ms [correlationId={}]",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), duration, correlationId);
            TraceContextUtil.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith(ApiMapping.ACTUATOR_API) || path.startsWith(ApiMapping.H2_CONSOLE_API);
    }
}
