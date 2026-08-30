package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class SessionExpiredException extends BaseException{

    public SessionExpiredException() { super(ErrorCode.SESSION_EXPIRED); }
}
