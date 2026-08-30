package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class AccountSuspendedException extends BaseException {
    public AccountSuspendedException() {
        super(ErrorCode.ACCOUNT_SUSPENDED);
    }
}
