package com.akash.pooler_backend.dto.response;

import com.akash.pooler_backend.entity.PbDiscoveryStatusEntity;
import com.akash.pooler_backend.enums.DiscoveryMode;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * @author Akash Kumar
 */
@Getter
@Builder
public class DiscoveryStatusResponse {

    private DiscoveryMode mode;
    private Double currentLatitude;
    private Double currentLongitude;
    private Double destinationLatitude;
    private Double destinationLongitude;
    private String destinationAddress;
    private Instant lastPingedAt;

    public static DiscoveryStatusResponse from(PbDiscoveryStatusEntity e) {
        if (e == null) {
            return DiscoveryStatusResponse.builder().mode(DiscoveryMode.OFF).build();
        }
        return DiscoveryStatusResponse.builder()
                .mode(e.getMode())
                .currentLatitude(e.getCurrentLatitude())
                .currentLongitude(e.getCurrentLongitude())
                .destinationLatitude(e.getDestinationLatitude())
                .destinationLongitude(e.getDestinationLongitude())
                .destinationAddress(e.getDestinationAddress())
                .lastPingedAt(e.getLastPingedAt())
                .build();
    }
}
