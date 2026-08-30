package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.dto.request.MidpointRequest;
import com.akash.pooler_backend.dto.request.RouteCompatibilityRequest;
import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.dto.response.CommonPickupPointResponse;
import com.akash.pooler_backend.dto.response.DistanceResponse;
import com.akash.pooler_backend.dto.response.RouteCompatibilityResponse;
import com.akash.pooler_backend.service.GeoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Stateless geo math: distance, midpoint (Meet-in-the-Middle hub),
 * and route-compatibility (Overlap Rule).
 *
 * @author Akash Kumar
 */
@RestController
@RequestMapping(ApiMapping.GEO_API)
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Geo Math", description = "Distance, midpoint and route-compatibility helpers")
public class GeoController {

    private final GeoService geoService;

    @GetMapping(ApiMapping.DISTANCE)
    @Operation(summary = "Great-circle distance and bearing between two coordinates")
    public ResponseEntity<ApiResponse<DistanceResponse>> distance(
            @RequestParam @DecimalMin("-90.0")  @DecimalMax("90.0")  double lat1,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lng1,
            @RequestParam @DecimalMin("-90.0")  @DecimalMax("90.0")  double lat2,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lng2) {
        return ResponseEntity.ok(ApiResponse.ok(geoService.distance(lat1, lng1, lat2, lng2)));
    }

    @PostMapping(ApiMapping.MIDPOINT)
    @Operation(summary = "Compute the common pickup hub between two users")
    public ResponseEntity<ApiResponse<CommonPickupPointResponse>> midpoint(
            @Valid @RequestBody MidpointRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(geoService.computeCommonPickup(
                req.getUserALatitude(), req.getUserALongitude(),
                req.getUserBLatitude(), req.getUserBLongitude())));
    }

    @PostMapping(ApiMapping.ROUTE_COMPATIBILITY)
    @Operation(summary = "Run the Overlap Rule for two trips")
    public ResponseEntity<ApiResponse<RouteCompatibilityResponse>> compatibility(
            @Valid @RequestBody RouteCompatibilityRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(geoService.computeCompatibility(
                req.getUserAOriginLat(), req.getUserAOriginLng(),
                req.getUserADestLat(),   req.getUserADestLng(),
                req.getUserBOriginLat(), req.getUserBOriginLng(),
                req.getUserBDestLat(),   req.getUserBDestLng())));
    }
}
