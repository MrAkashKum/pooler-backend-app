package com.akash.pooler_backend.interceptors.annotation;

import java.lang.annotation.*;

/**
 * Marks a controller method as rate-limited.
 * Processed by RateLimitAspect.
 * Usage:
 *   @RateLimit(maxRequests = 5, windowSeconds = 60)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    int maxRequests() default 10;
    int windowSeconds() default 60;
    String key() default ""; /* empty = use client IP */
}
