package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.enums.ErrorCode;
import com.akash.pooler_backend.enums.LocationAlias;

/**
 * Thrown when a user tries to create a second saved location with a unique
 * alias (e.g. two HOME entries).
 *
 * @author Akash Kumar
 */
public class LocationAliasConflictException extends BaseException {
    public LocationAliasConflictException(LocationAlias alias) {
        super(ErrorCode.LOCATION_ALIAS_CONFLICT,
                ResponseMessages.locationAliasAlreadyExists(alias.name()));
    }
}
