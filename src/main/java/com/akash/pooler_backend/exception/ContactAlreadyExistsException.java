package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

/**
 * @author Akash Kumar
 */
public class ContactAlreadyExistsException extends BaseException {
    public ContactAlreadyExistsException() {
        super(ErrorCode.CONTACT_ALREADY_EXISTS);
    }
}
