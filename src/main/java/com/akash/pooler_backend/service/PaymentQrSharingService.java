package com.akash.pooler_backend.service;

import com.akash.pooler_backend.dto.response.PaymentQrDownloadResponse;
import com.akash.pooler_backend.dto.response.PaymentQrShareStatusResponse;
import com.akash.pooler_backend.entity.PbUserEntity;

public interface PaymentQrSharingService {
    PaymentQrShareStatusResponse status(PbUserEntity user, String rideEntityId);
    PaymentQrShareStatusResponse share(PbUserEntity user, String rideEntityId);
    PaymentQrShareStatusResponse revoke(PbUserEntity user, String rideEntityId);
    PaymentQrDownloadResponse download(PbUserEntity user, String rideEntityId, String ownerEntityId);
}
