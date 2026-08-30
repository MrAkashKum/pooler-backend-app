package com.akash.pooler_backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class TypingIndicatorResponse {
    private String threadEntityId;
    private String userEntityId;
    private boolean typing;
    private Instant sentAt;
}
