package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.dto.request.AddReactionRequest;
import com.akash.pooler_backend.dto.request.EditMessageRequest;
import com.akash.pooler_backend.dto.request.MarkAsReadRequest;
import com.akash.pooler_backend.dto.request.RemoveReactionRequest;
import com.akash.pooler_backend.dto.request.SendMessageRequest;
import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.dto.response.ChatMessageResponse;
import com.akash.pooler_backend.dto.response.ChatThreadResponse;
import com.akash.pooler_backend.dto.response.ReadReceiptResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.interceptors.annotation.CurrentUser;
import com.akash.pooler_backend.interceptors.annotation.ValidSession;
import com.akash.pooler_backend.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiMapping.CHATS_API)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Ephemeral Chat", description = "Invitation chat with a two-hour retention window")
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    @Operation(summary = "List active chats for the current user")
    public ResponseEntity<ApiResponse<List<ChatThreadResponse>>> active(@CurrentUser PbUserEntity user) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getActiveChats(user)));
    }

    @GetMapping(ApiMapping.BY_INVITATION)
    public ResponseEntity<ApiResponse<ChatThreadResponse>> byInvitation(
            @CurrentUser PbUserEntity user, @PathVariable String invitationId) {
        return chatService.getChatThreadByInvitation(invitationId, user)
                .map(chat -> ResponseEntity.ok(ApiResponse.ok(chat)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(ApiMapping.THREAD_ID)
    public ResponseEntity<ApiResponse<ChatThreadResponse>> get(
            @CurrentUser PbUserEntity user, @PathVariable String threadId) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getChatThread(threadId, user)));
    }

    @GetMapping(ApiMapping.THREAD_MESSAGES)
    public ResponseEntity<ApiResponse<Page<ChatMessageResponse>>> messages(
            @CurrentUser PbUserEntity user,
            @PathVariable String threadId,
            @PageableDefault(size = 30) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getMessages(threadId, pageable.getPageNumber(), user, pageable)));
    }

    @PostMapping(ApiMapping.THREAD_MESSAGES)
    @ValidSession(reason = "Sending a chat message requires an active session")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> send(
            @CurrentUser PbUserEntity user,
            @PathVariable String threadId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.created(chatService.sendMessage(user, threadId, request)));
    }

    @PutMapping(ApiMapping.THREAD_MESSAGE)
    public ResponseEntity<ApiResponse<ChatMessageResponse>> edit(
            @CurrentUser PbUserEntity user,
            @PathVariable String threadId,
            @PathVariable String messageId,
            @Valid @RequestBody EditMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.editMessage(user, messageId, request)));
    }

    @PostMapping(ApiMapping.THREAD_READ)
    public ResponseEntity<ApiResponse<Void>> read(
            @CurrentUser PbUserEntity user,
            @PathVariable String threadId,
            @Valid @RequestBody MarkAsReadRequest request) {
        chatService.markMessagesAsRead(threadId, request.getMessageIds(), user);
        return ResponseEntity.ok(ApiResponse.message(ResponseMessages.CHAT_MESSAGES_MARKED_READ));
    }

    @PostMapping(ApiMapping.THREAD_MESSAGE_REACTIONS)
    public ResponseEntity<ApiResponse<ChatMessageResponse>> react(
            @CurrentUser PbUserEntity user,
            @PathVariable String threadId,
            @PathVariable String messageId,
            @RequestBody AddReactionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.addReaction(user, messageId, request.getEmoji())));
    }

    @DeleteMapping(ApiMapping.THREAD_MESSAGE_REACTIONS)
    public ResponseEntity<ApiResponse<ChatMessageResponse>> removeReaction(
            @CurrentUser PbUserEntity user,
            @PathVariable String threadId,
            @PathVariable String messageId,
            @RequestBody RemoveReactionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.removeReaction(user, messageId, request.getEmoji())));
    }

    @GetMapping(ApiMapping.THREAD_MESSAGE_RECEIPTS)
    public ResponseEntity<ApiResponse<ReadReceiptResponse>> receipts(
            @CurrentUser PbUserEntity user,
            @PathVariable String threadId,
            @PathVariable String messageId) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getReadReceipts(messageId, user)));
    }

    @GetMapping(ApiMapping.THREAD_SEARCH)
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> search(
            @CurrentUser PbUserEntity user,
            @PathVariable String threadId,
            @RequestParam String query) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.searchMessages(threadId, query, user)));
    }

    @DeleteMapping(ApiMapping.THREAD_ID)
    public ResponseEntity<ApiResponse<Void>> close(
            @CurrentUser PbUserEntity user, @PathVariable String threadId) {
        chatService.closeChat(threadId, user);
        return ResponseEntity.ok(ApiResponse.message(ResponseMessages.CHAT_CLOSED));
    }
}
