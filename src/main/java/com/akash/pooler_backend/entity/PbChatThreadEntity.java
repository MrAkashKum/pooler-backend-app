package com.akash.pooler_backend.entity;

import com.akash.pooler_backend.enums.ChatThreadStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "pb_chat_threads", indexes = {
        @Index(name = "idx_chat_thread_invitation", columnList = "invitation_entity_id", unique = true),
        @Index(name = "idx_chat_thread_participant1", columnList = "participant1_user_id"),
        @Index(name = "idx_chat_thread_participant2", columnList = "participant2_user_id"),
        @Index(name = "idx_chat_thread_status", columnList = "status"),
        @Index(name = "idx_chat_thread_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PbChatThreadEntity extends BaseEntity {

    @Column(name = "entity_id", unique = true, nullable = false)
    private String entityId;

    @Column(name = "invitation_entity_id", nullable = false)
    private String invitationEntityId;

    @Column(name = "participant1_user_id", nullable = false)
    private String participant1UserId;

    @Column(name = "participant2_user_id", nullable = false)
    private String participant2UserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ChatThreadStatus status = ChatThreadStatus.ACTIVE;

    @Column(name = "ride_entity_id")
    private String rideEntityId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "message_count")
    @Builder.Default
    private Long messageCount = 0L;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public long getRemainingMinutes() {
        long secondsRemaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, secondsRemaining / 60);
    }
}
