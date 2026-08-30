package com.akash.pooler_backend.entity;

import com.akash.pooler_backend.enums.TokenStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "pb_email_verification_tokens",
        indexes = {
                @Index(name = "idx_evt_token", columnList = "token", unique = true),
                @Index(name = "idx_evt_entity_id", columnList = "entity_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PbEmailVerificationTokenEntity extends BaseEntity {

    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    @Column(name = "entity_id", nullable = false, length = 64)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TokenStatus status = TokenStatus.ACTIVE;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "requested_from_ip", length = 45)
    private String requestedFromIp;

    public boolean isValid() {
        return status == TokenStatus.ACTIVE && Instant.now().isBefore(expiresAt);
    }
}
