package com.akash.pooler_backend.dto.response;

import com.akash.pooler_backend.entity.PbChatThreadEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ChatThreadResponse {
    private String entityId;
    private String invitationEntityId;
    private String participant1UserId;
    private String participant2UserId;
    private String rideEntityId;
    private String status;
    private Instant expiresAt;
    private long remainingSeconds;
    private long messageCount;
    private Instant lastMessageAt;

    public static ChatThreadResponse from(PbChatThreadEntity entity) {
        return ChatThreadResponse.builder()
                .entityId(entity.getEntityId())
                .invitationEntityId(entity.getInvitationEntityId())
                .participant1UserId(entity.getParticipant1UserId())
                .participant2UserId(entity.getParticipant2UserId())
                .rideEntityId(entity.getRideEntityId())
                .status(entity.getStatus().name())
                .expiresAt(entity.getExpiresAt())
                .remainingSeconds(Math.max(0, entity.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond()))
                .messageCount(entity.getMessageCount() == null ? 0 : entity.getMessageCount())
                .lastMessageAt(entity.getLastMessageAt())
                .build();
    }
}
