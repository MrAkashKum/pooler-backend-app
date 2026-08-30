package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.dto.request.SendMessageRequest;
import com.akash.pooler_backend.dto.request.ShareTelegramRequest;
import com.akash.pooler_backend.dto.response.TelegramProfileResponse;
import com.akash.pooler_backend.entity.PbTelegramProfileEntity;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.exception.TelegramProfileNotFoundException;
import com.akash.pooler_backend.repository.PbTelegramProfileRepository;
import com.akash.pooler_backend.service.ChatService;
import com.akash.pooler_backend.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramServiceImpl implements TelegramService {

    private final PbTelegramProfileRepository repository;
    private final ChatService chatService;

    @Override
    @Transactional
    public TelegramProfileResponse saveOrUpdateTelegramProfile(PbUserEntity user, ShareTelegramRequest request) {
        PbTelegramProfileEntity profile = repository.findByUserEntityId(user.getEntityId())
                .orElseGet(() -> PbTelegramProfileEntity.builder()
                        .entityId("tg-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                        .userEntityId(user.getEntityId())
                        .build());
        String handle = request.getTelegramHandle();
        if (handle != null && !handle.isBlank() && !handle.startsWith("@")) handle = "@" + handle;
        profile.setTelegramHandle(handle);
        profile.setTelegramPhoneNumber(request.getTelegramPhoneNumber());
        profile = repository.save(profile);
        log.info("telegramProfileSaved className={} methodName={} userId={} profileId={} handlePresent={}",
                getClass().getSimpleName(), "saveOrUpdateTelegramProfile", user.getEntityId(),
                profile.getEntityId(), profile.getTelegramHandle() != null && !profile.getTelegramHandle().isBlank());
        return TelegramProfileResponse.from(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public TelegramProfileResponse getTelegramProfile(String userId) {
        return repository.findByUserEntityId(userId)
                .map(TelegramProfileResponse::from)
                .orElseThrow(TelegramProfileNotFoundException::new);
    }

    @Override
    @Transactional
    public void removeTelegramProfile(String userId) {
        repository.findByUserEntityId(userId).ifPresent(profile -> {
            repository.delete(profile);
            log.info("telegramProfileRemoved className={} methodName={} userId={} profileId={}",
                    getClass().getSimpleName(), "removeTelegramProfile", userId, profile.getEntityId());
        });
    }

    @Override
    @Transactional
    public void shareTelegramInChat(String threadId, PbUserEntity user) {
        TelegramProfileResponse profile = getTelegramProfile(user.getEntityId());
        chatService.sendMessage(user, threadId, SendMessageRequest.builder()
                .content(profile.getTelegramHandle())
                .messageType("TELEGRAM_ID_SHARE")
                .metadata(Map.of("telegramHandle", profile.getTelegramHandle()))
                .build());
        log.info("telegramHandleShared className={} methodName={} userId={} threadId={}",
                getClass().getSimpleName(), "shareTelegramInChat", user.getEntityId(), threadId);
    }
}
