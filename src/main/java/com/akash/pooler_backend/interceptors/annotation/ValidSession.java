package com.akash.pooler_backend.interceptors.annotation;

import java.lang.annotation.*;

/**
 * Marks an endpoint that requires a valid persisted SessionToken in addition
 * to the standard JWT Bearer token. This enables dual-layer authentication
 * which is strongly recommended for high-security mobile app operations
 * (e.g. changing password, deleting account, viewing sensitive data).
 *
 * How it works:
 *  1. JwtAuthenticationFilter validates the JWT signature (cryptographic)
 *  2. AuthInterceptor validates the X-Session-Token header against the DB (revocable)
 *  3. @ValidSession AOP aspect enforces this annotation at the method level
 *
 * Mobile clients MUST send:
 *   Authorization: Bearer <accessToken>
 *   X-Session-Token: <sessionToken>
 *
 * Usage:
 *   @ValidSession
 *   @PutMapping("/me/change-password")
 *   public ResponseEntity<?> changePassword(...) { ... }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidSession {

    /**
     * Whether to refresh the session's lastAccessedAt timestamp on each call.
     * Default: true (sliding session window)
     */
    boolean sliding() default true;

    /**
     * Human-readable description of why this session check is required.
     * Used in logs and error messages.
     */
    String reason() default "Sensitive operation requires active session";

}
