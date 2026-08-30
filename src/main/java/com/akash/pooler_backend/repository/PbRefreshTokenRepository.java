package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbRefreshTokenEntity;
import com.akash.pooler_backend.enums.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PbRefreshTokenRepository extends JpaRepository<PbRefreshTokenEntity, Long> {
    Optional<PbRefreshTokenEntity> findByRefreshToken(String refreshToken);

    List<PbRefreshTokenEntity> findAllByEntityIdAndStatus(String entityId, TokenStatus status);

    @Modifying
    @Query("UPDATE PbRefreshTokenEntity t SET t.status='REVOKED' WHERE t.entityId=:entityId AND t.status='ACTIVE'")
    void revokeAllByEntityId(String entityId);

    @Modifying
    @Query("DELETE FROM PbRefreshTokenEntity t WHERE t.status='EXPIRED' OR t.status='REVOKED'")
    void cleanupExpiredTokens();

    @Modifying
    @Query("DELETE FROM PbRefreshTokenEntity t WHERE t.entityId=:entityId")
    void deleteAllByEntityId(String entityId);
}
