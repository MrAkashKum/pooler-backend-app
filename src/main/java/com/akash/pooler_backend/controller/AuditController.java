package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.entity.PbAuditLogEntity;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.interceptors.annotation.CurrentUser;
import com.akash.pooler_backend.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiMapping.AUDIT_API)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Audit Logs", description = "Security audit trail")
public class AuditController {

    private final AuditService auditService;

    @GetMapping(ApiMapping.ME)
    @Operation(summary = "Get current pbUserEntity's audit log")
    public ResponseEntity<ApiResponse<Page<PbAuditLogEntity>>> myLogs(
            @CurrentUser PbUserEntity pbUserEntity,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(auditService.getAuditLogs(pbUserEntity.getEntityId(), pageable)));
    }

    @GetMapping(ApiMapping.AUDIT_USER)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Get audit log for any user")
    public ResponseEntity<ApiResponse<Page<PbAuditLogEntity>>> userLogs(
            @PathVariable String entityId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(auditService.getAuditLogs(entityId, pageable)));
    }


}
