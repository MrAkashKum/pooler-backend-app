package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class ChatExpiredException extends BaseException {

    public ChatExpiredException() {
        super(ErrorCode.CHAT_EXPIRED, ErrorCode.CHAT_EXPIRED.getDefaultMessage());
    }

    public ChatExpiredException(String message) {
        super(ErrorCode.CHAT_EXPIRED, message);
    }
}
