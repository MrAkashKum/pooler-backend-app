package com.akash.pooler_backend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ArrivalConfirmationResponse {
    private RideResponse ride;
    private boolean bothArrived;
}
