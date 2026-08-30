package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.config.AppProperties;
import com.akash.pooler_backend.dto.request.AcceptInvitationRequest;
import com.akash.pooler_backend.dto.request.SendRideInvitationRequest;
import com.akash.pooler_backend.dto.response.CommonPickupPointResponse;
import com.akash.pooler_backend.entity.PbRideInvitationEntity;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.InvitationStatusEnums;
import com.akash.pooler_backend.enums.RideStatus;
import com.akash.pooler_backend.exception.InvitationInvalidStateException;
import com.akash.pooler_backend.exception.InvitationParticipantBusyException;
import com.akash.pooler_backend.exception.InvitationRetryLockedException;
import com.akash.pooler_backend.repository.PbRideInvitationRepository;
import com.akash.pooler_backend.repository.PbRideRepository;
import com.akash.pooler_backend.repository.PbUserRepository;
import com.akash.pooler_backend.service.ChatService;
import com.akash.pooler_backend.service.GeoService;
import com.akash.pooler_backend.service.RideInvitationService;
import com.akash.pooler_backend.service.RideService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideInvitationServiceImplTest {

    private static final String USER_A = "user-a";
    private static final String USER_B = "user-b";
    private static final List<RideStatus> TERMINAL_RIDE_STATUSES = List.of(RideStatus.COMPLETED, RideStatus.CANCELLED);

    @Mock
    private PbRideInvitationRepository invitationRepository;
    @Mock
    private PbRideRepository rideRepository;
    @Mock
    private PbUserRepository userRepository;
    @Mock
    private GeoService geoService;
    @Mock
    private RideService rideService;
    @Mock
    private ChatService chatService;

    private RideInvitationServiceImpl service;

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(RideInvitationServiceImpl.class, RideInvitationService.class);
    }

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getInvitation().setDefaultTtlSeconds(300);
        appProperties.getInvitation().setDeclineRetryLockHours(8);
        service = new RideInvitationServiceImpl(
                invitationRepository, rideRepository, userRepository, geoService, rideService, chatService, appProperties);
    }

    @Test
    void sendBlocksDuplicatePendingInviteForSamePairOnly() {
        when(userRepository.findByEntityId(USER_B)).thenReturn(Optional.of(user(USER_B)));
        when(invitationRepository.existsPendingPair(
                eq(USER_A), eq(USER_B), eq(InvitationStatusEnums.PENDING), any(Instant.class)))
                .thenReturn(true);

        assertThrows(InvitationInvalidStateException.class,
                () -> service.send(user(USER_A), sendRequest(USER_B)));

        verify(invitationRepository, never()).save(any(PbRideInvitationEntity.class));
    }

    @Test
    void sendBlocksSamePairAfterReceiverDeclinedWithinLockWindow() {
        when(userRepository.findByEntityId(USER_B)).thenReturn(Optional.of(user(USER_B)));
        when(invitationRepository.existsRecentReceiverDecline(
                eq(USER_A), eq(USER_B), eq(InvitationStatusEnums.DECLINED), any(Instant.class)))
                .thenReturn(true);

        assertThrows(InvitationRetryLockedException.class,
                () -> service.send(user(USER_A), sendRequest(USER_B)));

        verify(invitationRepository, never()).save(any(PbRideInvitationEntity.class));
    }

    @Test
    void sendBlocksWhenEitherParticipantAlreadyHasActiveAcceptedMeetup() {
        when(userRepository.findByEntityId(USER_B)).thenReturn(Optional.of(user(USER_B)));
        when(invitationRepository.existsActiveAcceptedMeetup(USER_A, InvitationStatusEnums.ACCEPTED)).thenReturn(false);
        when(invitationRepository.existsActiveAcceptedMeetup(USER_B, InvitationStatusEnums.ACCEPTED)).thenReturn(true);

        assertThrows(InvitationParticipantBusyException.class,
                () -> service.send(user(USER_A), sendRequest(USER_B)));

        verify(invitationRepository, never()).save(any(PbRideInvitationEntity.class));
    }

    @Test
    void sendBlocksWhenEitherParticipantAlreadyHasActiveRide() {
        when(userRepository.findByEntityId(USER_B)).thenReturn(Optional.of(user(USER_B)));
        when(rideRepository.existsActiveForUser(USER_A, TERMINAL_RIDE_STATUSES)).thenReturn(true);

        assertThrows(InvitationParticipantBusyException.class,
                () -> service.send(user(USER_A), sendRequest(USER_B)));

        verify(invitationRepository, never()).save(any(PbRideInvitationEntity.class));
    }

    @Test
    void acceptReservesParticipantsAndClearsOtherPendingInvites() {
        PbRideInvitationEntity invitation = pendingInvitation();
        when(invitationRepository.findByEntityId(invitation.getEntityId())).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(PbRideInvitationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(geoService.computeCommonPickup(
                invitation.getSenderLat(), invitation.getSenderLng(), 1.31, 103.82))
                .thenReturn(CommonPickupPointResponse.builder()
                        .pickupLatitude(1.305)
                        .pickupLongitude(103.81)
                        .pickupAddress("Common point")
                        .distanceFromUserAKm(0.4)
                        .distanceFromUserBKm(0.5)
                        .build());

        var response = service.accept(user(USER_B), invitation.getEntityId(), acceptRequest());

        assertEquals(InvitationStatusEnums.ACCEPTED, response.getStatus());
        verify(invitationRepository).declinePendingForParticipantsExcept(
                eq(List.of(USER_A, USER_B)),
                eq(invitation.getEntityId()),
                eq(InvitationStatusEnums.PENDING),
                eq(InvitationStatusEnums.DECLINED),
                eq("system:match-lock"),
                ArgumentMatchers.any(Instant.class));
        verify(chatService).createChatThread(any(PbUserEntity.class), any(PbRideInvitationEntity.class));
    }

    private static PbUserEntity user(String entityId) {
        PbUserEntity user = new PbUserEntity();
        user.setEntityId(entityId);
        return user;
    }

    private static SendRideInvitationRequest sendRequest(String receiverEntityId) {
        return SendRideInvitationRequest.builder()
                .receiverEntityId(receiverEntityId)
                .senderLatitude(1.30)
                .senderLongitude(103.80)
                .senderDestinationLatitude(1.35)
                .senderDestinationLongitude(103.90)
                .senderDestinationAddress("Destination")
                .build();
    }

    private static AcceptInvitationRequest acceptRequest() {
        return AcceptInvitationRequest.builder()
                .receiverLatitude(1.31)
                .receiverLongitude(103.82)
                .receiverDestinationLatitude(1.35)
                .receiverDestinationLongitude(103.90)
                .receiverDestinationAddress("Destination")
                .build();
    }

    private static PbRideInvitationEntity pendingInvitation() {
        return PbRideInvitationEntity.builder()
                .entityId("inv-test")
                .senderEntityId(USER_A)
                .receiverEntityId(USER_B)
                .senderLat(1.30)
                .senderLng(103.80)
                .senderDestLat(1.35)
                .senderDestLng(103.90)
                .senderDestAddress("Destination")
                .status(InvitationStatusEnums.PENDING)
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }
}
