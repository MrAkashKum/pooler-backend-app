package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.entity.PbChatArchiveEntity;
import com.akash.pooler_backend.entity.PbChatMessageEntity;
import com.akash.pooler_backend.entity.PbChatThreadEntity;
import com.akash.pooler_backend.exception.ChatNotFoundException;
import com.akash.pooler_backend.repository.PbChatArchiveRepository;
import com.akash.pooler_backend.repository.PbChatMessageRepository;
import com.akash.pooler_backend.repository.PbChatThreadRepository;
import com.akash.pooler_backend.service.ChatArchivalService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatArchivalServiceImpl implements ChatArchivalService {
    private static final String METHOD_ARCHIVE_THREAD = "archiveThread";

    private final PbChatArchiveRepository archiveRepository;
    private final PbChatThreadRepository threadRepository;
    private final PbChatMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void archiveThread(String threadId) {
        if (archiveRepository.findByThreadId(threadId).isPresent()) {
            log.debug("chatArchiveSkipped className={} methodName={} threadId={} reason=alreadyArchived",
                    getClass().getSimpleName(), METHOD_ARCHIVE_THREAD, threadId);
            return;
        }
        PbChatThreadEntity thread = threadRepository.findByEntityId(threadId).orElseThrow(ChatNotFoundException::new);
        List<PbChatMessageEntity> messages = messageRepository.findByThreadAndCreatedAfter(threadId, Instant.EPOCH);
        Map<String, Object> archive = Map.of(
                "thread", Map.of(
                        "entityId", thread.getEntityId(),
                        "invitationEntityId", thread.getInvitationEntityId(),
                        "participant1UserId", thread.getParticipant1UserId(),
                        "participant2UserId", thread.getParticipant2UserId(),
                        "expiresAt", thread.getExpiresAt().toString()),
                "messages", messages.stream().map(message -> Map.of(
                        "entityId", message.getEntityId(),
                        "senderEntityId", message.getSender(),
                        "content", message.getContent(),
                        "type", message.getMessageType().name(),
                        "createdAt", message.getCreatedAt().toString())).toList());
        try {
            byte[] serialized = objectMapper.writeValueAsBytes(archive);
            archiveRepository.save(PbChatArchiveEntity.builder()
                    .entityId("archive-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20))
                    .threadId(threadId)
                    .archiveData(archive)
                    .sizeBytes((long) serialized.length)
                    .build());
            log.info("chatArchived className={} methodName={} threadId={} messageCount={} sizeBytes={}",
                    getClass().getSimpleName(), METHOD_ARCHIVE_THREAD, threadId, messages.size(), serialized.length);
        } catch (JsonProcessingException exception) {
            log.error("chatArchiveFailed className={} methodName={} threadId={} exceptionType={}",
                    getClass().getSimpleName(), METHOD_ARCHIVE_THREAD, threadId, exception.getClass().getSimpleName(), exception);
            throw new IllegalStateException(ResponseMessages.chatArchiveWriteFailed(threadId), exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String retrieveArchivedThread(String threadId) {
        PbChatArchiveEntity archive = archiveRepository.findByThreadId(threadId).orElseThrow(ChatNotFoundException::new);
        try {
            String serializedArchive = objectMapper.writeValueAsString(archive.getArchiveData());
            log.debug("chatArchiveRead className={} methodName={} threadId={}",
                    getClass().getSimpleName(), "retrieveArchivedThread", threadId);
            return serializedArchive;
        } catch (JsonProcessingException exception) {
            log.error("chatArchiveReadFailed className={} methodName={} threadId={} exceptionType={}",
                    getClass().getSimpleName(), "retrieveArchivedThread", threadId,
                    exception.getClass().getSimpleName(), exception);
            throw new IllegalStateException(ResponseMessages.CHAT_ARCHIVE_READ_FAILED, exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getArchivedMessageCount(String threadId) {
        return archiveRepository.findByThreadId(threadId).isPresent() ? messageRepository.countByThreadId(threadId) : 0;
    }
}
