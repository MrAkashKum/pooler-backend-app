package com.akash.pooler_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "pb_message_reactions", indexes = {
        @Index(name = "idx_msg_reaction_msg_id", columnList = "message_id"),
        @Index(name = "idx_msg_reaction_user_id", columnList = "user_id"),
        @Index(name = "idx_msg_reaction_unique", columnList = "message_id,user_id,reaction", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PbMessageReactionEntity extends BaseEntity {

    @Column(name = "entity_id", unique = true, nullable = false)
    private String entityId;

    @Column(name = "message_id", nullable = false)
    private String messageId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "reaction", length = 4, nullable = false)
    private String reaction;
}
