package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.dto.request.SaveLocationRequest;
import com.akash.pooler_backend.dto.request.UpdateLocationRequest;
import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.dto.response.SavedLocationResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.interceptors.annotation.CurrentUser;
import com.akash.pooler_backend.service.SavedLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Personal saved locations: HOME, WORK, and free-form CUSTOM points.
 *
 * @author Akash Kumar
 */
@RestController
@RequestMapping(ApiMapping.LOCATIONS_API)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Saved Locations", description = "Bookmarked places (home / work / custom)")
public class SavedLocationController {

    private final SavedLocationService savedLocationService;

    @PostMapping
    @Operation(summary = "Save a new location")
    public ResponseEntity<ApiResponse<SavedLocationResponse>> create(
            @CurrentUser PbUserEntity user,
            @Valid @RequestBody SaveLocationRequest req) {
        return ResponseEntity.ok(ApiResponse.created(savedLocationService.create(user, req)));
    }

    @GetMapping
    @Operation(summary = "List all saved locations")
    public ResponseEntity<ApiResponse<List<SavedLocationResponse>>> list(@CurrentUser PbUserEntity user) {
        return ResponseEntity.ok(ApiResponse.ok(savedLocationService.listForUser(user)));
    }

    @GetMapping(ApiMapping.LOCATION_ID)
    @Operation(summary = "Fetch a specific saved location")
    public ResponseEntity<ApiResponse<SavedLocationResponse>> get(
            @CurrentUser PbUserEntity user,
            @PathVariable String locationEntityId) {
        return ResponseEntity.ok(ApiResponse.ok(savedLocationService.get(user, locationEntityId)));
    }

    @PutMapping(ApiMapping.LOCATION_ID)
    @Operation(summary = "Update a saved location (partial)")
    public ResponseEntity<ApiResponse<SavedLocationResponse>> update(
            @CurrentUser PbUserEntity user,
            @PathVariable String locationEntityId,
            @Valid @RequestBody UpdateLocationRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(ResponseMessages.LOCATION_UPDATED,
                savedLocationService.update(user, locationEntityId, req)));
    }

    @DeleteMapping(ApiMapping.LOCATION_ID)
    @Operation(summary = "Delete a saved location")
    public ResponseEntity<ApiResponse<Void>> delete(
            @CurrentUser PbUserEntity user,
            @PathVariable String locationEntityId) {
        savedLocationService.delete(user, locationEntityId);
        return ResponseEntity.ok(ApiResponse.message(ResponseMessages.LOCATION_DELETED));
    }
}
