package com.akash.pooler_backend.entity;

import com.akash.pooler_backend.enums.RideStatus;
import com.akash.pooler_backend.enums.RouteCompatibility;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A confirmed shared ride created from a fully-confirmed invitation.
 *
 * The "primary" passenger has the longer trip (per the Overlap Rule);
 * the cab drops off the "secondary" passenger first, then continues
 * straight on to the primary destination.
 *
 * @author Akash Kumar
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(
        name = "pb_ride",
        indexes = {
                @Index(name = "idx_ride_primary", columnList = "primary_entity_id"),
                @Index(name = "idx_ride_secondary", columnList = "secondary_entity_id"),
                @Index(name = "idx_ride_status", columnList = "status")
        }
)
public class PbRideEntity extends BaseEntity {

    @Column(name = "entity_id", nullable = false, unique = true, length = 64)
    private String entityId;

    @Column(name = "invitation_entity_id", length = 64)
    private String invitationEntityId;

    /** User with the longer trip (cab continues to this drop-off last). */
    @Column(name = "primary_entity_id", nullable = false, length = 64)
    private String primaryEntityId;

    /** User dropped off first (their drop-off lies on the way). */
    @Column(name = "secondary_entity_id", nullable = false, length = 64)
    private String secondaryEntityId;

    // Common pickup hub
    @Column(name = "pickup_lat", nullable = false) private Double pickupLat;
    @Column(name = "pickup_lng", nullable = false) private Double pickupLng;
    @Column(name = "pickup_address", length = 500) private String pickupAddress;

    // First drop-off (secondary user)
    @Column(name = "first_drop_lat", nullable = false) private Double firstDropLat;
    @Column(name = "first_drop_lng", nullable = false) private Double firstDropLng;
    @Column(name = "first_drop_address", length = 500) private String firstDropAddress;

    // Final drop-off (primary user)
    @Column(name = "final_drop_lat", nullable = false) private Double finalDropLat;
    @Column(name = "final_drop_lng", nullable = false) private Double finalDropLng;
    @Column(name = "final_drop_address", length = 500) private String finalDropAddress;

    @Column(name = "total_distance_km") private Double totalDistanceKm;
    @Column(name = "primary_trip_distance_km") private Double primaryTripDistanceKm;
    @Column(name = "secondary_trip_distance_km") private Double secondaryTripDistanceKm;
    @Column(name = "estimated_duration_minutes") private Integer estimatedDurationMinutes;
    @Column(name = "estimated_fare") private Double estimatedFare;

    @Column(name = "fare_split_total_fare") private Double fareSplitTotalFare;
    @Column(name = "fare_split_currency", length = 12) private String fareSplitCurrency;
    @Column(name = "fare_split_provider", length = 60) private String fareSplitProvider;
    @Column(name = "primary_fare_share") private Double primaryFareShare;
    @Column(name = "secondary_fare_share") private Double secondaryFareShare;
    @Column(name = "fare_split_updated_at") private Instant fareSplitUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "compatibility", length = 30)
    private RouteCompatibility compatibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private RideStatus status = RideStatus.REQUESTED;

    @Column(name = "started_at") private Instant startedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "cancelled_at") private Instant cancelledAt;
    @Column(name = "cancel_reason", length = 500) private String cancelReason;

    @Column(name = "primary_arrived", nullable = false)
    @Builder.Default
    private boolean primaryArrived = false;

    @Column(name = "secondary_arrived", nullable = false)
    @Builder.Default
    private boolean secondaryArrived = false;

    @Column(name = "primary_arrived_at") private Instant primaryArrivedAt;
    @Column(name = "secondary_arrived_at") private Instant secondaryArrivedAt;

    @Column(name = "primary_arrival_lat") private Double primaryArrivalLat;
    @Column(name = "primary_arrival_lng") private Double primaryArrivalLng;
    @Column(name = "primary_arrival_accuracy_meters") private Double primaryArrivalAccuracyMeters;
    @Column(name = "primary_arrival_distance_km") private Double primaryArrivalDistanceKm;

    @Column(name = "secondary_arrival_lat") private Double secondaryArrivalLat;
    @Column(name = "secondary_arrival_lng") private Double secondaryArrivalLng;
    @Column(name = "secondary_arrival_accuracy_meters") private Double secondaryArrivalAccuracyMeters;
    @Column(name = "secondary_arrival_distance_km") private Double secondaryArrivalDistanceKm;

    public boolean isParticipant(String userEntityId) {
        return primaryEntityId.equals(userEntityId) || secondaryEntityId.equals(userEntityId);
    }

    public boolean bothArrived() {
        return primaryArrived && secondaryArrived;
    }
}
