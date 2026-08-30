package com.akash.pooler_backend.aspect;

import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.interceptors.annotation.AuditAction;
import com.akash.pooler_backend.service.AuditService;
import com.akash.pooler_backend.utils.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect — persists audit entries for annotated methods.
 * Runs asynchronously so it never blocks the request thread.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @AfterReturning("@annotation(auditAction)")
    public void audit(JoinPoint jp, AuditAction auditAction) {
        try {
            String entity = resolveUserId();
            String ip   = resolveIp();
            String details = auditAction.includeArgs()
                    ? "argsRedacted=true,argCount=" + jp.getArgs().length
                    : null;

            auditService.log(entity, auditAction.value(), details, ip);
        } catch (Exception e) {
            log.warn("Audit logging failed: type={}", e.getClass().getSimpleName());
        }
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof PbUserEntity pbUserEntity) {
            return pbUserEntity.getEntityId();
        }
        return null;
    }

    private String resolveIp() {
        try {
            HttpServletRequest req = RequestUtil.currentRequest();
            return RequestUtil.getClientIp(req);
        } catch (Exception e) {
            return "unknown error";
        }
    }
}
