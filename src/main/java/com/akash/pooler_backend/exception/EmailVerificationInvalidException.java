package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class EmailVerificationInvalidException extends BaseException {

    public EmailVerificationInvalidException() {
        super(ErrorCode.EMAIL_VERIFICATION_INVALID);
    }
}
