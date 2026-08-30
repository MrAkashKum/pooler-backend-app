package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

/**
 * @author Akash Kumar
 */
public class InvitationInvalidStateException extends BaseException {
    public InvitationInvalidStateException() {
        super(ErrorCode.INVITATION_ALREADY_RESOLVED);
    }

    public InvitationInvalidStateException(String message) {
        super(ErrorCode.INVITATION_ALREADY_RESOLVED, message);
    }
}
