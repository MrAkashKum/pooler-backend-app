package com.akash.pooler_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "pb_payment_qr_share", indexes = {
        @Index(name = "idx_qr_share_ride", columnList = "ride_entity_id"),
        @Index(name = "idx_qr_share_recipient", columnList = "recipient_entity_id"),
        @Index(name = "idx_qr_share_active", columnList = "ride_entity_id,owner_entity_id,recipient_entity_id,revoked_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PbPaymentQrShareEntity extends BaseEntity {

    @Column(name = "entity_id", nullable = false, unique = true, length = 64)
    private String entityId;

    @Column(name = "ride_entity_id", nullable = false, length = 64)
    private String rideEntityId;

    @Column(name = "owner_entity_id", nullable = false, length = 64)
    private String ownerEntityId;

    @Column(name = "recipient_entity_id", nullable = false, length = 64)
    private String recipientEntityId;

    @Column(name = "shared_at", nullable = false)
    private Instant sharedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public boolean isActiveAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
