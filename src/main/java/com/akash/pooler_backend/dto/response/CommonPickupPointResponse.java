package com.akash.pooler_backend.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * The output of the "Meet-in-the-Middle" engine.
 *
 * @author Akash Kumar
 */
@Getter
@Builder
public class CommonPickupPointResponse {

    private Double pickupLatitude;
    private Double pickupLongitude;
    private String pickupAddress;
    private Double distanceFromUserAKm;
    private Double distanceFromUserBKm;
    private Double estimatedWalkMinutesUserA;
    private Double estimatedWalkMinutesUserB;
}
