package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

/**
 * @author Akash Kumar
 */
public class DiscoveryLocationRequiredException extends BaseException {
    public DiscoveryLocationRequiredException() {
        super(ErrorCode.DISCOVERY_LOCATION_REQUIRED);
    }
}
