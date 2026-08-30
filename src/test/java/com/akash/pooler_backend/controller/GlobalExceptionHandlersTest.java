package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.enums.ErrorCode;
import com.akash.pooler_backend.exception.AccountLockedException;
import com.akash.pooler_backend.exceptionHandlers.GlobalExceptionHandlers;
import com.akash.pooler_backend.utils.TraceContextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlersTest {

    private final GlobalExceptionHandlers handler = new GlobalExceptionHandlers();

    @AfterEach
    void tearDown() {
        TraceContextUtil.clear();
    }

    @Test
    void handleBaseExceptionUsesDomainErrorCode() {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<ApiResponse<Void>> result =
                handler.handleBaseException(new AccountLockedException(), request, response);

        assertError(result, HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_LOCKED,
                ErrorCode.ACCOUNT_LOCKED.getDefaultMessage(), request, response);
    }

    @Test
    void handleMissingServletRequestParameterReturnsCleanValidationError() {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<ApiResponse<Void>> result = handler.handleMissingServletRequestParameter(
                new MissingServletRequestParameterException("destination", "String"), request, response);

        assertError(result, HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST,
                "Missing required parameter: destination", request, response);
    }

    @Test
    void handleMethodNotSupportedReturnsMethodNotAllowed() {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<ApiResponse<Void>> result = handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("PATCH", List.of("GET", "POST")),
                request,
                response);

        assertError(result, HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.REQUEST_METHOD_NOT_SUPPORTED,
                ErrorCode.REQUEST_METHOD_NOT_SUPPORTED.getDefaultMessage(), request, response);
    }

    @Test
    void handleMediaTypeNotSupportedReturnsUnsupportedMediaType() {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<ApiResponse<Void>> result = handler.handleMediaTypeNotSupported(
                new HttpMediaTypeNotSupportedException("text/plain is not supported"), request, response);

        assertError(result, HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                ErrorCode.UNSUPPORTED_MEDIA_TYPE.getDefaultMessage(), request, response);
    }

    @Test
    void handleMediaTypeNotAcceptableReturnsNotAcceptable() {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<ApiResponse<Void>> result = handler.handleMediaTypeNotAcceptable(
                new HttpMediaTypeNotAcceptableException("application/xml is not acceptable"), request, response);

        assertError(result, HttpStatus.NOT_ACCEPTABLE, ErrorCode.MEDIA_TYPE_NOT_ACCEPTABLE,
                ErrorCode.MEDIA_TYPE_NOT_ACCEPTABLE.getDefaultMessage(), request, response);
    }

    @Test
    void handleResourceNotFoundReturnsNotFound() {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<ApiResponse<Void>> result = handler.handleResourceNotFound(
                new NoResourceFoundException(HttpMethod.GET, "/missing", "No static resource"),
                request,
                response);

        assertError(result, HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND,
                ErrorCode.RESOURCE_NOT_FOUND.getDefaultMessage(), request, response);
    }

    @Test
    void handleGenericDoesNotExposeInternalExceptionMessage() {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<ApiResponse<Void>> result =
                handler.handleGeneric(new IllegalStateException("sensitive implementation detail"), request, response);

        assertError(result, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                ErrorCode.INTERNAL_ERROR.getDefaultMessage(), request, response);
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
        request.addHeader(TraceContextUtil.CORRELATION_ID_HEADER, "mobile-trace-0001");
        return request;
    }

    private static void assertError(
            ResponseEntity<ApiResponse<Void>> result,
            HttpStatus status,
            ErrorCode errorCode,
            String message,
            MockHttpServletRequest request,
            MockHttpServletResponse response) {
        assertEquals(status, result.getStatusCode());
        ApiResponse<Void> body = result.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals(errorCode.getCode(), body.getErrorCode());
        assertEquals(message, body.getMessage());
        assertEquals(request.getRequestURI(), body.getPath());
        assertEquals("mobile-trace-0001", body.getTraceId());
        assertNotNull(body.getErrorReferenceId());
        assertEquals(body.getErrorReferenceId(), response.getHeader(TraceContextUtil.ERROR_REFERENCE_ID_HEADER));
    }
}
