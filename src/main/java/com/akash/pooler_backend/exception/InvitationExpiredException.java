package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

/**
 * @author Akash Kumar
 */
public class InvitationExpiredException extends BaseException {
    public InvitationExpiredException() {
        super(ErrorCode.INVITATION_EXPIRED);
    }
}
