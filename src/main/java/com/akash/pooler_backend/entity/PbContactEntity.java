package com.akash.pooler_backend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A directional "frequent contact" relationship.
 * {@code ownerEntityId} owns the contact; {@code contactUserEntityId}
 * is the referenced LinkUp user.
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
        name = "pb_contact",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_contact_owner_target",
                        columnNames = {"owner_entity_id", "contact_user_entity_id"}
                )
        },
        indexes = {
                @Index(name = "idx_contact_owner", columnList = "owner_entity_id")
        }
)
public class PbContactEntity extends BaseEntity {

    @Column(name = "entity_id", nullable = false, unique = true, length = 64)
    private String entityId;

    @Column(name = "owner_entity_id", nullable = false, length = 64)
    private String ownerEntityId;

    @Column(name = "contact_user_entity_id", nullable = false, length = 64)
    private String contactUserEntityId;

    @Column(name = "nickname", length = 120)
    private String nickname;

    @Column(name = "is_favorite", nullable = false)
    @Builder.Default
    private boolean favorite = false;
}
