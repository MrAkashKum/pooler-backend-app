package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbPasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PbPasswordResetTokenRepository extends JpaRepository<PbPasswordResetTokenEntity, Long> {

    Optional<PbPasswordResetTokenEntity> findByToken(String token);

    @Modifying
    @Query("UPDATE PbPasswordResetTokenEntity t SET t.status='REVOKED' WHERE t.entityId=:entityId AND t.status='ACTIVE'")
    void revokeAllByEntityId(String entityId);

    @Modifying
    @Query("DELETE FROM PbPasswordResetTokenEntity t WHERE t.entityId=:entityId")
    void deleteAllByEntityId(String entityId);
}
