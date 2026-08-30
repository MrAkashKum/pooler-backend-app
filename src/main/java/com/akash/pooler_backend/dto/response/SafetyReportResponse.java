package com.akash.pooler_backend.dto.response;

import com.akash.pooler_backend.entity.PbSafetyReportEntity;
import com.akash.pooler_backend.enums.SafetyReportStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * @author Akash Kumar
 */
@Getter
@Builder
public class SafetyReportResponse {

    private String entityId;
    private String reporterEntityId;
    private String rideEntityId;
    private String category;
    private String details;
    private boolean contactAllowed;
    private Double latitude;
    private Double longitude;
    private SafetyReportStatus status;
    private Instant createdAt;

    public static SafetyReportResponse from(PbSafetyReportEntity e) {
        return SafetyReportResponse.builder()
                .entityId(e.getEntityId())
                .reporterEntityId(e.getReporterEntityId())
                .rideEntityId(e.getRideEntityId())
                .category(e.getCategory())
                .details(e.getDetails())
                .contactAllowed(e.isContactAllowed())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
