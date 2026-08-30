package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbChatArchiveEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PbChatArchiveRepository extends JpaRepository<PbChatArchiveEntity, Long> {

    Optional<PbChatArchiveEntity> findByThreadId(String threadId);

    long deleteByThreadIdIn(List<String> threadIds);
}
