package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class MailDispatchException extends BaseException{

    public MailDispatchException(String msg, Throwable cause) {
        super(ErrorCode.MAIL_SEND_FAILED, msg, cause); }
}
