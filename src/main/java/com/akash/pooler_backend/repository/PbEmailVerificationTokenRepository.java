package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbEmailVerificationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PbEmailVerificationTokenRepository extends JpaRepository<PbEmailVerificationTokenEntity, Long> {

    Optional<PbEmailVerificationTokenEntity> findByToken(String token);

    @Modifying
    @Query("UPDATE PbEmailVerificationTokenEntity t SET t.status='REVOKED' WHERE t.entityId=:entityId AND t.status='ACTIVE'")
    void revokeAllByEntityId(String entityId);

    @Modifying
    @Query("DELETE FROM PbEmailVerificationTokenEntity t WHERE t.entityId=:entityId")
    void deleteAllByEntityId(String entityId);
}
