package com.akash.pooler_backend.dto.response;

import org.springframework.core.io.Resource;

public record ChatFileDownload(Resource resource, String originalFileName, String contentType) {
}
