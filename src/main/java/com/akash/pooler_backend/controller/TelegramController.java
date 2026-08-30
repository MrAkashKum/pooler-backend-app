package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.dto.request.ShareTelegramRequest;
import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.dto.response.TelegramProfileResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.interceptors.annotation.CurrentUser;
import com.akash.pooler_backend.service.TelegramService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiMapping.TELEGRAM_API)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Telegram Handoff", description = "Optional Telegram identity and chat handoff")
public class TelegramController {

    private final TelegramService telegramService;

    @PutMapping(ApiMapping.ME)
    public ResponseEntity<ApiResponse<TelegramProfileResponse>> save(
            @CurrentUser PbUserEntity user, @Valid @RequestBody ShareTelegramRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(telegramService.saveOrUpdateTelegramProfile(user, request)));
    }

    @GetMapping(ApiMapping.ME)
    public ResponseEntity<ApiResponse<TelegramProfileResponse>> get(@CurrentUser PbUserEntity user) {
        return ResponseEntity.ok(ApiResponse.ok(telegramService.getTelegramProfile(user.getEntityId())));
    }

    @DeleteMapping(ApiMapping.ME)
    public ResponseEntity<ApiResponse<Void>> delete(@CurrentUser PbUserEntity user) {
        telegramService.removeTelegramProfile(user.getEntityId());
        return ResponseEntity.ok(ApiResponse.message(ResponseMessages.TELEGRAM_PROFILE_REMOVED));
    }

    @PostMapping(ApiMapping.TELEGRAM_CHAT_SHARE)
    public ResponseEntity<ApiResponse<Void>> share(
            @CurrentUser PbUserEntity user, @PathVariable String threadId) {
        telegramService.shareTelegramInChat(threadId, user);
        return ResponseEntity.ok(ApiResponse.message(ResponseMessages.TELEGRAM_HANDLE_SHARED));
    }
}
