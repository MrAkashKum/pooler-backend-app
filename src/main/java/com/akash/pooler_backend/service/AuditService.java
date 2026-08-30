package com.akash.pooler_backend.service;

import com.akash.pooler_backend.entity.PbAuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Audit trail service — records security events for compliance.
 * All writes are async (never block the request thread).
 */
public interface AuditService {

    void log(String entityId, String action, String details, String ipAddress);
    void log(String entity, String action, String ipAddress);
    Page<PbAuditLogEntity> getAuditLogs(String entity, Pageable pageable);
}
