package com.akash.pooler_backend.service;

import com.akash.pooler_backend.dto.request.*;
import com.akash.pooler_backend.dto.response.ChatMessageResponse;
import com.akash.pooler_backend.dto.response.ChatThreadResponse;
import com.akash.pooler_backend.dto.response.ReadReceiptResponse;
import com.akash.pooler_backend.entity.PbChatThreadEntity;
import com.akash.pooler_backend.entity.PbRideInvitationEntity;
import com.akash.pooler_backend.entity.PbUserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ChatService {

    /**
     * Create a chat thread for an accepted ride invitation (both participants must accept).
     */
    PbChatThreadEntity createChatThread(PbUserEntity initiator, PbRideInvitationEntity invitation);

    /**
     * Send a message in an active chat thread.
     * Validates 2h window; rate-limits to 10 msg/min.
     */
    ChatMessageResponse sendMessage(PbUserEntity sender, String threadId, SendMessageRequest request);

    /**
     * Edit a previously sent message (15-min window).
     */
    ChatMessageResponse editMessage(PbUserEntity editor, String messageId, EditMessageRequest request);

    /**
     * Add an emoji reaction to a message.
     */
    ChatMessageResponse addReaction(PbUserEntity user, String messageId, String emoji);

    /**
     * Remove an emoji reaction from a message.
     */
    ChatMessageResponse removeReaction(PbUserEntity user, String messageId, String emoji);

    /**
     * Mark messages as read and broadcast read receipt.
     */
    void markMessagesAsRead(String threadId, List<String> messageIds, PbUserEntity user);

    /**
     * Get read receipts for a specific message.
     */
    ReadReceiptResponse getReadReceipts(String messageId, PbUserEntity user);

    /**
     * Search messages within a thread (only active chats).
     */
    List<ChatMessageResponse> searchMessages(String threadId, String query, PbUserEntity user);

    /**
     * Get chat thread details (with expiration check).
     */
    ChatThreadResponse getChatThread(String threadId, PbUserEntity user);

    /**
     * Get thread by invitation ID.
     */
    Optional<ChatThreadResponse> getChatThreadByInvitation(String invitationId, PbUserEntity user);

    /**
     * Get paginated messages from a thread.
     */
    Page<ChatMessageResponse> getMessages(String threadId, int page, PbUserEntity user, Pageable pageable);

    /**
     * Manually close a chat before 2h expiration.
     */
    void closeChat(String threadId, PbUserEntity user);

    /**
     * Archive expired chats (scheduled every 10 min).
     * Archives and indexes messages; moves to cold storage.
     */
    void archiveExpiredChats();

    /**
     * Get active chats for a user.
     */
    List<ChatThreadResponse> getActiveChats(PbUserEntity user);

    /**
     * Check if a user has access to a chat thread.
     */
    boolean hasAccessToChat(String threadId, PbUserEntity user);

    /**
     * Check if a chat thread is expired.
     */
    boolean isChatExpired(String threadId);

    /**
     * Publish typing indicator via WebSocket.
     */
    void publishTypingIndicator(String threadId, String userId, boolean isTyping);

    /**
     * Broadcast read receipt to the other participant.
     */
    void broadcastReadReceipt(String threadId, String messageId, String userId);
}
