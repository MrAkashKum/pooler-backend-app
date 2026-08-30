package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.enums.ErrorCode;

public class MessageEditLimitExceededException extends BaseException {

    public MessageEditLimitExceededException() {
        super(ErrorCode.MESSAGE_EDIT_LIMIT_EXCEEDED, ResponseMessages.MESSAGE_EDIT_WINDOW_EXPIRED);
    }

    public MessageEditLimitExceededException(String message) {
        super(ErrorCode.MESSAGE_EDIT_LIMIT_EXCEEDED, message);
    }
}
