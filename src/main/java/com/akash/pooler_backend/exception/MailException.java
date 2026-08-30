package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class MailException extends BaseException{

    public MailException(String message, Throwable cause) {
        super(ErrorCode.MAIL_SEND_FAILED, message, cause);
    }
}
