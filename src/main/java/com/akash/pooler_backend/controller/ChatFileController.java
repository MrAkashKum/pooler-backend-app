package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.dto.response.ChatFileDownload;
import com.akash.pooler_backend.dto.response.FileUploadResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.interceptors.annotation.CurrentUser;
import com.akash.pooler_backend.service.FileUploadService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Ephemeral Chat Files", description = "Participant-only files that expire with the meetup window")
public class ChatFileController {

    private final FileUploadService fileUploadService;

    @PostMapping(value = ApiMapping.CHATS_API + ApiMapping.THREAD_FILES, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(
            @CurrentUser PbUserEntity user,
            @PathVariable String threadId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.created(fileUploadService.uploadMessageFile(user, threadId, file)));
    }

    @GetMapping(ApiMapping.CHAT_FILES_API + ApiMapping.FILE_ID)
    public ResponseEntity<?> download(
            @CurrentUser PbUserEntity user,
            @PathVariable String fileId) {
        ChatFileDownload file = fileUploadService.loadMessageFile(user, fileId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.originalFileName().replace("\"", "") + "\"")
                .body(file.resource());
    }
}
