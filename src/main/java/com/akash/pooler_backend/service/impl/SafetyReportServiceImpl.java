package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.dto.request.CreateSafetyReportRequest;
import com.akash.pooler_backend.dto.response.SafetyReportResponse;
import com.akash.pooler_backend.entity.PbSafetyReportEntity;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.SafetyReportStatus;
import com.akash.pooler_backend.interceptors.annotation.AuditAction;
import com.akash.pooler_backend.repository.PbSafetyReportRepository;
import com.akash.pooler_backend.service.SafetyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * @author Akash Kumar
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SafetyReportServiceImpl implements SafetyReportService {

    private final PbSafetyReportRepository repository;

    @Override
    @Transactional
    @AuditAction("SAFETY_REPORT_CREATE")
    public SafetyReportResponse create(PbUserEntity reporter, CreateSafetyReportRequest request) {
        PbSafetyReportEntity entity = PbSafetyReportEntity.builder()
                .entityId(newId())
                .reporterEntityId(reporter.getEntityId())
                .rideEntityId(blankToNull(request.getRideEntityId()))
                .category(request.getCategory().trim())
                .details(request.getDetails().trim())
                .contactAllowed(Boolean.TRUE.equals(request.getContactAllowed()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(SafetyReportStatus.OPEN)
                .build();
        entity = repository.save(entity);
        log.info("Safety report {} created by user={}", entity.getEntityId(), reporter.getEntityId());
        return SafetyReportResponse.from(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SafetyReportResponse> listForReporter(PbUserEntity reporter) {
        return repository
                .findAllByReporterEntityIdOrderByCreatedAtDesc(reporter.getEntityId())
                .stream()
                .map(SafetyReportResponse::from)
                .toList();
    }

    private static String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }

    private static String newId() {
        return "safe-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
