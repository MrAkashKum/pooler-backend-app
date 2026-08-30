package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.dto.response.ChatFileDownload;
import com.akash.pooler_backend.dto.response.FileUploadResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.ErrorCode;
import com.akash.pooler_backend.exception.ChatAccessDeniedException;
import com.akash.pooler_backend.exception.FileUploadException;
import com.akash.pooler_backend.service.ChatService;
import com.akash.pooler_backend.service.FileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf", "text/plain");

    private final ChatService chatService;
    private final Path storagePath;
    private final long maxBytes;
    private final Duration expiry;
    private final int maxTrackedFiles;
    private final Map<String, StoredFile> files = new ConcurrentHashMap<>();

    public FileUploadServiceImpl(
            ChatService chatService,
            @Value("${file-upload.storage-path:/tmp/chat-files}") String storagePath,
            @Value("${file-upload.max-size-mb:10}") long maxSizeMb,
            @Value("${file-upload.expiration-hours:2}") long expirationHours,
            @Value("${file-upload.max-tracked-files:1000}") int maxTrackedFiles) {
        this.chatService = chatService;
        this.storagePath = Path.of(storagePath).toAbsolutePath().normalize();
        this.maxBytes = maxSizeMb * 1024 * 1024;
        this.expiry = Duration.ofHours(expirationHours);
        this.maxTrackedFiles = Math.max(1, maxTrackedFiles);
    }

    @Override
    public FileUploadResponse uploadMessageFile(PbUserEntity uploader, String threadId, MultipartFile file) {
        if (!chatService.hasAccessToChat(threadId, uploader) || chatService.isChatExpired(threadId)) {
            throw new ChatAccessDeniedException();
        }
        if (files.size() >= maxTrackedFiles) {
            cleanupExpiredFiles();
            if (files.size() >= maxTrackedFiles) {
                throw new FileUploadException(ErrorCode.RATE_LIMIT_EXCEEDED, ResponseMessages.FILE_CAPACITY_REACHED);
            }
        }
        validateFileSize(file);
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR, ResponseMessages.FILE_TYPE_NOT_ALLOWED);
        }
        String fileId = "file-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String originalName = safeOriginalName(file.getOriginalFilename());
        Path target = storagePath.resolve(fileId).normalize();
        if (!target.startsWith(storagePath)) throw new FileUploadException(ResponseMessages.FILE_PATH_INVALID);
        Instant expiresAt = Instant.now().plus(expiry);
        try {
            Files.createDirectories(storagePath);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            log.error("chatFileUploadFailed className={} methodName={} threadId={} uploaderId={} fileId={} exceptionType={}",
                    getClass().getSimpleName(), "uploadMessageFile", threadId, uploader.getEntityId(),
                    fileId, exception.getClass().getSimpleName(), exception);
            throw new FileUploadException(ResponseMessages.FILE_STORE_FAILED);
        }
        files.put(fileId, new StoredFile(threadId, originalName, contentType, target, expiresAt));
        log.info("chatFileUploaded className={} methodName={} threadId={} uploaderId={} fileId={} sizeBytes={} contentType={} expiresAt={}",
                getClass().getSimpleName(), "uploadMessageFile", threadId, uploader.getEntityId(),
                fileId, file.getSize(), contentType, expiresAt);
        return FileUploadResponse.builder()
                .fileEntityId(fileId)
                .originalFileName(originalName)
                .contentType(contentType)
                .sizeBytes(file.getSize())
                .temporaryUrl(ApiMapping.CHAT_FILES_API + "/" + fileId)
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    public String generateFileExpiryUrl(String fileId) {
        StoredFile file = requireActive(fileId);
        return ApiMapping.CHAT_FILES_API + "/" + fileId + "?expires=" + file.expiresAt().getEpochSecond();
    }

    @Override
    public void cleanupExpiredFiles() {
        files.forEach((fileId, file) -> {
            if (file.expiresAt().isBefore(Instant.now())) {
                try {
                    Files.deleteIfExists(file.path());
                } catch (IOException exception) {
                    log.warn("expiredChatFileDeleteFailed className={} methodName={} fileId={} threadId={} exceptionType={}",
                            getClass().getSimpleName(), "cleanupExpiredFiles", fileId, file.threadId(),
                            exception.getClass().getSimpleName());
                }
                files.remove(fileId);
                log.debug("expiredChatFileRemoved className={} methodName={} fileId={} threadId={}",
                        getClass().getSimpleName(), "cleanupExpiredFiles", fileId, file.threadId());
            }
        });
    }

    @Override
    public void validateFileSize(MultipartFile file) {
        if (file.isEmpty()) throw new FileUploadException(ErrorCode.VALIDATION_ERROR, ResponseMessages.FILE_EMPTY);
        if (file.getSize() > maxBytes) throw new FileUploadException();
    }

    @Override
    public ChatFileDownload loadMessageFile(PbUserEntity requester, String fileId) {
        StoredFile file = requireActive(fileId);
        if (!chatService.hasAccessToChat(file.threadId(), requester)) throw new ChatAccessDeniedException();
        return new ChatFileDownload(new FileSystemResource(file.path()), file.originalName(), file.contentType());
    }

    private StoredFile requireActive(String fileId) {
        StoredFile file = files.get(fileId);
        if (file == null || file.expiresAt().isBefore(Instant.now()) || !Files.exists(file.path())) {
            if (file != null) {
                files.remove(fileId);
            }
            throw new FileUploadException(ErrorCode.FILE_UPLOAD_EXPIRED, ErrorCode.FILE_UPLOAD_EXPIRED.getDefaultMessage());
        }
        return file;
    }

    private static String safeOriginalName(String original) {
        if (original == null || original.isBlank()) return "attachment";
        return Path.of(original).getFileName().toString().replaceAll("[^A-Za-z0-9._ -]", "_");
    }

    private record StoredFile(String threadId, String originalName, String contentType, Path path, Instant expiresAt) { }
}
