package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

/**
 * @author Akash Kumar
 */
public class RideInvalidStateException extends BaseException {
    public RideInvalidStateException() {
        super(ErrorCode.RIDE_INVALID_STATE);
    }

    public RideInvalidStateException(String message) {
        super(ErrorCode.RIDE_INVALID_STATE, message);
    }
}
