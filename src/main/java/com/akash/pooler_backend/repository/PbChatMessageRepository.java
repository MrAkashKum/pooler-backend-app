package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbChatMessageEntity;
import org.springframework.data.domain.Page;
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
public interface PbChatMessageRepository extends JpaRepository<PbChatMessageEntity, Long> {

    Optional<PbChatMessageEntity> findByEntityId(String entityId);

    Page<PbChatMessageEntity> findByThreadIdOrderByCreatedAtDesc(String threadId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM PbChatMessageEntity m WHERE m.threadId = :threadId")
    long countByThreadId(@Param("threadId") String threadId);

    @Query("SELECT m FROM PbChatMessageEntity m WHERE m.threadId = :threadId AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY m.createdAt DESC")
    List<PbChatMessageEntity> searchInThread(@Param("threadId") String threadId, @Param("query") String query);

    @Query("SELECT m FROM PbChatMessageEntity m WHERE m.threadId = :threadId AND m.createdAt >= :startTime ORDER BY m.createdAt ASC")
    List<PbChatMessageEntity> findByThreadAndCreatedAfter(@Param("threadId") String threadId, @Param("startTime") Instant startTime);

    @Modifying
    @Query("DELETE FROM PbChatMessageEntity m WHERE m.threadId = :threadId")
    void deleteByThreadId(@Param("threadId") String threadId);

    @Query("SELECT COUNT(m) FROM PbChatMessageEntity m WHERE m.threadId = :threadId AND m.sender <> :userId AND m.isRead = false")
    long countUnreadByThreadAndUser(@Param("threadId") String threadId, @Param("userId") String userId);

    @Query("SELECT m.entityId FROM PbChatMessageEntity m WHERE m.threadId IN :threadIds OR m.sender = :userId")
    List<String> findEntityIdsForAccountDeletion(@Param("threadIds") List<String> threadIds, @Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM PbChatMessageEntity m WHERE m.threadId IN :threadIds OR m.sender = :userId")
    int deleteForAccountDeletion(@Param("threadIds") List<String> threadIds, @Param("userId") String userId);
}
