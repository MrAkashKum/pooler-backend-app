package com.akash.pooler_backend.interceptors.annotation;

import java.lang.annotation.*;

/**
 * Injects the authenticated User into controller method parameters.
 * Usage:
 *   public ResponseEntity<?> getProfile(@CurrentUser User user) { ... }
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {}
