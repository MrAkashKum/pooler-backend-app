package com.akash.pooler_backend.service;

import com.akash.pooler_backend.dto.request.ShareTelegramRequest;
import com.akash.pooler_backend.dto.response.TelegramProfileResponse;
import com.akash.pooler_backend.entity.PbUserEntity;

public interface TelegramService {

    /**
     * Save or update Telegram profile for a user.
     */
    TelegramProfileResponse saveOrUpdateTelegramProfile(PbUserEntity user, ShareTelegramRequest request);

    /**
     * Get Telegram profile for a user.
     */
    TelegramProfileResponse getTelegramProfile(String userId);

    /**
     * Remove Telegram profile for a user.
     */
    void removeTelegramProfile(String userId);

    /**
     * Share Telegram ID within a chat thread (publishes to both participants).
     */
    void shareTelegramInChat(String threadId, PbUserEntity user);
}
