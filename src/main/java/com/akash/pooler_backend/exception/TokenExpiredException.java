package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class TokenExpiredException extends BaseException{

    public TokenExpiredException() {
        super(ErrorCode.TOKEN_EXPIRED);
    }

    public TokenExpiredException(String message) {
        super(ErrorCode.TOKEN_EXPIRED, message);
    }

}
