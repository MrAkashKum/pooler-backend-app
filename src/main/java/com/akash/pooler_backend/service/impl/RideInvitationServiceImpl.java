package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.config.AppProperties;
import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.dto.request.AcceptInvitationRequest;
import com.akash.pooler_backend.dto.request.SendRideInvitationRequest;
import com.akash.pooler_backend.dto.response.CommonPickupPointResponse;
import com.akash.pooler_backend.dto.response.RideInvitationResponse;
import com.akash.pooler_backend.dto.response.RideResponse;
import com.akash.pooler_backend.entity.PbRideInvitationEntity;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.InvitationStatusEnums;
import com.akash.pooler_backend.exception.InvitationExpiredException;
import com.akash.pooler_backend.exception.InvitationForbiddenException;
import com.akash.pooler_backend.exception.InvitationInvalidStateException;
import com.akash.pooler_backend.exception.InvitationNotFoundException;
import com.akash.pooler_backend.exception.InvitationParticipantBusyException;
import com.akash.pooler_backend.exception.InvitationRetryLockedException;
import com.akash.pooler_backend.exception.InvitationSelfNotAllowedException;
import com.akash.pooler_backend.exception.UserNotFoundException;
import com.akash.pooler_backend.interceptors.annotation.AuditAction;
import com.akash.pooler_backend.repository.PbRideInvitationRepository;
import com.akash.pooler_backend.repository.PbRideRepository;
import com.akash.pooler_backend.repository.PbUserRepository;
import com.akash.pooler_backend.service.GeoService;
import com.akash.pooler_backend.service.ChatService;
import com.akash.pooler_backend.service.RideInvitationService;
import com.akash.pooler_backend.service.RideService;
import com.akash.pooler_backend.enums.RideStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * @author Akash Kumar
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RideInvitationServiceImpl implements RideInvitationService {

    private static final String SYSTEM_MATCH_LOCK_ACTOR = "system:match-lock";
    private static final List<RideStatus> TERMINAL_RIDE_STATUSES = List.of(RideStatus.COMPLETED, RideStatus.CANCELLED);

    private final PbRideInvitationRepository invitationRepository;
    private final PbRideRepository rideRepository;
    private final PbUserRepository userRepository;
    private final GeoService geoService;
    private final RideService rideService;
    private final ChatService chatService;
    private final AppProperties appProperties;

    @Override
    @Transactional
    @AuditAction("INVITATION_SEND")
    public RideInvitationResponse send(PbUserEntity sender, SendRideInvitationRequest req) {
        if (sender.getEntityId().equals(req.getReceiverEntityId())) {
            throw new InvitationSelfNotAllowedException();
        }
        userRepository.findByEntityId(req.getReceiverEntityId())
                .orElseThrow(() -> new UserNotFoundException(req.getReceiverEntityId()));

        Instant now = Instant.now();
        ensureParticipantsAvailable(sender.getEntityId(), req.getReceiverEntityId());
        ensurePairCanBeInvited(sender.getEntityId(), req.getReceiverEntityId(), now);

        int ttl = req.getTtlSeconds() != null
                ? req.getTtlSeconds()
                : appProperties.getInvitation().getDefaultTtlSeconds();

        PbRideInvitationEntity entity = PbRideInvitationEntity.builder()
                .entityId(newId())
                .senderEntityId(sender.getEntityId())
                .receiverEntityId(req.getReceiverEntityId())
                .senderLat(req.getSenderLatitude())
                .senderLng(req.getSenderLongitude())
                .senderDestLat(req.getSenderDestinationLatitude())
                .senderDestLng(req.getSenderDestinationLongitude())
                .senderDestAddress(req.getSenderDestinationAddress())
                .status(InvitationStatusEnums.PENDING)
                .expiresAt(now.plusSeconds(ttl))
                .message(req.getMessage())
                .build();

        entity = invitationRepository.save(entity);
        log.info("Invitation {} sent from {} to {} ttlSeconds={}",
                entity.getEntityId(), sender.getEntityId(), req.getReceiverEntityId(), ttl);
        return RideInvitationResponse.from(entity);
    }

    @Override
    @Transactional
    @AuditAction("INVITATION_ACCEPT")
    public RideInvitationResponse accept(PbUserEntity receiver, String invitationEntityId, AcceptInvitationRequest req) {
        PbRideInvitationEntity inv = invitationRepository.findByEntityId(invitationEntityId)
                .orElseThrow(InvitationNotFoundException::new);

        if (!inv.getReceiverEntityId().equals(receiver.getEntityId())) {
            throw new InvitationForbiddenException();
        }
        ensurePendingAndFresh(inv);
        ensureParticipantsAvailable(inv.getSenderEntityId(), inv.getReceiverEntityId());

        inv.setReceiverLat(req.getReceiverLatitude());
        inv.setReceiverLng(req.getReceiverLongitude());
        inv.setReceiverDestLat(req.getReceiverDestinationLatitude());
        inv.setReceiverDestLng(req.getReceiverDestinationLongitude());
        inv.setReceiverDestAddress(req.getReceiverDestinationAddress());

        // Run the Meet-in-the-Middle engine for the pickup hub
        CommonPickupPointResponse pickup = geoService.computeCommonPickup(
                inv.getSenderLat(), inv.getSenderLng(),
                inv.getReceiverLat(), inv.getReceiverLng()
        );
        inv.setPickupLat(pickup.getPickupLatitude());
        inv.setPickupLng(pickup.getPickupLongitude());
        inv.setPickupAddress(pickup.getPickupAddress());
        inv.setEstimatedWalkDistanceKm(
                Math.max(pickup.getDistanceFromUserAKm(), pickup.getDistanceFromUserBKm()));

        inv.setStatus(InvitationStatusEnums.ACCEPTED);
        Instant now = Instant.now();
        inv.setRespondedAt(now);
        inv.setRespondedByEntityId(receiver.getEntityId());

        inv = invitationRepository.save(inv);
        int clearedPending = invitationRepository.declinePendingForParticipantsExcept(
                List.of(inv.getSenderEntityId(), inv.getReceiverEntityId()),
                inv.getEntityId(),
                InvitationStatusEnums.PENDING,
                InvitationStatusEnums.DECLINED,
                SYSTEM_MATCH_LOCK_ACTOR,
                now);
        chatService.createChatThread(receiver, inv);
        log.info("Invitation {} accepted; pickup hub computed, clearedPendingInvites={}",
                invitationEntityId, clearedPending);
        return RideInvitationResponse.from(inv);
    }

    @Override
    @Transactional
    @AuditAction("INVITATION_DECLINE")
    public RideInvitationResponse decline(PbUserEntity receiver, String invitationEntityId) {
        PbRideInvitationEntity inv = invitationRepository.findByEntityId(invitationEntityId)
                .orElseThrow(InvitationNotFoundException::new);
        if (!inv.getReceiverEntityId().equals(receiver.getEntityId())) {
            throw new InvitationForbiddenException();
        }
        ensurePendingAndFresh(inv);
        inv.setStatus(InvitationStatusEnums.DECLINED);
        inv.setRespondedAt(Instant.now());
        inv.setRespondedByEntityId(receiver.getEntityId());
        return RideInvitationResponse.from(invitationRepository.save(inv));
    }

    @Override
    @Transactional
    @AuditAction("INVITATION_CONFIRM_PICKUP")
    public ConfirmResult confirmPickup(PbUserEntity user, String invitationEntityId) {
        PbRideInvitationEntity inv = invitationRepository.findByEntityId(invitationEntityId)
                .orElseThrow(InvitationNotFoundException::new);
        if (!inv.isParticipant(user.getEntityId())) {
            throw new InvitationForbiddenException();
        }
        if (inv.getStatus() != InvitationStatusEnums.ACCEPTED) {
            throw new InvitationInvalidStateException(
                    ResponseMessages.INVITATION_ACCEPTED_REQUIRED);
        }

        if (inv.getSenderEntityId().equals(user.getEntityId())) {
            inv.setSenderConfirmed(true);
        } else {
            inv.setReceiverConfirmed(true);
        }
        inv = invitationRepository.save(inv);

        // Promote to Ride when both have confirmed
        if (inv.isFullyConfirmed()) {
            RideResponse ride = rideService.createFromInvitation(inv);
            log.info("Both parties confirmed invitation {} — ride {} created",
                    invitationEntityId, ride.getEntityId());
            return new ConfirmResult(RideInvitationResponse.from(inv), ride);
        }

        return new ConfirmResult(RideInvitationResponse.from(inv), null);
    }

    @Override
    @Transactional
    @AuditAction("INVITATION_CANCEL")
    public RideInvitationResponse cancel(PbUserEntity user, String invitationEntityId) {
        PbRideInvitationEntity inv = invitationRepository.findByEntityId(invitationEntityId)
                .orElseThrow(InvitationNotFoundException::new);
        if (!inv.isParticipant(user.getEntityId())) {
            throw new InvitationForbiddenException();
        }
        if (inv.getStatus() == InvitationStatusEnums.DECLINED) {
            throw new InvitationInvalidStateException(ResponseMessages.INVITATION_ALREADY_DECLINED);
        }
        inv.setStatus(InvitationStatusEnums.DECLINED);
        inv.setRespondedAt(Instant.now());
        inv.setRespondedByEntityId(user.getEntityId());
        inv.setMessage((inv.getMessage() == null ? "" : inv.getMessage() + " | ")
                + "Cancelled by " + user.getEntityId());
        return RideInvitationResponse.from(invitationRepository.save(inv));
    }

    @Override
    @Transactional(readOnly = true)
    public RideInvitationResponse get(PbUserEntity user, String invitationEntityId) {
        PbRideInvitationEntity inv = invitationRepository.findByEntityId(invitationEntityId)
                .orElseThrow(InvitationNotFoundException::new);
        if (!inv.isParticipant(user.getEntityId())) {
            throw new InvitationForbiddenException();
        }
        return RideInvitationResponse.from(inv);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideInvitationResponse> inbox(PbUserEntity user) {
        return invitationRepository
                .findAllByReceiverEntityIdOrderByCreatedAtDesc(user.getEntityId())
                .stream().map(RideInvitationResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideInvitationResponse> outbox(PbUserEntity user) {
        return invitationRepository
                .findAllBySenderEntityIdOrderByCreatedAtDesc(user.getEntityId())
                .stream().map(RideInvitationResponse::from).toList();
    }

    // ─── helpers ──────────────────────────────────────────────────────

    private void ensurePairCanBeInvited(String senderEntityId, String receiverEntityId, Instant now) {
        if (invitationRepository.existsPendingPair(
                senderEntityId, receiverEntityId, InvitationStatusEnums.PENDING, now)) {
            throw new InvitationInvalidStateException(ResponseMessages.INVITATION_PENDING_PAIR_EXISTS);
        }

        int lockHours = appProperties.getInvitation().getDeclineRetryLockHours();
        Instant retryCutoff = now.minus(lockHours, ChronoUnit.HOURS);
        if (invitationRepository.existsRecentReceiverDecline(
                senderEntityId, receiverEntityId, InvitationStatusEnums.DECLINED, retryCutoff)) {
            throw new InvitationRetryLockedException(lockHours);
        }
    }

    private void ensureParticipantsAvailable(String senderEntityId, String receiverEntityId) {
        if (isUserBusy(senderEntityId) || isUserBusy(receiverEntityId)) {
            throw new InvitationParticipantBusyException();
        }
    }

    private boolean isUserBusy(String userEntityId) {
        return invitationRepository.existsActiveAcceptedMeetup(userEntityId, InvitationStatusEnums.ACCEPTED)
                || rideRepository.existsActiveForUser(userEntityId, TERMINAL_RIDE_STATUSES);
    }

    private void ensurePendingAndFresh(PbRideInvitationEntity inv) {
        if (inv.getStatus() != InvitationStatusEnums.PENDING) {
            throw new InvitationInvalidStateException(
                    ResponseMessages.invitationStatusCannotBeModified(inv.getStatus()));
        }
        if (inv.isExpired()) {
            inv.setStatus(InvitationStatusEnums.DECLINED);
            invitationRepository.save(inv);
            throw new InvitationExpiredException();
        }
    }

    private static String newId() {
        return "inv-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
