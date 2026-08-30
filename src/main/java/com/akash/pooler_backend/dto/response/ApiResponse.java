package com.akash.pooler_backend.dto.response;

import com.akash.pooler_backend.constants.ResponseMessages;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Generic API response wrapper — follows the Builder pattern.
 * All endpoints return this envelope for a consistent contract.
 *
 * @param <T> payload type
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Builder.Default
    private boolean success = true;

    private String message;
    private String errorCode;
    private String traceId;
    private String errorReferenceId;
    private T data;
    private String path;

    @Builder.Default
    private Instant timestamp = Instant.now();

    // ─── Static factory helpers ──────────────────────────────────

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder().success(true).message(message).data(data).build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder().success(true).message(ResponseMessages.RESOURCE_CREATED).data(data).build();
    }

    public static ApiResponse<Void> noContent(String message) {
        return ApiResponse.<Void>builder().success(true).message(message).build();
    }
    public static ApiResponse<Void> message(String msg) {
        return ApiResponse.<Void>builder().success(true).message(msg).build(); }
}
