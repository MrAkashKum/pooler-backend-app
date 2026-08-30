package com.akash.pooler_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * The most recent live-location ping for a user inside a Shared Session.
 * One row per (ride, user); newer pings overwrite the row.
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
        name = "pb_live_location",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_live_location_ride_user",
                        columnNames = {"ride_entity_id", "user_entity_id"}
                )
        },
        indexes = {
                @Index(name = "idx_live_location_ride", columnList = "ride_entity_id")
        }
)
public class PbLiveLocationEntity extends BaseEntity {

    @Column(name = "entity_id", nullable = false, unique = true, length = 64)
    private String entityId;

    @Column(name = "ride_entity_id", nullable = false, length = 64)
    private String rideEntityId;

    @Column(name = "user_entity_id", nullable = false, length = 64)
    private String userEntityId;

    @Column(name = "latitude", nullable = false) private Double latitude;
    @Column(name = "longitude", nullable = false) private Double longitude;
    @Column(name = "heading_degrees") private Double headingDegrees;
    @Column(name = "speed_kmh") private Double speedKmh;
    @Column(name = "accuracy_meters") private Double accuracyMeters;

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt;
}
