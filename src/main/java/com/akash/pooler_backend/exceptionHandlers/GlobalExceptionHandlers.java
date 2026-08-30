package com.akash.pooler_backend.exceptionHandlers;

import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.enums.ErrorCode;
import com.akash.pooler_backend.exception.BaseException;
import com.akash.pooler_backend.utils.TraceContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralised exception handler following the Single Responsibility Principle.
 * All exception-to-HTTP response mapping lives here.
 * @author Akash Kumar
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandlers {

    // ─────────────────────────────────────────────────────────────
    // Domain Exceptions (BaseException subtypes)
    // ─────────────────────────────────────────────────────────────

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(
            BaseException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(ex.getErrorCode(), ex.getMessage(), request, response);
    }

    // ─────────────────────────────────────────────────────────────
    // Spring Security Exceptions
    // ─────────────────────────────────────────────────────────────

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(ErrorCode.ACCESS_DENIED, ex.getMessage(), request, response);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDenied(
            AuthorizationDeniedException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(ErrorCode.ACCESS_DENIED, ex.getMessage(), request, response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(ErrorCode.INVALID_CREDENTIALS, ex.getMessage(), request, response);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked(LockedException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(ErrorCode.ACCOUNT_LOCKED, ex.getMessage(), request, response);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(DisabledException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(ErrorCode.ACCOUNT_INACTIVE, ex.getMessage(), request, response);
    }

    // ─────────────────────────────────────────────────────────────
    // Validation Exceptions
    // ─────────────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request, HttpServletResponse response) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        String traceId = TraceContextUtil.currentCorrelationId(request);
        String errorReferenceId = TraceContextUtil.attachErrorReference(request, response);
        log.warn("API error response errorCode={} status={} path={} traceId={} errorReferenceId={} type={} fields={}",
                ErrorCode.VALIDATION_ERROR.getCode(), HttpStatus.BAD_REQUEST.value(), request.getRequestURI(),
                traceId, errorReferenceId, ex.getClass().getSimpleName(), fieldErrors.keySet());

        ApiResponse<Map<String, String>> body = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .errorCode(ErrorCode.VALIDATION_ERROR.getCode())
                .traceId(traceId)
                .errorReferenceId(errorReferenceId)
                .message(ErrorCode.VALIDATION_ERROR.getDefaultMessage())
                .data(fieldErrors)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(ErrorCode.VALIDATION_ERROR, ex.getMessage(), request, response);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(
            HandlerMethodValidationException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), request, response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(ErrorCode.INVALID_REQUEST, ResponseMessages.invalidParameterType(ex.getName()), request, response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(ErrorCode.INVALID_REQUEST, ResponseMessages.missingRequiredParameter(ex.getParameterName()), request, response);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingPathVariable(
            MissingPathVariableException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(ErrorCode.INVALID_REQUEST, ResponseMessages.missingPathVariable(ex.getVariableName()), request, response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(
            HttpMessageNotReadableException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(ErrorCode.INVALID_REQUEST, ResponseMessages.INVALID_REQUEST_BODY, request, response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(ErrorCode.REQUEST_METHOD_NOT_SUPPORTED, ResponseMessages.REQUEST_METHOD_NOT_SUPPORTED, request, response);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ResponseMessages.UNSUPPORTED_MEDIA_TYPE, request, response);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(ErrorCode.MEDIA_TYPE_NOT_ACCEPTABLE, ResponseMessages.MEDIA_TYPE_NOT_ACCEPTABLE, request, response);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            Exception ex, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.getDefaultMessage(), request, response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(ErrorCode.INVALID_REQUEST, ex.getMessage(), request, response);
    }

    // ─────────────────────────────────────────────────────────────
    // Catch-All
    // ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), request, response, ex);
    }

    // ─────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────

    private ResponseEntity<ApiResponse<Void>> buildResponse(ErrorCode errorCode, String message, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(errorCode, message, request, response, null);
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(ErrorCode errorCode, String message, HttpServletRequest request, HttpServletResponse response, Exception exception) {
        String traceId = TraceContextUtil.currentCorrelationId(request);
        String errorReferenceId = TraceContextUtil.attachErrorReference(request, response);
        if (exception == null) {
            log.warn("apiError className={} methodName={} errorCode={} status={} path={} traceId={} errorReferenceId={}",
                    getClass().getSimpleName(), "buildResponse", errorCode.getCode(), errorCode.getHttpStatus().value(),
                    request.getRequestURI(), traceId, errorReferenceId);
        } else {
            log.error("apiError className={} methodName={} errorCode={} status={} path={} traceId={} errorReferenceId={} type={} origin={}",
                    getClass().getSimpleName(), "buildResponse",
                    errorCode.getCode(), errorCode.getHttpStatus().value(), request.getRequestURI(),
                    traceId, errorReferenceId, exception.getClass().getSimpleName(), origin(exception));
        }

        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .errorCode(errorCode.getCode())
                .traceId(traceId)
                .errorReferenceId(errorReferenceId)
                .message(message != null ? message : errorCode.getDefaultMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    private static String origin(Exception exception) {
        for (StackTraceElement element : exception.getStackTrace()) {
            if (element.getClassName().startsWith("com.akash.pooler_backend")) {
                return element.getClassName() + "." + element.getMethodName() + ":" + element.getLineNumber();
            }
        }
        StackTraceElement[] stackTrace = exception.getStackTrace();
        return stackTrace.length == 0 ? "unknown" : stackTrace[0].getClassName() + "." + stackTrace[0].getMethodName() + ":" + stackTrace[0].getLineNumber();
    }

}
