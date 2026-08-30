package com.akash.pooler_backend.interceptors.annotation;

import java.lang.annotation.*;

/**
 * Marks a controller method or class as a public endpoint.
 * Endpoints annotated with @PublicEndpoint:
 *   - Do NOT require an Authorization header
 *   - Are excluded from the AuthInterceptor session check
 *   - Are still subject to rate limiting (@RateLimit)
 * This annotation is informational (the actual security is configured
 * in SecurityConfig.PUBLIC_MATCHERS), but the AuthorizationAspect reads it
 * to skip unnecessary checks and improve observability.
 * Usage:
 *   @PublicEndpoint
 *   @PostMapping("/auth/login")
 *   public ResponseEntity<?> login(...) { ... }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublicEndpoint {
    String description() default "";
}
