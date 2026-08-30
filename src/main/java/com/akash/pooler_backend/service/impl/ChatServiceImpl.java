package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.dto.request.EditMessageRequest;
import com.akash.pooler_backend.dto.request.SendMessageRequest;
import com.akash.pooler_backend.dto.response.ChatMessageResponse;
import com.akash.pooler_backend.dto.response.ChatThreadResponse;
import com.akash.pooler_backend.dto.response.ReadReceiptResponse;
import com.akash.pooler_backend.entity.PbChatMessageEntity;
import com.akash.pooler_backend.entity.PbChatThreadEntity;
import com.akash.pooler_backend.entity.PbRideInvitationEntity;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.ChatThreadStatus;
import com.akash.pooler_backend.enums.MessageType;
import com.akash.pooler_backend.exception.ChatAccessDeniedException;
import com.akash.pooler_backend.exception.ChatExpiredException;
import com.akash.pooler_backend.exception.ChatNotFoundException;
import com.akash.pooler_backend.exception.MessageEditLimitExceededException;
import com.akash.pooler_backend.exception.RateLimitException;
import com.akash.pooler_backend.repository.PbChatMessageRepository;
import com.akash.pooler_backend.repository.PbChatThreadRepository;
import com.akash.pooler_backend.service.ChatService;
import com.akash.pooler_backend.service.ChatArchivalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final PbChatThreadRepository threadRepository;
    private final PbChatMessageRepository messageRepository;
    private final ChatArchivalService archivalService;

    @Value("${chat.expiration-hours:2}")
    private long expirationHours;

    @Value("${chat.message-edit-window-minutes:15}")
    private long editWindowMinutes;

    @Value("${chat.rate-limit-messages-per-minute:10}")
    private long messagesPerMinute;

    @Value("${chat.cleanup-batch-size:100}")
    private int cleanupBatchSize;

    @Override
    @Transactional
    public PbChatThreadEntity createChatThread(PbUserEntity initiator, PbRideInvitationEntity invitation) {
        if (!invitation.isParticipant(initiator.getEntityId())) {
            throw new ChatAccessDeniedException();
        }
        return threadRepository.findByInvitationEntityId(invitation.getEntityId())
                .orElseGet(() -> threadRepository.save(PbChatThreadEntity.builder()
                        .entityId(newId("chat"))
                        .invitationEntityId(invitation.getEntityId())
                        .participant1UserId(invitation.getSenderEntityId())
                        .participant2UserId(invitation.getReceiverEntityId())
                        .status(ChatThreadStatus.ACTIVE)
                        .expiresAt(Instant.now().plus(Duration.ofHours(expirationHours)))
                        .build()));
    }

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(PbUserEntity sender, String threadId, SendMessageRequest request) {
        PbChatThreadEntity thread = requireActiveThread(threadId, sender);
        long recentCount = messageRepository.findByThreadAndCreatedAfter(threadId, Instant.now().minusSeconds(60))
                .stream().filter(message -> message.getSender().equals(sender.getEntityId())).count();
        if (recentCount >= messagesPerMinute) {
            throw new RateLimitException();
        }

        MessageType type = MessageType.TEXT;
        if (request.getMessageType() != null && !request.getMessageType().isBlank()) {
            type = MessageType.valueOf(request.getMessageType().trim().toUpperCase());
        }
        PbChatMessageEntity message = messageRepository.save(PbChatMessageEntity.builder()
                .entityId(newId("msg"))
                .threadId(threadId)
                .sender(sender.getEntityId())
                .content(request.getContent().trim())
                .messageType(type)
                .metadata(request.getMetadata() == null ? new HashMap<>() : new HashMap<>(request.getMetadata()))
                .readByUserIds(new ArrayList<>(List.of(sender.getEntityId())))
                .build());

        thread.setMessageCount((thread.getMessageCount() == null ? 0 : thread.getMessageCount()) + 1);
        thread.setLastMessageAt(Instant.now());
        threadRepository.save(thread);
        return ChatMessageResponse.from(message);
    }

    @Override
    @Transactional
    public ChatMessageResponse editMessage(PbUserEntity editor, String messageId, EditMessageRequest request) {
        PbChatMessageEntity message = requireMessage(messageId);
        requireActiveThread(message.getThreadId(), editor);
        if (!message.getSender().equals(editor.getEntityId())) {
            throw new ChatAccessDeniedException(ResponseMessages.MESSAGE_EDIT_SENDER_ONLY);
        }
        if (message.getCreatedAt().plus(Duration.ofMinutes(editWindowMinutes)).isBefore(Instant.now())) {
            throw new MessageEditLimitExceededException();
        }
        message.setContent(request.getNewContent().trim());
        message.setEditedAt(Instant.now());
        return ChatMessageResponse.from(messageRepository.save(message));
    }

    @Override
    @Transactional
    public ChatMessageResponse addReaction(PbUserEntity user, String messageId, String emoji) {
        PbChatMessageEntity message = requireMessage(messageId);
        requireActiveThread(message.getThreadId(), user);
        message.getReactions().computeIfAbsent(emoji, ignored -> new ArrayList<>());
        if (!message.getReactions().get(emoji).contains(user.getEntityId())) {
            message.getReactions().get(emoji).add(user.getEntityId());
        }
        return ChatMessageResponse.from(messageRepository.save(message));
    }

    @Override
    @Transactional
    public ChatMessageResponse removeReaction(PbUserEntity user, String messageId, String emoji) {
        PbChatMessageEntity message = requireMessage(messageId);
        requireActiveThread(message.getThreadId(), user);
        if (message.getReactions().containsKey(emoji)) {
            message.getReactions().get(emoji).remove(user.getEntityId());
            if (message.getReactions().get(emoji).isEmpty()) message.getReactions().remove(emoji);
        }
        return ChatMessageResponse.from(messageRepository.save(message));
    }

    @Override
    @Transactional
    public void markMessagesAsRead(String threadId, List<String> messageIds, PbUserEntity user) {
        requireActiveThread(threadId, user);
        for (String messageId : messageIds) {
            PbChatMessageEntity message = requireMessage(messageId);
            if (!message.getThreadId().equals(threadId)) throw new ChatAccessDeniedException();
            if (!message.getReadByUserIds().contains(user.getEntityId())) message.getReadByUserIds().add(user.getEntityId());
            message.setIsRead(true);
            messageRepository.save(message);
        }
    }

    @Override
    public ReadReceiptResponse getReadReceipts(String messageId, PbUserEntity user) {
        PbChatMessageEntity message = requireMessage(messageId);
        requireThread(message.getThreadId(), user);
        return ReadReceiptResponse.builder()
                .messageEntityId(messageId)
                .readByUserIds(List.copyOf(message.getReadByUserIds()))
                .build();
    }

    @Override
    public List<ChatMessageResponse> searchMessages(String threadId, String query, PbUserEntity user) {
        requireActiveThread(threadId, user);
        return messageRepository.searchInThread(threadId, query).stream().map(ChatMessageResponse::from).toList();
    }

    @Override
    public ChatThreadResponse getChatThread(String threadId, PbUserEntity user) {
        return ChatThreadResponse.from(requireThread(threadId, user));
    }

    @Override
    public Optional<ChatThreadResponse> getChatThreadByInvitation(String invitationId, PbUserEntity user) {
        return threadRepository.findByInvitationEntityId(invitationId)
                .filter(thread -> isParticipant(thread, user.getEntityId()))
                .map(ChatThreadResponse::from);
    }

    @Override
    public Page<ChatMessageResponse> getMessages(String threadId, int page, PbUserEntity user, Pageable pageable) {
        requireThread(threadId, user);
        return messageRepository.findByThreadIdOrderByCreatedAtDesc(threadId, pageable).map(ChatMessageResponse::from);
    }

    @Override
    @Transactional
    public void closeChat(String threadId, PbUserEntity user) {
        PbChatThreadEntity thread = requireThread(threadId, user);
        thread.setStatus(ChatThreadStatus.CLOSED);
        threadRepository.save(thread);
    }

    @Override
    @Transactional
    public void archiveExpiredChats() {
        Pageable cleanupPage = PageRequest.of(0, Math.max(1, cleanupBatchSize));
        List<PbChatThreadEntity> expired = threadRepository.findExpiredThreads(Instant.now(), ChatThreadStatus.ACTIVE, cleanupPage);
        expired.forEach(thread -> {
            archivalService.archiveThread(thread.getEntityId());
            thread.setStatus(ChatThreadStatus.ARCHIVED);
            thread.setArchivedAt(Instant.now());
        });
        threadRepository.saveAll(expired);
    }

    @Override
    public List<ChatThreadResponse> getActiveChats(PbUserEntity user) {
        return threadRepository.findActiveByParticipant(user.getEntityId(), ChatThreadStatus.ACTIVE)
                .stream().map(ChatThreadResponse::from).toList();
    }

    @Override
    public boolean hasAccessToChat(String threadId, PbUserEntity user) {
        return threadRepository.findByEntityId(threadId)
                .map(thread -> isParticipant(thread, user.getEntityId()))
                .orElse(false);
    }

    @Override
    public boolean isChatExpired(String threadId) {
        return threadRepository.findByEntityId(threadId).map(PbChatThreadEntity::isExpired).orElse(true);
    }

    @Override
    public void publishTypingIndicator(String threadId, String userId, boolean isTyping) {
        log.debug("Typing indicator thread={} user={} typing={}", threadId, userId, isTyping);
    }

    @Override
    public void broadcastReadReceipt(String threadId, String messageId, String userId) {
        log.debug("Read receipt thread={} message={} user={}", threadId, messageId, userId);
    }

    private PbChatThreadEntity requireThread(String threadId, PbUserEntity user) {
        PbChatThreadEntity thread = threadRepository.findByEntityId(threadId).orElseThrow(ChatNotFoundException::new);
        if (!isParticipant(thread, user.getEntityId())) throw new ChatAccessDeniedException();
        return thread;
    }

    private PbChatThreadEntity requireActiveThread(String threadId, PbUserEntity user) {
        PbChatThreadEntity thread = requireThread(threadId, user);
        if (thread.getStatus() != ChatThreadStatus.ACTIVE || thread.isExpired()) throw new ChatExpiredException();
        return thread;
    }

    private PbChatMessageEntity requireMessage(String messageId) {
        return messageRepository.findByEntityId(messageId).orElseThrow(ChatNotFoundException::new);
    }

    private boolean isParticipant(PbChatThreadEntity thread, String userId) {
        return thread.getParticipant1UserId().equals(userId) || thread.getParticipant2UserId().equals(userId);
    }

    private static String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
