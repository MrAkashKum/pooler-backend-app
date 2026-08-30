package com.akash.pooler_backend.aspect;

import com.akash.pooler_backend.exception.RateLimitException;
import com.akash.pooler_backend.interceptors.annotation.RateLimit;
import com.akash.pooler_backend.utils.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AOP Aspect — enforces {@link @RateLimit} on annotated controller methods.
 *
 * Implementation: in-memory sliding window counter per (IP + endpoint) key.
 * For production at scale, replace with Redis (e.g. Redisson) to share
 * state across multiple pod replicas.
 *
 * Designed as a Strategy Pattern — the rate-limiting algorithm can be swapped
 * by changing the implementation behind the RateLimiter interface without
 * touching controllers.
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    /** key → (windowStartEpochMs, requestCount) */
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupEpochMs = new AtomicLong(0);

    @Value("${rate-limit.max-entries:10000}")
    private int maxEntries;

    @Value("${rate-limit.cleanup-interval-seconds:60}")
    private long cleanupIntervalSeconds;

    @Before("@annotation(rateLimit)")
    public void enforce(JoinPoint jp, RateLimit rateLimit) {
        HttpServletRequest req;
        try {
            req = RequestUtil.currentRequest();
        } catch (Exception e) {
            return; // can't enforce without request context — fail open
        }

        String ip = RequestUtil.getClientIp(req);
        String endpoint = buildEndpointKey(jp);
        String key = rateLimit.key().isBlank() ? ip + ":" + endpoint : rateLimit.key() + ":" + ip;

        long windowMs = rateLimit.windowSeconds() * 1000L;
        long now = Instant.now().toEpochMilli();
        cleanupExpiredCounters(now);
        if (!counters.containsKey(key) && counters.size() >= Math.max(1, maxEntries)) {
            log.warn("Rate limiter capacity reached; rejecting request for endpoint={}", endpoint);
            throw new RateLimitException();
        }

        WindowCounter counter = counters.compute(key, (k, existing) -> {
            if (existing == null || (now - existing.windowStart) > windowMs) {
                return new WindowCounter(now, windowMs); // new window
            }
            return existing;
        });

        int count = counter.count.incrementAndGet();

        if (count > rateLimit.maxRequests()) {
            log.warn("Rate limit exceeded: endpoint={} count={}/{}",
                    endpoint, count, rateLimit.maxRequests());
            throw new RateLimitException();
        }

        log.debug("Rate check: endpoint={} count={}/{} window={}s",
                endpoint, count, rateLimit.maxRequests(), rateLimit.windowSeconds());
    }

    private String buildEndpointKey(JoinPoint jp) {
        Method method = ((MethodSignature) jp.getSignature()).getMethod();
        return jp.getTarget().getClass().getSimpleName() + "." + method.getName();
    }

    private void cleanupExpiredCounters(long now) {
        long intervalMs = Math.max(1, cleanupIntervalSeconds) * 1000L;
        long previous = lastCleanupEpochMs.get();
        if ((now - previous) < intervalMs || !lastCleanupEpochMs.compareAndSet(previous, now)) {
            return;
        }
        counters.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    // ── Inner value class ─────────────────────────────────────────────
    private static class WindowCounter {
        final long windowStart;
        final long windowMs;
        final AtomicInteger count = new AtomicInteger(0);
        WindowCounter(long windowStart, long windowMs) {
            this.windowStart = windowStart;
            this.windowMs = windowMs;
        }
        boolean isExpired(long now) {
            return (now - windowStart) > windowMs;
        }
    }
}
