package com.akash.pooler_backend.service;

import com.akash.pooler_backend.dto.request.CreateSafetyReportRequest;
import com.akash.pooler_backend.dto.response.SafetyReportResponse;
import com.akash.pooler_backend.entity.PbUserEntity;

import java.util.List;

/**
 * @author Akash Kumar
 */
public interface SafetyReportService {

    SafetyReportResponse create(PbUserEntity reporter, CreateSafetyReportRequest request);

    List<SafetyReportResponse> listForReporter(PbUserEntity reporter);
}
