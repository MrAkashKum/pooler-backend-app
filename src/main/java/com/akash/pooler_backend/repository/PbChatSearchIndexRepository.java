package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbChatSearchIndexEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PbChatSearchIndexRepository extends JpaRepository<PbChatSearchIndexEntity, Long> {

    Optional<PbChatSearchIndexEntity> findByThreadId(String threadId);

    long deleteByThreadIdIn(List<String> threadIds);
}
