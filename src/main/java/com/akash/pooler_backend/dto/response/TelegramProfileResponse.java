package com.akash.pooler_backend.dto.response;

import com.akash.pooler_backend.entity.PbTelegramProfileEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TelegramProfileResponse {
    private String entityId;
    private String userEntityId;
    private String telegramHandle;
    private String telegramPhoneNumber;
    private boolean verified;

    public static TelegramProfileResponse from(PbTelegramProfileEntity entity) {
        return TelegramProfileResponse.builder()
                .entityId(entity.getEntityId())
                .userEntityId(entity.getUserEntityId())
                .telegramHandle(entity.getTelegramHandle())
                .telegramPhoneNumber(entity.getTelegramPhoneNumber())
                .verified(Boolean.TRUE.equals(entity.getIsVerified()))
                .build();
    }
}
