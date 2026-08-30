package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class UserNotFoundException extends BaseException{

    public UserNotFoundException(String identifier) {
        super(ErrorCode.USER_NOT_FOUND, ErrorCode.USER_NOT_FOUND.getDefaultMessage());
    }
}
