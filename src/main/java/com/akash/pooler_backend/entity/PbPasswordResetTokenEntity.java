package com.akash.pooler_backend.entity;

import com.akash.pooler_backend.enums.TokenStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name="pb_password_reset_tokens",
        indexes={@Index(name="idx_prt_token",columnList="token",unique=true),
                @Index(name="idx_prt_user_id",columnList="entity_id")})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PbPasswordResetTokenEntity extends BaseEntity{

    @Column(name = "token", nullable=false,unique=true,length=255)
    private String token;

    @Column(name="entity_id",nullable=false)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable=false,length=20)
    @Builder.Default
    private TokenStatus status = TokenStatus.ACTIVE;

    @Column(name = "expires_at", nullable=false)
    private Instant expiresAt;

    @Column(name = "requested_from_ip", length=45)
    private String requestedFromIp;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    public boolean isValid()   {
        return status == TokenStatus.ACTIVE && !isExpired();
    }
}
