package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.dto.request.LiveLocationUpdateRequest;
import com.akash.pooler_backend.dto.response.LiveLocationResponse;
import com.akash.pooler_backend.entity.PbLiveLocationEntity;
import com.akash.pooler_backend.entity.PbRideEntity;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.exception.RideForbiddenException;
import com.akash.pooler_backend.exception.RideInvalidStateException;
import com.akash.pooler_backend.exception.RideNotFoundException;
import com.akash.pooler_backend.repository.PbLiveLocationRepository;
import com.akash.pooler_backend.repository.PbRideRepository;
import com.akash.pooler_backend.service.LiveLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @author Akash Kumar
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveLocationServiceImpl implements LiveLocationService {

    private final PbLiveLocationRepository repo;
    private final PbRideRepository rideRepository;

    @Override
    @Transactional
    public LiveLocationResponse publish(PbUserEntity user, String rideEntityId, LiveLocationUpdateRequest req) {
        PbRideEntity ride = loadParticipantRide(user, rideEntityId);
        if (ride.getStatus().isTerminal()) {
            throw new RideInvalidStateException(ResponseMessages.liveLocationTerminalRide(ride.getStatus()));
        }

        PbLiveLocationEntity entity = repo
                .findByRideEntityIdAndUserEntityId(rideEntityId, user.getEntityId())
                .orElseGet(() -> PbLiveLocationEntity.builder()
                        .entityId(newId())
                        .rideEntityId(rideEntityId)
                        .userEntityId(user.getEntityId())
                        .build());

        entity.setLatitude(req.getLatitude());
        entity.setLongitude(req.getLongitude());
        entity.setHeadingDegrees(req.getHeadingDegrees());
        entity.setSpeedKmh(req.getSpeedKmh());
        entity.setAccuracyMeters(req.getAccuracyMeters());
        entity.setReportedAt(Instant.now());

        entity = repo.save(entity);
        log.debug("Live ping for ride={}, user={}", rideEntityId, user.getEntityId());
        return LiveLocationResponse.from(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LiveLocationResponse> getForRide(PbUserEntity user, String rideEntityId) {
        loadParticipantRide(user, rideEntityId);
        return repo.findAllByRideEntityId(rideEntityId)
                .stream().map(LiveLocationResponse::from).toList();
    }

    private PbRideEntity loadParticipantRide(PbUserEntity user, String rideEntityId) {
        PbRideEntity ride = rideRepository.findByEntityId(rideEntityId)
                .orElseThrow(RideNotFoundException::new);
        if (!ride.isParticipant(user.getEntityId())) {
            throw new RideForbiddenException();
        }
        return ride;
    }

    private static String newId() {
        return "llc-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
