package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;
import lombok.Getter;

/**
 * Base exception for the entire application.
 * All custom exceptions extend this class (Open/Closed Principle).
 */
@Getter
public abstract class BaseException extends RuntimeException{

    private final ErrorCode errorCode;
    private final Object[] args;

    protected BaseException(ErrorCode errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    protected BaseException(ErrorCode errorCode, String message, Throwable cause, Object... args) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = args;
    }

    protected BaseException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.args = new Object[0];
    }
}
