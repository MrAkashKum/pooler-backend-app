package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.dto.request.DiscoveryToggleRequest;
import com.akash.pooler_backend.dto.request.LocationPingRequest;
import com.akash.pooler_backend.dto.request.NearbySearchRequest;
import com.akash.pooler_backend.dto.response.DiscoveryStatusResponse;
import com.akash.pooler_backend.dto.response.NearbyUserResponse;
import com.akash.pooler_backend.entity.PbContactEntity;
import com.akash.pooler_backend.entity.PbDiscoveryStatusEntity;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.DiscoveryMode;
import com.akash.pooler_backend.enums.Gender;
import com.akash.pooler_backend.enums.MatchPreference;
import com.akash.pooler_backend.exception.DiscoveryLocationRequiredException;
import com.akash.pooler_backend.exception.DiscoveryNotEnabledException;
import com.akash.pooler_backend.interceptors.annotation.AuditAction;
import com.akash.pooler_backend.repository.PbContactRepository;
import com.akash.pooler_backend.repository.PbDiscoveryStatusRepository;
import com.akash.pooler_backend.repository.PbUserRepository;
import com.akash.pooler_backend.service.DiscoveryService;
import com.akash.pooler_backend.utils.GeoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Akash Kumar
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscoveryServiceImpl implements DiscoveryService {

    /** Pings older than this are considered stale and excluded from matches. */
    private static final long DISCOVERY_FRESHNESS_SECONDS = 5 * 60L;

    /** Approximate degrees per kilometre — used for the bounding box. */
    private static final double DEG_PER_KM = 1.0 / 111.0;

    private final PbDiscoveryStatusRepository discoveryRepo;
    private final PbContactRepository contactRepo;
    private final PbUserRepository userRepo;

    @Override
    @Transactional
    @AuditAction("DISCOVERY_TOGGLE")
    public DiscoveryStatusResponse toggle(PbUserEntity user, DiscoveryToggleRequest req) {
        if (req.getMode() == DiscoveryMode.ON
                && (req.getCurrentLatitude() == null || req.getCurrentLongitude() == null)) {
            throw new DiscoveryLocationRequiredException();
        }

        PbDiscoveryStatusEntity entity = discoveryRepo
                .findByUserEntityId(user.getEntityId())
                .orElseGet(() -> PbDiscoveryStatusEntity.builder()
                        .entityId(newId())
                        .userEntityId(user.getEntityId())
                        .build());

        entity.setMode(req.getMode());
        if (req.getMode() == DiscoveryMode.ON) {
            entity.setCurrentLatitude(req.getCurrentLatitude());
            entity.setCurrentLongitude(req.getCurrentLongitude());
            entity.setDestinationLatitude(req.getDestinationLatitude());
            entity.setDestinationLongitude(req.getDestinationLongitude());
            entity.setDestinationAddress(req.getDestinationAddress());
            entity.setLastPingedAt(Instant.now());
        }
        entity = discoveryRepo.save(entity);
        log.info("Discovery {} for user={}", req.getMode(), user.getEntityId());
        return DiscoveryStatusResponse.from(entity);
    }

    @Override
    @Transactional
    public DiscoveryStatusResponse ping(PbUserEntity user, LocationPingRequest req) {
        PbDiscoveryStatusEntity entity = discoveryRepo
                .findByUserEntityId(user.getEntityId())
                .orElseThrow(DiscoveryNotEnabledException::new);
        if (entity.getMode() != DiscoveryMode.ON) {
            throw new DiscoveryNotEnabledException();
        }
        entity.setCurrentLatitude(req.getLatitude());
        entity.setCurrentLongitude(req.getLongitude());
        entity.setLastPingedAt(Instant.now());
        return DiscoveryStatusResponse.from(discoveryRepo.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public DiscoveryStatusResponse getStatus(PbUserEntity user) {
        return discoveryRepo
                .findByUserEntityId(user.getEntityId())
                .map(DiscoveryStatusResponse::from)
                .orElseGet(() -> DiscoveryStatusResponse.from(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NearbyUserResponse> findNearby(PbUserEntity user, NearbySearchRequest req) {
        // Bounding box for first-pass filter (cheap on the DB side)
        double latDelta = req.getRadiusKm() * DEG_PER_KM;
        double lngDelta = req.getRadiusKm() * DEG_PER_KM
                / Math.max(0.000001, Math.cos(Math.toRadians(req.getLatitude())));

        Instant freshness = Instant.now().minusSeconds(DISCOVERY_FRESHNESS_SECONDS);

        List<PbDiscoveryStatusEntity> candidates = discoveryRepo.findNearbyCandidates(
                DiscoveryMode.ON,
                user.getEntityId(),
                req.getLatitude() - latDelta,
                req.getLatitude() + latDelta,
                req.getLongitude() - lngDelta,
                req.getLongitude() + lngDelta,
                freshness
        );

        if (candidates.isEmpty()) return List.of();

        // Resolve user details in batch
        Set<String> userIds = candidates.stream()
                .map(PbDiscoveryStatusEntity::getUserEntityId)
                .collect(Collectors.toSet());
        Map<String, PbUserEntity> userById = userRepo.findAll().stream()
                .filter(candidateUser -> userIds.contains(candidateUser.getEntityId()))
                .collect(Collectors.toMap(PbUserEntity::getEntityId, Function.identity(), (first, duplicate) -> first));

        // Resolve contacts of the requester for "inContacts" flag
        Set<String> contactIds = contactRepo
                .findAllByOwnerEntityIdOrderByFavoriteDescCreatedAtDesc(user.getEntityId())
                .stream()
                .map(PbContactEntity::getContactUserEntityId)
                .collect(Collectors.toSet());

        return candidates.stream()
                .map(candidate -> {
                    double distanceKm = GeoUtil.haversineKm(
                            req.getLatitude(), req.getLongitude(),
                            candidate.getCurrentLatitude(), candidate.getCurrentLongitude());
                    if (distanceKm > req.getRadiusKm()) return null;

                    double bearing = GeoUtil.bearingDegrees(
                            req.getLatitude(), req.getLongitude(),
                            candidate.getCurrentLatitude(), candidate.getCurrentLongitude());

                    PbUserEntity candidateUser = userById.get(candidate.getUserEntityId());
                    if (!isMutualPreferenceMatch(user, candidateUser)) return null;
                    return NearbyUserResponse.builder()
                            .userEntityId(candidate.getUserEntityId())
                            .fullName(candidateUser != null ? candidateUser.getFullName() : null)
                            .profilePictureUrl(candidateUser != null ? candidateUser.getProfilePictureUrl() : null)
                            .gender(candidateUser != null ? safeGender(candidateUser.getGender()) : Gender.UNKNOWN)
                            .matchPreference(candidateUser != null
                                    ? safePreference(candidateUser.getMatchPreference())
                                    : MatchPreference.ANY)
                            .currentLatitude(candidate.getCurrentLatitude())
                            .currentLongitude(candidate.getCurrentLongitude())
                            .destinationLatitude(candidate.getDestinationLatitude())
                            .destinationLongitude(candidate.getDestinationLongitude())
                            .distanceKm(round(distanceKm, 4))
                            .bearingDegrees(round(bearing, 2))
                            .inContacts(contactIds.contains(candidate.getUserEntityId()))
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingDouble(NearbyUserResponse::getDistanceKm))
                .toList();
    }

    private static double round(double v, int d) {
        double f = Math.pow(10, d);
        return Math.round(v * f) / f;
    }

    private static boolean isMutualPreferenceMatch(PbUserEntity requester, PbUserEntity candidate) {
        if (candidate == null) return false;
        return allows(safePreference(requester.getMatchPreference()), safeGender(candidate.getGender()))
                && allows(safePreference(candidate.getMatchPreference()), safeGender(requester.getGender()));
    }

    private static boolean allows(MatchPreference preference, Gender gender) {
        if (gender == Gender.UNKNOWN) return false;
        if (preference == MatchPreference.ANY) return true;
        return (preference == MatchPreference.MALE && gender == Gender.MALE)
                || (preference == MatchPreference.FEMALE && gender == Gender.FEMALE);
    }

    private static Gender safeGender(Gender gender) {
        return gender != null ? gender : Gender.UNKNOWN;
    }

    private static MatchPreference safePreference(MatchPreference preference) {
        return MatchPreference.normalized(preference);
    }

    private static String newId() {
        return "dsc-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
