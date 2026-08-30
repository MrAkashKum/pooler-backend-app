package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.entity.PbAuditLogEntity;
import com.akash.pooler_backend.repository.PbAuditLogRepository;
import com.akash.pooler_backend.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final PbAuditLogRepository auditLogRepository;

    @Override
    @Async("auditExecutor")
    @Transactional
    public void log(String entityId, String action, String details, String ipAddress) {
        try {
            auditLogRepository.save(PbAuditLogEntity.of(entityId, action, details, ipAddress));
            log.debug("Audit: userId={} action={}", entityId, action);
        } catch (Exception e) {
            log.error("Audit write failed: type={}", e.getClass().getSimpleName());
        }
    }

    @Override
    @Async("auditExecutor")
    @Transactional
    public void log(String entityId, String action, String ipAddress) {
        log(entityId, action, null, ipAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PbAuditLogEntity> getAuditLogs(String userId, Pageable pageable) {
        return auditLogRepository.findAllByEntityIdOrderByCreatedAtDesc(userId, pageable);
    }
}
