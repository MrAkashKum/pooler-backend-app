package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

/**
 * @author Akash Kumar
 */
public class InvalidCoordinatesException extends BaseException {
    public InvalidCoordinatesException() {
        super(ErrorCode.INVALID_COORDINATES);
    }

    public InvalidCoordinatesException(String message) {
        super(ErrorCode.INVALID_COORDINATES, message);
    }
}
