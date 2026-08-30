package com.akash.pooler_backend.entity;

import com.akash.pooler_backend.enums.InvitationStatusEnums;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A ride-share invitation between two users.
 *
 * Stores the snapshot of both users' current locations, both intended
 * destinations, and the computed Common Pickup Hub. The invitation
 * itself is short-lived (default 5 minutes) and must be accepted AND
 * confirmed before a {@link PbRideEntity} is created.
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
        name = "pb_ride_invitation",
        indexes = {
                @Index(name = "idx_invitation_sender", columnList = "sender_entity_id"),
                @Index(name = "idx_invitation_receiver", columnList = "receiver_entity_id"),
                @Index(name = "idx_invitation_status", columnList = "status")
        }
)
public class PbRideInvitationEntity extends BaseEntity {

    @Column(name = "entity_id", nullable = false, unique = true, length = 64)
    private String entityId;

    @Column(name = "sender_entity_id", nullable = false, length = 64)
    private String senderEntityId;

    @Column(name = "receiver_entity_id", nullable = false, length = 64)
    private String receiverEntityId;

    // Sender snapshot
    @Column(name = "sender_lat", nullable = false) private Double senderLat;
    @Column(name = "sender_lng", nullable = false) private Double senderLng;
    @Column(name = "sender_dest_lat", nullable = false) private Double senderDestLat;
    @Column(name = "sender_dest_lng", nullable = false) private Double senderDestLng;
    @Column(name = "sender_dest_address", length = 500) private String senderDestAddress;

    // Receiver snapshot (filled on accept)
    @Column(name = "receiver_lat") private Double receiverLat;
    @Column(name = "receiver_lng") private Double receiverLng;
    @Column(name = "receiver_dest_lat") private Double receiverDestLat;
    @Column(name = "receiver_dest_lng") private Double receiverDestLng;
    @Column(name = "receiver_dest_address", length = 500) private String receiverDestAddress;

    // Common Pickup Hub (computed by the Meet-in-the-Middle engine)
    @Column(name = "pickup_lat") private Double pickupLat;
    @Column(name = "pickup_lng") private Double pickupLng;
    @Column(name = "pickup_address", length = 500) private String pickupAddress;

    @Column(name = "estimated_walk_distance_km") private Double estimatedWalkDistanceKm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InvitationStatusEnums status = InvitationStatusEnums.PENDING;

    /** Set to true only after BOTH users confirm the suggested pickup. */
    @Column(name = "sender_confirmed", nullable = false)
    @Builder.Default
    private boolean senderConfirmed = false;

    @Column(name = "receiver_confirmed", nullable = false)
    @Builder.Default
    private boolean receiverConfirmed = false;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "responded_by_entity_id", length = 64)
    private String respondedByEntityId;

    @Column(name = "message", length = 500)
    private String message;

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isFullyConfirmed() {
        return senderConfirmed && receiverConfirmed
                && status == InvitationStatusEnums.ACCEPTED;
    }

    public boolean isParticipant(String userEntityId) {
        return senderEntityId.equals(userEntityId) || receiverEntityId.equals(userEntityId);
    }
}
