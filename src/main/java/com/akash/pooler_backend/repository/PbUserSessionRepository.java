package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbUserSessionEntity;
import com.akash.pooler_backend.enums.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PbUserSessionRepository extends JpaRepository<PbUserSessionEntity, Long> {

    Optional<PbUserSessionEntity> findByToken(String token);

    List<PbUserSessionEntity> findAllByEntityIdAndStatus(String entityId, TokenStatus status);

    @Modifying
    @Query("UPDATE PbUserSessionEntity t SET t.status='REVOKED' WHERE t.entityId=:entityId AND t.status='ACTIVE'")
    void revokeAllByEntityId(String entityId);

    @Modifying
    @Query("DELETE FROM PbUserSessionEntity t WHERE t.status='EXPIRED' OR t.status='REVOKED'")
    void cleanupExpiredTokens();

    @Modifying
    @Query("DELETE FROM PbUserSessionEntity t WHERE t.entityId=:entityId")
    void deleteAllByEntityId(String entityId);
}
