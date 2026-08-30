package com.akash.pooler_backend.entity;

import com.akash.pooler_backend.enums.LocationAlias;
import jakarta.persistence.*;
import lombok.*;

/**
 * A user's saved location — Home / Work / Favourite / Custom.
 *
 * The {@code (user_entity_id, alias)} pair is unique only when the alias is
 * one of the singleton aliases (HOME, WORK). Multiple FAVOURITE / CUSTOM
 * locations are permitted; uniqueness for those is enforced in service code.
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
        name = "pb_saved_location",
        indexes = {
                @Index(name = "idx_saved_location_user", columnList = "user_entity_id"),
                @Index(name = "idx_saved_location_alias", columnList = "alias")
        }
)
public class PbSavedLocationEntity extends BaseEntity {

    @Column(name = "entity_id", nullable = false, unique = true, length = 64)
    private String entityId;

    @Column(name = "user_entity_id", nullable = false, length = 64)
    private String userEntityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alias", nullable = false, length = 20)
    private LocationAlias alias;

    @Column(name = "label", length = 120)
    private String label;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;
}
