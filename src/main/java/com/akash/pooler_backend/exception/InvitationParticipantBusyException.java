package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.enums.ErrorCode;

public class InvitationParticipantBusyException extends BaseException {

    public InvitationParticipantBusyException() {
        super(ErrorCode.INVITATION_PARTICIPANT_BUSY, ResponseMessages.INVITATION_PARTICIPANT_BUSY);
    }
}
