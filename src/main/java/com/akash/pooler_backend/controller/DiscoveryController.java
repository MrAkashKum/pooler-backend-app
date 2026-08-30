package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.dto.request.DiscoveryToggleRequest;
import com.akash.pooler_backend.dto.request.LocationPingRequest;
import com.akash.pooler_backend.dto.request.NearbySearchRequest;
import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.dto.response.DiscoveryStatusResponse;
import com.akash.pooler_backend.dto.response.NearbyUserResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.interceptors.annotation.CurrentUser;
import com.akash.pooler_backend.interceptors.annotation.ValidSession;
import com.akash.pooler_backend.service.DiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Per-user "Ride-Sharing Toggle" and nearby-user discovery.
 *
 * @author Akash Kumar
 */
@RestController
@RequestMapping(ApiMapping.DISCOVERY_API)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Discovery", description = "Ride-sharing toggle and nearby user search")
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    @PutMapping(ApiMapping.TOGGLE)
    @ValidSession(reason = "Broadcasting your live location requires an active session")
    @Operation(summary = "Turn discovery mode ON or OFF")
    public ResponseEntity<ApiResponse<DiscoveryStatusResponse>> toggle(
            @CurrentUser PbUserEntity user,
            @Valid @RequestBody DiscoveryToggleRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(discoveryService.toggle(user, req)));
    }

    @PostMapping(ApiMapping.PING)
    @Operation(summary = "Update your current location while in discovery mode")
    public ResponseEntity<ApiResponse<DiscoveryStatusResponse>> ping(
            @CurrentUser PbUserEntity user,
            @Valid @RequestBody LocationPingRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(discoveryService.ping(user, req)));
    }

    @GetMapping(ApiMapping.STATUS)
    @Operation(summary = "Read your current discovery status")
    public ResponseEntity<ApiResponse<DiscoveryStatusResponse>> status(@CurrentUser PbUserEntity user) {
        return ResponseEntity.ok(ApiResponse.ok(discoveryService.getStatus(user)));
    }

    @PostMapping(ApiMapping.NEARBY)
    @Operation(summary = "Find nearby users currently broadcasting in discovery mode")
    public ResponseEntity<ApiResponse<List<NearbyUserResponse>>> nearby(
            @CurrentUser PbUserEntity user,
            @Valid @RequestBody NearbySearchRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(discoveryService.findNearby(user, req)));
    }
}
