package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.config.AppProperties;
import com.akash.pooler_backend.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Public health and version endpoint.
 * Android/iOS apps call this on startup to check backend availability
 * and whether a forced-upgrade is needed based on minAppVersion.
 */
@RestController
@RequestMapping(ApiMapping.PUBLIC_API)
@RequiredArgsConstructor
@Tag(name = "Public", description = "No-auth endpoints for health, version checks")
public class HealthController {

    private final AppProperties props;

    @GetMapping(ApiMapping.HEALTH)
    @Operation(summary = "Health check for mobile app startup ping")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> info = Map.of(
                "status",      "UP",
                "service",     props.getName(),
                "version",     props.getVersion(),
                "java",        Runtime.version().toString(),
                "timestamp",   Instant.now().toString()
        );
        return ResponseEntity.ok(ApiResponse.ok(info));
    }

    @GetMapping(ApiMapping.VERSION)
    @Operation(summary = "Returns backend version + minimum required app version")
    public ResponseEntity<ApiResponse<Map<String, String>>> version() {
        Map<String, String> version = Map.of(
                "backendVersion",    props.getVersion(),
                "minAndroidVersion", "1.0.0",
                "minIosVersion",     "1.0.0"
        );
        return ResponseEntity.ok(ApiResponse.ok(version));
    }
}
