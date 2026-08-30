package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbChatThreadEntity;
import com.akash.pooler_backend.enums.ChatThreadStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PbChatThreadRepository extends JpaRepository<PbChatThreadEntity, Long> {

    Optional<PbChatThreadEntity> findByEntityId(String entityId);

    Optional<PbChatThreadEntity> findByInvitationEntityId(String invitationEntityId);

    @Query("SELECT c FROM PbChatThreadEntity c WHERE " +
            "(c.participant1UserId = :userId OR c.participant2UserId = :userId) AND c.status = :status")
    List<PbChatThreadEntity> findActiveByParticipant(@Param("userId") String userId, @Param("status") ChatThreadStatus status);

    @Query("SELECT c FROM PbChatThreadEntity c WHERE c.expiresAt < :now AND c.status = :status ORDER BY c.expiresAt ASC")
    List<PbChatThreadEntity> findExpiredThreads(@Param("now") Instant now,
                                                @Param("status") ChatThreadStatus status,
                                                Pageable pageable);

    @Query("SELECT c FROM PbChatThreadEntity c WHERE c.status = :status ORDER BY c.lastMessageAt DESC")
    List<PbChatThreadEntity> findByStatus(@Param("status") ChatThreadStatus status);

    @Query("SELECT COUNT(c) FROM PbChatThreadEntity c WHERE c.participant1UserId = :userId AND c.status = :status")
    long countActiveChats(@Param("userId") String userId, @Param("status") ChatThreadStatus status);

    @Query("SELECT c.entityId FROM PbChatThreadEntity c WHERE c.participant1UserId = :userId OR c.participant2UserId = :userId")
    List<String> findEntityIdsByParticipant(@Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM PbChatThreadEntity c WHERE c.participant1UserId = :userId OR c.participant2UserId = :userId")
    int deleteByParticipant(@Param("userId") String userId);
}
