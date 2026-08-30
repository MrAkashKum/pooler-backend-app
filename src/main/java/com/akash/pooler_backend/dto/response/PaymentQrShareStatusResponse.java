package com.akash.pooler_backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class PaymentQrShareStatusResponse {
    private boolean configured;
    private boolean shareAllowed;
    private boolean sharedByMe;
    private boolean sharedWithMe;
    private boolean downloadAvailable;
    private String sharedOwnerEntityId;
    private Instant shareExpiresAt;
    private String reason;
}
