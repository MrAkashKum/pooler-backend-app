package com.akash.pooler_backend.entity;

import com.akash.pooler_backend.enums.DiscoveryMode;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Per-user discovery (ride-share) state, plus the user's last known
 * coordinates broadcast while in {@link DiscoveryMode#ON}.
 *
 * One row per user; uniqueness is enforced on {@code user_entity_id}.
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
        name = "pb_discovery_status",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_discovery_user", columnNames = "user_entity_id")
        },
        indexes = {
                @Index(name = "idx_discovery_mode", columnList = "mode")
        }
)
public class PbDiscoveryStatusEntity extends BaseEntity {

    @Column(name = "entity_id", nullable = false, unique = true, length = 64)
    private String entityId;

    @Column(name = "user_entity_id", nullable = false, length = 64)
    private String userEntityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 10)
    @Builder.Default
    private DiscoveryMode mode = DiscoveryMode.OFF;

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "destination_latitude")
    private Double destinationLatitude;

    @Column(name = "destination_longitude")
    private Double destinationLongitude;

    @Column(name = "destination_address", length = 500)
    private String destinationAddress;

    @Column(name = "last_pinged_at")
    private Instant lastPingedAt;
}
