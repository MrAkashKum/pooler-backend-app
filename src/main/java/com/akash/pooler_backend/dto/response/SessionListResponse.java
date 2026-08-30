package com.akash.pooler_backend.dto.response;

import com.akash.pooler_backend.entity.PbUserSessionEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Represents one active session — sent to the mobile app
 * so users can see and revoke sessions from their device list.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionListResponse {

    private String entityId;
    private String platform;
    private String deviceId;
    private String ipAddress;
    private Instant createdAt;
    private Instant lastAccessedAt;
    private Instant expiresAt;
    private boolean current;

    public static SessionListResponse from(PbUserSessionEntity pbUserSessionEntity, boolean isCurrent) {
        return SessionListResponse.builder()
                .entityId(pbUserSessionEntity.getEntityId())
                .platform(pbUserSessionEntity.getPlatform())
                .deviceId(pbUserSessionEntity.getDeviceId())
                .ipAddress(pbUserSessionEntity.getIpAddress())
                .createdAt(pbUserSessionEntity.getCreatedAt())
                .lastAccessedAt(pbUserSessionEntity.getLastAccessedAt())
                .expiresAt(pbUserSessionEntity.getExpiresAt())
                .current(isCurrent)
                .build();
    }
}
