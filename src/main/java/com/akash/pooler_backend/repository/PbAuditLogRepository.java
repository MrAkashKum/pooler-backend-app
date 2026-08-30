package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbAuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PbAuditLogRepository extends JpaRepository<PbAuditLogEntity, Long> {

    Page<PbAuditLogEntity> findAllByEntityIdOrderByCreatedAtDesc(String entityId, Pageable pageable);

    long deleteByEntityId(String entityId);
}
