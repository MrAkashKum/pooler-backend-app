package com.akash.pooler_backend.entity;

import com.akash.pooler_backend.enums.TokenStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "pb_refresh_token")
public class PbRefreshTokenEntity extends BaseEntity{

    @Column(name = "refresh_token", nullable=false, unique=true,length=512)
    private String refreshToken;

    @Column(name="entity_id", nullable=false)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable=false,length=20)
    @Builder.Default
    private TokenStatus status = TokenStatus.ACTIVE;

    @Column(name = "device_id", length=200)
    private String deviceId;

    @Column(name = "platform", length=50)
    private String platform;

    @Column(name = "app_version", length=50)
    private String appVersion;

    @Column(name = "ip_address", length=45)
    private String ipAddress;

    @Column(name = "expires_at", nullable=false)
    private Instant expiresAt;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return status == TokenStatus.REVOKED;
    }

    public boolean isActiveStatus()  {
        return status == TokenStatus.ACTIVE && !isExpired();
    }

}
