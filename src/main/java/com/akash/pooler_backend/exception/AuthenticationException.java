package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class AuthenticationException extends BaseException{

    public AuthenticationException() { super(ErrorCode.INVALID_CREDENTIALS); }

    public AuthenticationException(String message) { super(ErrorCode.INVALID_CREDENTIALS, message); }
}
