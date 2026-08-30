package com.akash.pooler_backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class PaymentQrDownloadResponse {
    private String url;
    private Instant expiresAt;
    private String ownerEntityId;
}
