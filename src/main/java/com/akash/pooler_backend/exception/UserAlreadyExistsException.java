package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class UserAlreadyExistsException extends BaseException{

    public UserAlreadyExistsException(String email) {
        super(ErrorCode.USER_ALREADY_EXISTS, ErrorCode.USER_ALREADY_EXISTS.getDefaultMessage());
    }
}
