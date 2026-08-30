package com.akash.pooler_backend.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * @author Akash Kumar
 */
@Getter
@Builder
public class DistanceResponse {
    private Double distanceKm;
    private Double bearingDegrees;
}
