package com.akash.pooler_backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class FileUploadResponse {
    private String fileEntityId;
    private String originalFileName;
    private String contentType;
    private long sizeBytes;
    private String temporaryUrl;
    private Instant expiresAt;
}
