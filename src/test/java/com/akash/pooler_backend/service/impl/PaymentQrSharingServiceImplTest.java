package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.dto.response.PaymentQrDownloadResponse;
import com.akash.pooler_backend.dto.response.PaymentQrShareStatusResponse;
import com.akash.pooler_backend.entity.PbPaymentQrShareEntity;
import com.akash.pooler_backend.entity.PbRideEntity;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.ProfileMediaPurpose;
import com.akash.pooler_backend.enums.RideStatus;
import com.akash.pooler_backend.exception.PaymentQrNotConfiguredException;
import com.akash.pooler_backend.exception.PaymentQrNotSharedException;
import com.akash.pooler_backend.exception.RideInvalidStateException;
import com.akash.pooler_backend.repository.PbPaymentQrShareRepository;
import com.akash.pooler_backend.repository.PbRideRepository;
import com.akash.pooler_backend.repository.PbUserRepository;
import com.akash.pooler_backend.service.PaymentQrSharingService;
import com.akash.pooler_backend.service.ProfileMediaService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentQrSharingServiceImplTest {

    private static final String RIDE_ID = "ride-1";
    private static final String USER_A = "user-a";
    private static final String USER_B = "user-b";

    @Mock private PbRideRepository rideRepository;
    @Mock private PbUserRepository userRepository;
    @Mock private PbPaymentQrShareRepository shareRepository;
    @Mock private ProfileMediaService profileMediaService;

    private PaymentQrSharingServiceImpl service;

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(PaymentQrSharingServiceImpl.class, PaymentQrSharingService.class);
    }

    @BeforeEach
    void setUp() {
        service = new PaymentQrSharingServiceImpl(
                rideRepository, userRepository, shareRepository, profileMediaService);
    }

    @Test
    void missingQrIsOptionalAndStatusDoesNotFail() {
        PbUserEntity user = user(USER_A, null);
        when(rideRepository.findByEntityId(RIDE_ID)).thenReturn(Optional.of(ride(RideStatus.CAB_DISPATCHED)));

        PaymentQrShareStatusResponse status = service.status(user, RIDE_ID);

        assertFalse(status.isConfigured());
        assertTrue(status.isShareAllowed());
        assertFalse(status.isSharedByMe());
    }

    @Test
    void sharingWithoutQrReturnsBusinessConflictAndDoesNotPersist() {
        PbUserEntity user = user(USER_A, null);
        when(rideRepository.findByEntityId(RIDE_ID)).thenReturn(Optional.of(ride(RideStatus.CAB_DISPATCHED)));

        assertThrows(PaymentQrNotConfiguredException.class, () -> service.share(user, RIDE_ID));
        verify(shareRepository, never()).save(any());
    }

    @Test
    void sharingBeforeCabBookingIsRejected() {
        PbUserEntity user = user(USER_A, "s3://bucket/qr");
        when(rideRepository.findByEntityId(RIDE_ID)).thenReturn(Optional.of(ride(RideStatus.CONFIRMED)));

        assertThrows(RideInvalidStateException.class, () -> service.share(user, RIDE_ID));
        verify(shareRepository, never()).save(any());
    }

    @Test
    void sharingDuringJourneyCreatesRideBoundGrant() {
        PbUserEntity user = user(USER_A, "s3://bucket/qr");
        when(rideRepository.findByEntityId(RIDE_ID)).thenReturn(Optional.of(ride(RideStatus.IN_TRANSIT)));
        when(shareRepository.findByRideEntityIdAndOwnerEntityIdAndRecipientEntityIdAndRevokedAtIsNull(
                RIDE_ID, USER_A, USER_B)).thenReturn(List.of());
        when(shareRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.share(user, RIDE_ID);

        verify(shareRepository).save(any(PbPaymentQrShareEntity.class));
    }

    @Test
    void matchedRecipientCanDownloadActiveSharedQr() {
        PbUserEntity recipient = user(USER_B, null);
        PbUserEntity owner = user(USER_A, "s3://bucket/qr");
        PbPaymentQrShareEntity grant = grant();
        when(rideRepository.findByEntityId(RIDE_ID)).thenReturn(Optional.of(ride(RideStatus.IN_TRANSIT)));
        when(shareRepository.findFirstByRideEntityIdAndOwnerEntityIdAndRecipientEntityIdAndRevokedAtIsNullOrderBySharedAtDesc(
                RIDE_ID, USER_A, USER_B)).thenReturn(Optional.of(grant));
        when(userRepository.findByEntityId(USER_A)).thenReturn(Optional.of(owner));
        when(profileMediaService.createOwnerDownloadUrl(owner, ProfileMediaPurpose.PAYMENT_QR))
                .thenReturn("https://signed.example/qr");

        PaymentQrDownloadResponse response = service.download(recipient, RIDE_ID, USER_A);

        assertTrue(response.getUrl().startsWith("https://signed.example"));
    }

    @Test
    void cancelledRidePreventsRecipientDownloadEvenWithGrant() {
        PbUserEntity recipient = user(USER_B, null);
        when(rideRepository.findByEntityId(RIDE_ID)).thenReturn(Optional.of(ride(RideStatus.CANCELLED)));
        when(shareRepository.findFirstByRideEntityIdAndOwnerEntityIdAndRecipientEntityIdAndRevokedAtIsNullOrderBySharedAtDesc(
                RIDE_ID, USER_A, USER_B)).thenReturn(Optional.of(grant()));

        assertThrows(PaymentQrNotSharedException.class,
                () -> service.download(recipient, RIDE_ID, USER_A));
    }

    private static PbRideEntity ride(RideStatus status) {
        return PbRideEntity.builder()
                .entityId(RIDE_ID)
                .primaryEntityId(USER_A)
                .secondaryEntityId(USER_B)
                .pickupLat(1.3)
                .pickupLng(103.8)
                .firstDropLat(1.31)
                .firstDropLng(103.81)
                .finalDropLat(1.32)
                .finalDropLng(103.82)
                .status(status)
                .build();
    }

    private static PbUserEntity user(String id, String qr) {
        return PbUserEntity.builder()
                .entityId(id)
                .username(id)
                .email(id + "@example.com")
                .firstName(id)
                .lastName("Rider")
                .passwordHash("hash")
                .paymentQrCodeUrl(qr)
                .build();
    }

    private static PbPaymentQrShareEntity grant() {
        return PbPaymentQrShareEntity.builder()
                .entityId("grant-1")
                .rideEntityId(RIDE_ID)
                .ownerEntityId(USER_A)
                .recipientEntityId(USER_B)
                .sharedAt(Instant.now().minusSeconds(30))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
