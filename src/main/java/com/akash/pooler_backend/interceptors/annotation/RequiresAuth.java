package com.akash.pooler_backend.interceptors.annotation;

import com.akash.pooler_backend.enums.Role;

import java.lang.annotation.*;

/**
 * Method-level annotation for declarative role-based access control.
 * Processed by AuthorizationAspect.
 * Usage:
 *   @RequiresAuth                           // any authenticated user
 *   @RequiresAuth(roles = Role.ROLE_ADMIN)  // admin only
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresAuth {
    Role[] roles() default {};
    boolean requireActiveSession() default false;
}
