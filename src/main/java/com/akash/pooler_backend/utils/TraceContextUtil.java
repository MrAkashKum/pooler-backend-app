package com.akash.pooler_backend.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class TraceContextUtil {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String ERROR_REFERENCE_ID_HEADER = "X-Error-Reference-ID";
    public static final String CORRELATION_ID_MDC_KEY = "x_request_id";
    public static final String ERROR_REFERENCE_ID_MDC_KEY = "error_reference_id";
    public static final String CORRELATION_ID_ATTRIBUTE = "trace.correlationId";
    public static final String ERROR_REFERENCE_ID_ATTRIBUTE = "trace.errorReferenceId";

    private static final Pattern SAFE_EXTERNAL_ID = Pattern.compile("^[A-Za-z0-9._:-]{8,128}$");
    private static final DateTimeFormatter ERROR_ID_DATE = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final SecureRandom RANDOM = new SecureRandom();

    private TraceContextUtil() {
    }

    public static String resolveOrCreateCorrelationId(HttpServletRequest request) {
        Object existing = request.getAttribute(CORRELATION_ID_ATTRIBUTE);
        if (existing instanceof String id && !id.isBlank()) {
            return id;
        }

        String headerValue = request.getHeader(CORRELATION_ID_HEADER);
        String correlationId = isSafeExternalId(headerValue) ? headerValue.trim() : UUID.randomUUID().toString();
        request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId);
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        return correlationId;
    }

    public static String currentCorrelationId(HttpServletRequest request) {
        Object existing = request.getAttribute(CORRELATION_ID_ATTRIBUTE);
        if (existing instanceof String id && !id.isBlank()) {
            return id;
        }
        String mdcValue = MDC.get(CORRELATION_ID_MDC_KEY);
        if (mdcValue != null && !mdcValue.isBlank()) {
            return mdcValue;
        }
        return resolveOrCreateCorrelationId(request);
    }

    public static String attachErrorReference(HttpServletRequest request, HttpServletResponse response) {
        Object existing = request.getAttribute(ERROR_REFERENCE_ID_ATTRIBUTE);
        String referenceId = existing instanceof String id && !id.isBlank() ? id : createErrorReferenceId();
        request.setAttribute(ERROR_REFERENCE_ID_ATTRIBUTE, referenceId);
        MDC.put(ERROR_REFERENCE_ID_MDC_KEY, referenceId);
        response.setHeader(ERROR_REFERENCE_ID_HEADER, referenceId);
        return referenceId;
    }

    public static String createErrorReferenceId() {
        long randomNumber = Math.floorMod(RANDOM.nextLong(), 1_000_000_000_000L);
        return "ERR-" + ERROR_ID_DATE.format(Instant.now()) + "-" + String.format(Locale.ROOT, "%012d", randomNumber);
    }

    public static void clear() {
        MDC.remove(CORRELATION_ID_MDC_KEY);
        MDC.remove(ERROR_REFERENCE_ID_MDC_KEY);
    }

    private static boolean isSafeExternalId(String value) {
        return value != null && SAFE_EXTERNAL_ID.matcher(value.trim()).matches();
    }
}
