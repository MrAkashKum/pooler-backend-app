package com.akash.pooler_backend.dto.response;

import com.akash.pooler_backend.entity.PbChatMessageEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ChatMessageResponse {
    private String entityId;
    private String threadId;
    private String senderEntityId;
    private String content;
    private String messageType;
    private Map<String, Object> metadata;
    private List<String> readByUserIds;
    private Map<String, List<String>> reactions;
    private boolean read;
    private Instant createdAt;
    private Instant editedAt;

    public static ChatMessageResponse from(PbChatMessageEntity entity) {
        return ChatMessageResponse.builder()
                .entityId(entity.getEntityId())
                .threadId(entity.getThreadId())
                .senderEntityId(entity.getSender())
                .content(entity.getContent())
                .messageType(entity.getMessageType().name())
                .metadata(entity.getMetadata())
                .readByUserIds(entity.getReadByUserIds())
                .reactions(entity.getReactions())
                .read(Boolean.TRUE.equals(entity.getIsRead()))
                .createdAt(entity.getCreatedAt())
                .editedAt(entity.getEditedAt())
                .build();
    }
}
