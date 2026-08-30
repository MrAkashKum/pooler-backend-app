package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

/**
 * @author Akash Kumar
 */
public class IncompatibleRouteException extends BaseException {
    public IncompatibleRouteException() {
        super(ErrorCode.INCOMPATIBLE_ROUTE);
    }

    public IncompatibleRouteException(String message) {
        super(ErrorCode.INCOMPATIBLE_ROUTE, message);
    }
}
