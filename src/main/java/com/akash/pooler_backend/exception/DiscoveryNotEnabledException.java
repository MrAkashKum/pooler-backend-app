package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

/**
 * @author Akash Kumar
 */
public class DiscoveryNotEnabledException extends BaseException {
    public DiscoveryNotEnabledException() {
        super(ErrorCode.DISCOVERY_NOT_ENABLED);
    }
}
