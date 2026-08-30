package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

/**
 * @author Akash Kumar
 */
public class RideForbiddenException extends BaseException {
    public RideForbiddenException() {
        super(ErrorCode.RIDE_FORBIDDEN);
    }
}
