package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

/**
 * @author Akash Kumar
 */
public class ContactSelfNotAllowedException extends BaseException {
    public ContactSelfNotAllowedException() {
        super(ErrorCode.CONTACT_SELF_NOT_ALLOWED);
    }
}
