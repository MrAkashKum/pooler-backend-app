package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

/**
 * @author Akash Kumar
 */
public class RideNotFoundException extends BaseException {
    public RideNotFoundException() {
        super(ErrorCode.RIDE_NOT_FOUND);
    }
}
