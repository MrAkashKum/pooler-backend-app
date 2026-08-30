package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.dto.response.PaymentQrDownloadResponse;
import com.akash.pooler_backend.dto.response.PaymentQrShareStatusResponse;
import com.akash.pooler_backend.entity.PbPaymentQrShareEntity;
import com.akash.pooler_backend.entity.PbRideEntity;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.ProfileMediaPurpose;
import com.akash.pooler_backend.enums.RideStatus;
import com.akash.pooler_backend.exception.PaymentQrNotConfiguredException;
import com.akash.pooler_backend.exception.PaymentQrNotSharedException;
import com.akash.pooler_backend.exception.RideForbiddenException;
import com.akash.pooler_backend.exception.RideInvalidStateException;
import com.akash.pooler_backend.exception.RideNotFoundException;
import com.akash.pooler_backend.exception.UserNotFoundException;
import com.akash.pooler_backend.repository.PbPaymentQrShareRepository;
import com.akash.pooler_backend.repository.PbRideRepository;
import com.akash.pooler_backend.repository.PbUserRepository;
import com.akash.pooler_backend.service.PaymentQrSharingService;
import com.akash.pooler_backend.service.ProfileMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentQrSharingServiceImpl implements PaymentQrSharingService {

    private static final Set<RideStatus> ACTIVE_SHARE_STATES = EnumSet.of(
            RideStatus.CAB_DISPATCHED,
            RideStatus.EN_ROUTE_TO_PICKUP,
            RideStatus.AT_PICKUP,
            RideStatus.IN_TRANSIT);
    private static final Duration ACTIVE_GRANT_LIFETIME = Duration.ofHours(12);
    private static final Duration COMPLETED_SETTLEMENT_WINDOW = Duration.ofHours(2);
    private static final Duration DOWNLOAD_URL_LIFETIME = Duration.ofMinutes(5);

    private final PbRideRepository rideRepository;
    private final PbUserRepository userRepository;
    private final PbPaymentQrShareRepository shareRepository;
    private final ProfileMediaService profileMediaService;

    @Override
    @Transactional(readOnly = true)
    public PaymentQrShareStatusResponse status(PbUserEntity user, String rideEntityId) {
        PbRideEntity ride = loadParticipant(user, rideEntityId);
        return buildStatus(user, ride, Instant.now());
    }

    @Override
    @Transactional
    public PaymentQrShareStatusResponse share(PbUserEntity user, String rideEntityId) {
        PbRideEntity ride = loadParticipant(user, rideEntityId);
        Instant now = Instant.now();
        if (!shareAllowed(ride, now)) {
            throw new RideInvalidStateException(ResponseMessages.PAYMENT_QR_SHARE_NOT_ALLOWED);
        }
        if (!hasPaymentQr(user)) {
            throw new PaymentQrNotConfiguredException();
        }
        String recipientId = otherParticipant(ride, user.getEntityId());
        shareRepository.findByRideEntityIdAndOwnerEntityIdAndRecipientEntityIdAndRevokedAtIsNull(
                        rideEntityId, user.getEntityId(), recipientId)
                .forEach(existing -> existing.setRevokedAt(now));
        shareRepository.save(PbPaymentQrShareEntity.builder()
                .entityId("qrs-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                .rideEntityId(rideEntityId)
                .ownerEntityId(user.getEntityId())
                .recipientEntityId(recipientId)
                .sharedAt(now)
                .expiresAt(grantExpiry(ride, now))
                .build());
        return buildStatus(user, ride, now);
    }

    @Override
    @Transactional
    public PaymentQrShareStatusResponse revoke(PbUserEntity user, String rideEntityId) {
        PbRideEntity ride = loadParticipant(user, rideEntityId);
        Instant now = Instant.now();
        String recipientId = otherParticipant(ride, user.getEntityId());
        shareRepository.findByRideEntityIdAndOwnerEntityIdAndRecipientEntityIdAndRevokedAtIsNull(
                        rideEntityId, user.getEntityId(), recipientId)
                .forEach(existing -> existing.setRevokedAt(now));
        return buildStatus(user, ride, now);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentQrDownloadResponse download(PbUserEntity user, String rideEntityId, String ownerEntityId) {
        PbRideEntity ride = loadParticipant(user, rideEntityId);
        Instant now = Instant.now();

        if (ownerEntityId == null || ownerEntityId.isBlank() || ownerEntityId.equals(user.getEntityId())) {
            if (!hasPaymentQr(user)) {
                throw new PaymentQrNotConfiguredException();
            }
            return downloadForOwner(user, now);
        }

        String expectedOwnerId = otherParticipant(ride, user.getEntityId());
        if (!expectedOwnerId.equals(ownerEntityId)) {
            throw new PaymentQrNotSharedException();
        }
        PbPaymentQrShareEntity grant = activeGrant(rideEntityId, ownerEntityId, user.getEntityId(), now)
                .filter(ignored -> shareAllowed(ride, now))
                .orElseThrow(PaymentQrNotSharedException::new);
        PbUserEntity owner = userRepository.findByEntityId(grant.getOwnerEntityId())
                .orElseThrow(() -> new UserNotFoundException(grant.getOwnerEntityId()));
        if (!hasPaymentQr(owner)) {
            throw new PaymentQrNotSharedException();
        }
        return PaymentQrDownloadResponse.builder()
                .url(profileMediaService.createOwnerDownloadUrl(owner, ProfileMediaPurpose.PAYMENT_QR))
                .expiresAt(now.plus(DOWNLOAD_URL_LIFETIME))
                .ownerEntityId(owner.getEntityId())
                .build();
    }

    private PaymentQrDownloadResponse downloadForOwner(PbUserEntity owner, Instant now) {
        return PaymentQrDownloadResponse.builder()
                .url(profileMediaService.createOwnerDownloadUrl(owner, ProfileMediaPurpose.PAYMENT_QR))
                .expiresAt(now.plus(DOWNLOAD_URL_LIFETIME))
                .ownerEntityId(owner.getEntityId())
                .build();
    }

    private PaymentQrShareStatusResponse buildStatus(PbUserEntity user, PbRideEntity ride, Instant now) {
        String otherId = otherParticipant(ride, user.getEntityId());
        Optional<PbPaymentQrShareEntity> outbound = activeGrant(
                ride.getEntityId(), user.getEntityId(), otherId, now);
        Optional<PbPaymentQrShareEntity> inbound = activeGrant(
                ride.getEntityId(), otherId, user.getEntityId(), now);
        boolean allowed = shareAllowed(ride, now);
        boolean configured = hasPaymentQr(user);
        String reason = configured
                ? (allowed ? null : "RIDE_NOT_ELIGIBLE")
                : "QR_NOT_CONFIGURED";
        return PaymentQrShareStatusResponse.builder()
                .configured(configured)
                .shareAllowed(allowed)
                .sharedByMe(outbound.isPresent())
                .sharedWithMe(inbound.isPresent() && allowed)
                .downloadAvailable(configured || (inbound.isPresent() && allowed))
                .sharedOwnerEntityId(inbound.map(PbPaymentQrShareEntity::getOwnerEntityId).orElse(null))
                .shareExpiresAt(outbound.map(PbPaymentQrShareEntity::getExpiresAt)
                        .orElseGet(() -> inbound.map(PbPaymentQrShareEntity::getExpiresAt).orElse(null)))
                .reason(reason)
                .build();
    }

    private Optional<PbPaymentQrShareEntity> activeGrant(
            String rideId, String ownerId, String recipientId, Instant now) {
        return shareRepository
                .findFirstByRideEntityIdAndOwnerEntityIdAndRecipientEntityIdAndRevokedAtIsNullOrderBySharedAtDesc(
                        rideId, ownerId, recipientId)
                .filter(grant -> grant.isActiveAt(now));
    }

    private boolean shareAllowed(PbRideEntity ride, Instant now) {
        if (ACTIVE_SHARE_STATES.contains(ride.getStatus())) {
            return true;
        }
        return ride.getStatus() == RideStatus.COMPLETED
                && ride.getCompletedAt() != null
                && now.isBefore(ride.getCompletedAt().plus(COMPLETED_SETTLEMENT_WINDOW));
    }

    private Instant grantExpiry(PbRideEntity ride, Instant now) {
        if (ride.getStatus() == RideStatus.COMPLETED && ride.getCompletedAt() != null) {
            return ride.getCompletedAt().plus(COMPLETED_SETTLEMENT_WINDOW);
        }
        return now.plus(ACTIVE_GRANT_LIFETIME);
    }

    private PbRideEntity loadParticipant(PbUserEntity user, String rideEntityId) {
        PbRideEntity ride = rideRepository.findByEntityId(rideEntityId)
                .orElseThrow(RideNotFoundException::new);
        if (!ride.isParticipant(user.getEntityId())) {
            throw new RideForbiddenException();
        }
        return ride;
    }

    private static boolean hasPaymentQr(PbUserEntity user) {
        return user.getPaymentQrCodeUrl() != null
                && user.getPaymentQrCodeUrl().startsWith("s3://");
    }

    private static String otherParticipant(PbRideEntity ride, String userId) {
        return ride.getPrimaryEntityId().equals(userId)
                ? ride.getSecondaryEntityId()
                : ride.getPrimaryEntityId();
    }
}
