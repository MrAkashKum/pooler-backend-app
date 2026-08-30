package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.dto.request.CreateSafetyReportRequest;
import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.dto.response.SafetyReportResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.interceptors.annotation.CurrentUser;
import com.akash.pooler_backend.service.SafetyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Safety issue reporting from the mobile Safety centre.
 *
 * @author Akash Kumar
 */
@RestController
@RequestMapping(ApiMapping.SAFETY_REPORTS_API)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Safety Reports", description = "Create and read rider safety reports")
public class SafetyReportController {

    private final SafetyReportService safetyReportService;

    @PostMapping
    @Operation(summary = "Create a safety report")
    public ResponseEntity<ApiResponse<SafetyReportResponse>> create(
            @CurrentUser PbUserEntity reporter,
            @Valid @RequestBody CreateSafetyReportRequest request) {
        return ResponseEntity.ok(ApiResponse.created(safetyReportService.create(reporter, request)));
    }

    @GetMapping
    @Operation(summary = "List reports created by the current user")
    public ResponseEntity<ApiResponse<List<SafetyReportResponse>>> list(@CurrentUser PbUserEntity reporter) {
        return ResponseEntity.ok(ApiResponse.ok(safetyReportService.listForReporter(reporter)));
    }
}
