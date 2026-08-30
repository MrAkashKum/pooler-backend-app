package com.akash.pooler_backend.dto.response;

import com.akash.pooler_backend.entity.PbLiveLocationEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * @author Akash Kumar
 */
@Getter
@Builder
public class LiveLocationResponse {

    private String userEntityId;
    private Double latitude;
    private Double longitude;
    private Double headingDegrees;
    private Double speedKmh;
    private Double accuracyMeters;
    private Instant reportedAt;

    public static LiveLocationResponse from(PbLiveLocationEntity e) {
        return LiveLocationResponse.builder()
                .userEntityId(e.getUserEntityId())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .headingDegrees(e.getHeadingDegrees())
                .speedKmh(e.getSpeedKmh())
                .accuracyMeters(e.getAccuracyMeters())
                .reportedAt(e.getReportedAt())
                .build();
    }
}
