package com.akash.pooler_backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ChatArchivedResponse {
    private String threadEntityId;
    private long messageCount;
    private Instant archivedAt;
}
