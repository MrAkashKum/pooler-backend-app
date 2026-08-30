package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

/**
 * @author AKash Kumar
 */
public interface PbUserRepository extends JpaRepository<PbUserEntity, Long> {

    Optional<PbUserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<PbUserEntity> findByUsername(String username);

    @Modifying
    @Query("UPDATE PbUserEntity u SET u.lastLoginAt=:now, u.failedLoginAttempts=0, u.lockedUntil=null WHERE u.entityId=:entityId")
    void updateLoginSuccess(String entityId, Instant now);

    @Modifying
    @Query("UPDATE PbUserEntity u SET u.failedLoginAttempts=u.failedLoginAttempts+1 WHERE u.entityId=:entityId")
    void incrementFailedAttempts(String entityId);

    @Modifying
    @Query("UPDATE PbUserEntity u SET u.lockedUntil=:until, u.status=:status WHERE u.entityId=:entityId")
    void lockAccount(String entityId, Instant until, UserStatus status);

    Optional<PbUserEntity> findByEntityId(String entityId);
}
