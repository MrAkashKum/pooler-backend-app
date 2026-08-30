package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class AccountLockedException extends BaseException{

    public AccountLockedException() { super(ErrorCode.ACCOUNT_LOCKED); }
    public AccountLockedException(String msg) { super(ErrorCode.ACCOUNT_LOCKED, msg); }
}
