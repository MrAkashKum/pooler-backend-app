package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class PaymentQrNotSharedException extends BaseException {
    public PaymentQrNotSharedException() {
        super(ErrorCode.PAYMENT_QR_NOT_SHARED);
    }
}
