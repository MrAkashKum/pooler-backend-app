package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

/**
 * @author Akash Kumar
 */
public class LocationNotFoundException extends BaseException {
    public LocationNotFoundException() {
        super(ErrorCode.LOCATION_NOT_FOUND);
    }

    public LocationNotFoundException(String message) {
        super(ErrorCode.LOCATION_NOT_FOUND, message);
    }
}
