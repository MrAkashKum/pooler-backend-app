package com.akash.pooler_backend.service;

import com.akash.pooler_backend.dto.request.LiveLocationUpdateRequest;
import com.akash.pooler_backend.dto.response.LiveLocationResponse;
import com.akash.pooler_backend.entity.PbUserEntity;

import java.util.List;

/**
 * Real-time location sharing during a Shared Session.
 *
 * @author Akash Kumar
 */
public interface LiveLocationService {

    LiveLocationResponse publish(PbUserEntity user, String rideEntityId, LiveLocationUpdateRequest req);

    List<LiveLocationResponse> getForRide(PbUserEntity user, String rideEntityId);
}
