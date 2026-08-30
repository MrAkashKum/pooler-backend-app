package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class InvalidResetTokenException extends BaseException{

    public InvalidResetTokenException() { super(ErrorCode.INVALID_RESET_TOKEN); }
}
