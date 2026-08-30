package com.akash.pooler_backend.aspect;

import com.akash.pooler_backend.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Cross-cutting concern: method-level timing for all service classes.
 * Logs WARN when any service method exceeds 500ms.
 */
@Slf4j
@Aspect
@Component
public class ServiceLoggingAspect {

    @Pointcut("execution(* com.akash.pooler_backend.service.impl.*.*(..))")
    public void serviceMethods() {
        // Pointcut signature for service implementation methods.
    }

    @Around("serviceMethods()")
    public Object logExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String method = pjp.getSignature().toLongString();
        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed > 500) {
                log.warn("serviceFlow classMethod=\"{}\" outcome=slow durationMs={}", method, elapsed);
            } else {
                log.debug("serviceFlow classMethod=\"{}\" outcome=success durationMs={}", method, elapsed);
            }
            return result;
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - start;
            if (t instanceof BaseException) {
                log.warn("serviceFlow classMethod=\"{}\" outcome=rejected exceptionType={} durationMs={}",
                        method, t.getClass().getSimpleName(), elapsed);
            } else {
                log.error("serviceFlow classMethod=\"{}\" outcome=failed exceptionType={} durationMs={}",
                        method, t.getClass().getSimpleName(), elapsed, t);
            }
            throw t;
        }
    }
}
