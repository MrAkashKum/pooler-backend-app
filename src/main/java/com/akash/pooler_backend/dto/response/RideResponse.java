package com.akash.pooler_backend.dto.response;

import com.akash.pooler_backend.entity.PbRideEntity;
import com.akash.pooler_backend.enums.RideStatus;
import com.akash.pooler_backend.enums.RouteCompatibility;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * @author Akash Kumar
 */
@Getter
@Builder
public class RideResponse {

    private String entityId;
    private String invitationEntityId;
    private String primaryEntityId;
    private String secondaryEntityId;

    private Double pickupLatitude;
    private Double pickupLongitude;
    private String pickupAddress;

    private Double firstDropLatitude;
    private Double firstDropLongitude;
    private String firstDropAddress;

    private Double finalDropLatitude;
    private Double finalDropLongitude;
    private String finalDropAddress;

    private Double totalDistanceKm;
    private Double primaryTripDistanceKm;
    private Double secondaryTripDistanceKm;
    private Integer estimatedDurationMinutes;
    private Double estimatedFare;
    private String suggestedBookerEntityId;
    private Double fareSplitTotalFare;
    private String fareSplitCurrency;
    private String fareSplitProvider;
    private Double primaryFareShare;
    private Double secondaryFareShare;
    private Instant fareSplitUpdatedAt;

    private RouteCompatibility compatibility;
    private RideStatus status;

    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private String cancelReason;
    private boolean primaryArrived;
    private boolean secondaryArrived;
    private Instant primaryArrivedAt;
    private Instant secondaryArrivedAt;
    private Double primaryArrivalDistanceKm;
    private Double secondaryArrivalDistanceKm;
    private boolean handoffUnlocked;
    private Instant createdAt;

    public static RideResponse from(PbRideEntity r) {
        return RideResponse.builder()
                .entityId(r.getEntityId())
                .invitationEntityId(r.getInvitationEntityId())
                .primaryEntityId(r.getPrimaryEntityId())
                .secondaryEntityId(r.getSecondaryEntityId())
                .pickupLatitude(r.getPickupLat()).pickupLongitude(r.getPickupLng())
                .pickupAddress(r.getPickupAddress())
                .firstDropLatitude(r.getFirstDropLat()).firstDropLongitude(r.getFirstDropLng())
                .firstDropAddress(r.getFirstDropAddress())
                .finalDropLatitude(r.getFinalDropLat()).finalDropLongitude(r.getFinalDropLng())
                .finalDropAddress(r.getFinalDropAddress())
                .totalDistanceKm(r.getTotalDistanceKm())
                .primaryTripDistanceKm(r.getPrimaryTripDistanceKm())
                .secondaryTripDistanceKm(r.getSecondaryTripDistanceKm())
                .estimatedDurationMinutes(r.getEstimatedDurationMinutes())
                .estimatedFare(r.getEstimatedFare())
                .suggestedBookerEntityId(r.getPrimaryEntityId())
                .fareSplitTotalFare(r.getFareSplitTotalFare())
                .fareSplitCurrency(r.getFareSplitCurrency())
                .fareSplitProvider(r.getFareSplitProvider())
                .primaryFareShare(r.getPrimaryFareShare())
                .secondaryFareShare(r.getSecondaryFareShare())
                .fareSplitUpdatedAt(r.getFareSplitUpdatedAt())
                .compatibility(r.getCompatibility())
                .status(r.getStatus())
                .startedAt(r.getStartedAt())
                .completedAt(r.getCompletedAt())
                .cancelledAt(r.getCancelledAt())
                .cancelReason(r.getCancelReason())
                .primaryArrived(r.isPrimaryArrived())
                .secondaryArrived(r.isSecondaryArrived())
                .primaryArrivedAt(r.getPrimaryArrivedAt())
                .secondaryArrivedAt(r.getSecondaryArrivedAt())
                .primaryArrivalDistanceKm(r.getPrimaryArrivalDistanceKm())
                .secondaryArrivalDistanceKm(r.getSecondaryArrivalDistanceKm())
                .handoffUnlocked(com.akash.pooler_backend.utils.RideHandoffUtil.isHandoffUnlocked(r))
                .createdAt(r.getCreatedAt())
                .build();
    }
}
