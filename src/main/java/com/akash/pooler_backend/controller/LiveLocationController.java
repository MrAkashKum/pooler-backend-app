package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.dto.request.LiveLocationUpdateRequest;
import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.dto.response.LiveLocationResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.interceptors.annotation.CurrentUser;
import com.akash.pooler_backend.service.LiveLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Real-time location publish/fetch during a Shared Session.
 *
 * Mobile clients post their GPS pings here every few seconds while
 * the ride is active so the other participant can render them on
 * a live map.
 *
 * @author Akash Kumar
 */
@RestController
@RequestMapping(ApiMapping.RIDE_LIVE_LOCATION_API)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Live Location", description = "Real-time GPS pings during a ride")
public class LiveLocationController {

    private final LiveLocationService liveLocationService;

    @PostMapping
    @Operation(summary = "Publish your current GPS ping for this ride")
    public ResponseEntity<ApiResponse<LiveLocationResponse>> publish(
            @CurrentUser PbUserEntity user,
            @PathVariable String rideEntityId,
            @Valid @RequestBody LiveLocationUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                liveLocationService.publish(user, rideEntityId, req)));
    }

    @GetMapping
    @Operation(summary = "Fetch the latest live pings for both participants of this ride")
    public ResponseEntity<ApiResponse<List<LiveLocationResponse>>> getForRide(
            @CurrentUser PbUserEntity user,
            @PathVariable String rideEntityId) {
        return ResponseEntity.ok(ApiResponse.ok(
                liveLocationService.getForRide(user, rideEntityId)));
    }
}
