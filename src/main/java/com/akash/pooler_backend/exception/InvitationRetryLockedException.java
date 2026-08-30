package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.enums.ErrorCode;

public class InvitationRetryLockedException extends BaseException {

    public InvitationRetryLockedException(int lockHours) {
        super(ErrorCode.INVITATION_RETRY_LOCKED, ResponseMessages.invitationRetryLocked(lockHours));
    }
}
