package com.akash.pooler_backend.dto.response;

import com.akash.pooler_backend.enums.RouteCompatibility;
import lombok.Builder;
import lombok.Getter;

/**
 * Result of running the Overlap Rule between two intended trips.
 *
 * @author Akash Kumar
 */
@Getter
@Builder
public class RouteCompatibilityResponse {

    private RouteCompatibility compatibility;
    private Double bearingDeltaDegrees;
    private Double userATripKm;
    private Double userBTripKm;
    private Double sharedKm;
    private Double detourPercent;

    /** Which user has the longer route — their drop-off is the final stop. */
    private String primaryRoute;   // "A" or "B"
    private String secondaryRoute; // "A" or "B"
}
