package com.akash.pooler_backend.dto.response;

import com.akash.pooler_backend.entity.PbSavedLocationEntity;
import com.akash.pooler_backend.enums.LocationAlias;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * @author Akash Kumar
 */
@Getter
@Builder
public class SavedLocationResponse {

    private String entityId;
    private LocationAlias alias;
    private String label;
    private String address;
    private Double latitude;
    private Double longitude;
    private Instant createdAt;
    private Instant updatedAt;

    public static SavedLocationResponse from(PbSavedLocationEntity e) {
        return SavedLocationResponse.builder()
                .entityId(e.getEntityId())
                .alias(e.getAlias())
                .label(e.getLabel())
                .address(e.getAddress())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
