package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class TelegramProfileNotFoundException extends BaseException {

    public TelegramProfileNotFoundException() {
        super(ErrorCode.TELEGRAM_PROFILE_NOT_FOUND, ErrorCode.TELEGRAM_PROFILE_NOT_FOUND.getDefaultMessage());
    }

    public TelegramProfileNotFoundException(String message) {
        super(ErrorCode.TELEGRAM_PROFILE_NOT_FOUND, message);
    }
}