package com.akash.pooler_backend.service;

import com.akash.pooler_backend.dto.response.FileUploadResponse;
import com.akash.pooler_backend.dto.response.ChatFileDownload;
import com.akash.pooler_backend.entity.PbUserEntity;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    /**
     * Upload a file for a chat message (max 10MB, 2h expiration).
     */
    FileUploadResponse uploadMessageFile(PbUserEntity uploader, String threadId, MultipartFile file);

    /**
     * Generate a pre-signed URL for temporary file access (2h).
     */
    String generateFileExpiryUrl(String fileId);

    /**
     * Clean up expired files (scheduled every 10 minutes).
     */
    void cleanupExpiredFiles();

    /**
     * Validate file size (max 10MB).
     */
    void validateFileSize(MultipartFile file);

    ChatFileDownload loadMessageFile(PbUserEntity requester, String fileId);
}
