package com.akash.pooler_backend.interceptors.annotation;

import java.lang.annotation.*;

/**
 * Marks methods whose execution should be recorded in the audit log.
 * Processed by AuditAspect.
 * Usage:
 *   @AuditAction("USER_LOGIN")
 *   @AuditAction(value = "PASSWORD_RESET", includeArgs = false)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditAction {
    String value();
    boolean includeArgs() default false;
}
