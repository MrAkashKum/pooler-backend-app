package com.akash.pooler_backend.aspect;

import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.Role;
import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.exception.AuthenticationException;
import com.akash.pooler_backend.interceptors.annotation.RequiresAuth;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * AOP Aspect — enforces {@link RequiresAuth} programmatic role checks.
 *
 * Complements Spring Security's @PreAuthorize:
 *   - @PreAuthorize is annotation-config declarative (good for most cases)
 *   - @RequiresAuth is AOP-driven and evaluated at method entry (finer control)
 *
 * Hierarchy:
 *   JwtAuthenticationFilter → SecurityConfig rules → @RequiresAuth aspect → method
 */
@Slf4j
@Aspect
@Component
public class AuthorizationAspect {

    @Before("@annotation(requiresAuth)")
    public void enforce(JoinPoint jp, RequiresAuth requiresAuth) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof PbUserEntity pbUserEntity)) {
            throw new AuthenticationException(ResponseMessages.AUTHENTICATION_REQUIRED);
        }

        Role[] requiredRoles = requiresAuth.roles();
        if (requiredRoles.length == 0) {
            // No specific roles required — just needs to be authenticated
            return;
        }

        Set<Role> allowed = Set.of(requiredRoles);
        if (!allowed.contains(pbUserEntity.getRole())) {
            log.warn("@RequiresAuth denied: entityId={} role={} required={}",
                    pbUserEntity.getEntityId(), pbUserEntity.getRole(), Arrays.toString(requiredRoles));
            throw new AuthenticationException(
                    "Role " + pbUserEntity.getRole() + " is not authorised for this operation. " +
                            "Required: " + Arrays.toString(requiredRoles));
        }

        log.debug("@RequiresAuth ✔ entityId={} role={}", pbUserEntity.getEntityId(), pbUserEntity.getRole());
    }
}
