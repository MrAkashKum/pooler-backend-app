package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class PaymentQrNotConfiguredException extends BaseException {
    public PaymentQrNotConfiguredException() {
        super(ErrorCode.PAYMENT_QR_NOT_CONFIGURED);
    }
}
