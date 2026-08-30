package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class ChatNotFoundException extends BaseException {

    public ChatNotFoundException() {
        super(ErrorCode.CHAT_NOT_FOUND, ErrorCode.CHAT_NOT_FOUND.getDefaultMessage());
    }

    public ChatNotFoundException(String message) {
        super(ErrorCode.CHAT_NOT_FOUND, message);
    }
}