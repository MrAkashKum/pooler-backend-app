package com.akash.pooler_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.Date;

/**
 * Returns decoded JWT metadata without revealing the full payload.
 * Useful for mobile apps to check token expiry before making calls.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenInfoResponse {

    private String subject;
    private String role;
    private String tokenType;
    private Date expiresAt;
    private boolean expired;
    private long expiresInSeconds;
}
