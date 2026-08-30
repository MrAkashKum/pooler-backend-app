package com.akash.pooler_backend.service;

import com.akash.pooler_backend.dto.request.DiscoveryToggleRequest;
import com.akash.pooler_backend.dto.request.LocationPingRequest;
import com.akash.pooler_backend.dto.request.NearbySearchRequest;
import com.akash.pooler_backend.dto.response.DiscoveryStatusResponse;
import com.akash.pooler_backend.dto.response.NearbyUserResponse;
import com.akash.pooler_backend.entity.PbUserEntity;

import java.util.List;

/**
 * Manages the per-user "Ride-Sharing Toggle" and surfaces nearby
 * candidates while the user is in Discovery Mode.
 *
 * @author Akash Kumar
 */
public interface DiscoveryService {

    DiscoveryStatusResponse toggle(PbUserEntity user, DiscoveryToggleRequest req);

    DiscoveryStatusResponse ping(PbUserEntity user, LocationPingRequest req);

    DiscoveryStatusResponse getStatus(PbUserEntity user);

    List<NearbyUserResponse> findNearby(PbUserEntity user, NearbySearchRequest req);
}
