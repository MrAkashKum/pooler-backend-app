package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

/**
 * @author Akash Kumar
 */
public class ContactNotFoundException extends BaseException {
    public ContactNotFoundException() {
        super(ErrorCode.CONTACT_NOT_FOUND);
    }
}
