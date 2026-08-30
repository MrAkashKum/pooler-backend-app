package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class RefreshTokenExpiredException extends BaseException{

    public RefreshTokenExpiredException() { super(ErrorCode.REFRESH_TOKEN_EXPIRED); }
}
