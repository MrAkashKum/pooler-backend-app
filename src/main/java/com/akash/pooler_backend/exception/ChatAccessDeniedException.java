package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class ChatAccessDeniedException extends BaseException {

    public ChatAccessDeniedException() {
        super(ErrorCode.CHAT_ACCESS_DENIED, ErrorCode.CHAT_ACCESS_DENIED.getDefaultMessage());
    }

    public ChatAccessDeniedException(String message) {
        super(ErrorCode.CHAT_ACCESS_DENIED, message);
    }
}