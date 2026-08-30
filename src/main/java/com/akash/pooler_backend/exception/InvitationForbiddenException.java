package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

/**
 * @author Akash Kumar
 */
public class InvitationForbiddenException extends BaseException {
    public InvitationForbiddenException() {
        super(ErrorCode.INVITATION_FORBIDDEN);
    }
}
