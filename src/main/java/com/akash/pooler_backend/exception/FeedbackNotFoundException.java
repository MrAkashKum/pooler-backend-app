package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.enums.ErrorCode;

/**
 * Raised when an admin tries to read or delete a feedback record that no longer exists.
 *
 * @author Akash Kumar
 */
public class FeedbackNotFoundException extends BaseException {

    public FeedbackNotFoundException(String feedbackEntityId) {
        super(ErrorCode.RESOURCE_NOT_FOUND, ResponseMessages.feedbackNotFound(feedbackEntityId));
    }
}
