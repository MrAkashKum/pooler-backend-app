package com.akash.pooler_backend.dto.response;

import com.akash.pooler_backend.enums.Gender;
import com.akash.pooler_backend.enums.MatchPreference;
import lombok.Builder;
import lombok.Getter;

/**
 * A nearby Discovery-Mode user surfaced by the matching engine.
 *
 * @author Akash Kumar
 */
@Getter
@Builder
public class NearbyUserResponse {

    private String userEntityId;
    private String fullName;
    private String profilePictureUrl;
    private Gender gender;
    private MatchPreference matchPreference;
    private Double currentLatitude;
    private Double currentLongitude;
    private Double distanceKm;
    private Double bearingDegrees;
    private Double destinationLatitude;
    private Double destinationLongitude;

    /** Whether this user is also a saved contact of the requester. */
    private boolean inContacts;
}
