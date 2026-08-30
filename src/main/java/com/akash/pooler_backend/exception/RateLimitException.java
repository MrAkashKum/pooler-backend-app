package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class RateLimitException extends BaseException{

    public RateLimitException() { super(ErrorCode.RATE_LIMIT_EXCEEDED); }
}
