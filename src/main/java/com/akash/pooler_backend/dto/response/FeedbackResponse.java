package com.akash.pooler_backend.dto.response;

import com.akash.pooler_backend.entity.PbFeedbackEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Admin-visible feedback response. The mobile app only uses create.
 *
 * @author Akash Kumar
 */
@Getter
@Builder
public class FeedbackResponse {

    private String entityId;
    private String submitterEntityId;
    private String emotion;
    private String subject;
    private int rating;
    private String message;
    private String platform;
    private String appVersion;
    private Instant createdAt;

    public static FeedbackResponse from(PbFeedbackEntity entity) {
        return FeedbackResponse.builder()
                .entityId(entity.getEntityId())
                .submitterEntityId(entity.getSubmitterEntityId())
                .emotion(entity.getEmotion())
                .subject(entity.getSubject())
                .rating(entity.getRating())
                .message(entity.getMessage())
                .platform(entity.getPlatform())
                .appVersion(entity.getAppVersion())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
