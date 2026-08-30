package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

/**
 * @author Akash Kumar
 */
public class InvitationSelfNotAllowedException extends BaseException {
    public InvitationSelfNotAllowedException() {
        super(ErrorCode.INVITATION_SELF_NOT_ALLOWED);
    }
}
