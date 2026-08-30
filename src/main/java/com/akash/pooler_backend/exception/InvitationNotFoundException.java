package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

/**
 * @author Akash Kumar
 */
public class InvitationNotFoundException extends BaseException {
    public InvitationNotFoundException() {
        super(ErrorCode.INVITATION_NOT_FOUND);
    }
}
