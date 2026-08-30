package com.akash.pooler_backend.dto.response;

import com.akash.pooler_backend.entity.PbRideInvitationEntity;
import com.akash.pooler_backend.enums.InvitationStatusEnums;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * @author Akash Kumar
 */
@Getter
@Builder
public class RideInvitationResponse {

    private String entityId;
    private String senderEntityId;
    private String receiverEntityId;

    private Double senderLatitude;
    private Double senderLongitude;
    private Double senderDestinationLatitude;
    private Double senderDestinationLongitude;
    private String senderDestinationAddress;

    private Double receiverLatitude;
    private Double receiverLongitude;
    private Double receiverDestinationLatitude;
    private Double receiverDestinationLongitude;
    private String receiverDestinationAddress;

    private Double pickupLatitude;
    private Double pickupLongitude;
    private String pickupAddress;
    private Double estimatedWalkDistanceKm;

    private InvitationStatusEnums status;
    private boolean senderConfirmed;
    private boolean receiverConfirmed;

    private Instant expiresAt;
    private Instant respondedAt;
    private Instant createdAt;
    private String message;

    public static RideInvitationResponse from(PbRideInvitationEntity i) {
        return RideInvitationResponse.builder()
                .entityId(i.getEntityId())
                .senderEntityId(i.getSenderEntityId())
                .receiverEntityId(i.getReceiverEntityId())
                .senderLatitude(i.getSenderLat()).senderLongitude(i.getSenderLng())
                .senderDestinationLatitude(i.getSenderDestLat())
                .senderDestinationLongitude(i.getSenderDestLng())
                .senderDestinationAddress(i.getSenderDestAddress())
                .receiverLatitude(i.getReceiverLat()).receiverLongitude(i.getReceiverLng())
                .receiverDestinationLatitude(i.getReceiverDestLat())
                .receiverDestinationLongitude(i.getReceiverDestLng())
                .receiverDestinationAddress(i.getReceiverDestAddress())
                .pickupLatitude(i.getPickupLat()).pickupLongitude(i.getPickupLng())
                .pickupAddress(i.getPickupAddress())
                .estimatedWalkDistanceKm(i.getEstimatedWalkDistanceKm())
                .status(i.getStatus())
                .senderConfirmed(i.isSenderConfirmed())
                .receiverConfirmed(i.isReceiverConfirmed())
                .expiresAt(i.getExpiresAt())
                .respondedAt(i.getRespondedAt())
                .createdAt(i.getCreatedAt())
                .message(i.getMessage())
                .build();
    }
}
