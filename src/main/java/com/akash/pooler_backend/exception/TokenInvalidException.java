package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class TokenInvalidException extends BaseException{

    public TokenInvalidException() { super(ErrorCode.TOKEN_INVALID); }
    public TokenInvalidException(String msg) { super(ErrorCode.TOKEN_INVALID, msg); }

}
