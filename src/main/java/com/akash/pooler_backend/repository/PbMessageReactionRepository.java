package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbMessageReactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PbMessageReactionRepository extends JpaRepository<PbMessageReactionEntity, Long> {

    List<PbMessageReactionEntity> findByMessageId(String messageId);

    @Modifying
    @Query("DELETE FROM PbMessageReactionEntity r WHERE r.messageId = :messageId AND r.userId = :userId AND r.reaction = :emoji")
    void deleteByMessageAndUserAndReaction(@Param("messageId") String messageId, @Param("userId") String userId, @Param("emoji") String emoji);

    @Query("SELECT COUNT(r) FROM PbMessageReactionEntity r WHERE r.messageId = :messageId AND r.reaction = :emoji")
    long countByMessageAndReaction(@Param("messageId") String messageId, @Param("emoji") String emoji);

    @Modifying
    @Query("DELETE FROM PbMessageReactionEntity r WHERE r.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM PbMessageReactionEntity r WHERE r.messageId IN :messageIds")
    int deleteByMessageIds(@Param("messageIds") List<String> messageIds);
}
