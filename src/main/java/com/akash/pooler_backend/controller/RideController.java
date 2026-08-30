package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.dto.request.CancelRideRequest;
import com.akash.pooler_backend.dto.request.ConfirmArrivalRequest;
import com.akash.pooler_backend.dto.request.UpdateFareSplitRequest;
import com.akash.pooler_backend.dto.request.UpdateRideStatusRequest;
import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.dto.response.RideResponse;
import com.akash.pooler_backend.dto.response.ArrivalConfirmationResponse;
import com.akash.pooler_backend.dto.response.PaymentQrDownloadResponse;
import com.akash.pooler_backend.dto.response.PaymentQrShareStatusResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.interceptors.annotation.CurrentUser;
import com.akash.pooler_backend.interceptors.annotation.ValidSession;
import com.akash.pooler_backend.service.RideService;
import com.akash.pooler_backend.service.PaymentQrSharingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lifecycle of confirmed shared rides.
 *
 * @author Akash Kumar
 */
@RestController
@RequestMapping(ApiMapping.RIDES_API)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Rides", description = "Shared-ride lifecycle and history")
public class RideController {

    private final RideService rideService;
    private final PaymentQrSharingService paymentQrSharingService;

    @GetMapping(ApiMapping.RIDE_ID)
    @Operation(summary = "Fetch a single ride (must be a participant)")
    public ResponseEntity<ApiResponse<RideResponse>> get(
            @CurrentUser PbUserEntity user,
            @PathVariable String rideEntityId) {
        return ResponseEntity.ok(ApiResponse.ok(rideService.get(user, rideEntityId)));
    }

    @GetMapping(ApiMapping.ACTIVE)
    @Operation(summary = "List rides currently in progress")
    public ResponseEntity<ApiResponse<List<RideResponse>>> active(@CurrentUser PbUserEntity user) {
        return ResponseEntity.ok(ApiResponse.ok(rideService.active(user)));
    }

    @GetMapping(ApiMapping.HISTORY)
    @Operation(summary = "List completed and cancelled rides")
    public ResponseEntity<ApiResponse<List<RideResponse>>> history(@CurrentUser PbUserEntity user) {
        return ResponseEntity.ok(ApiResponse.ok(rideService.history(user)));
    }

    @PutMapping(ApiMapping.RIDE_STATUS)
    @ValidSession(reason = "Changing ride status requires an active session")
    @Operation(summary = "Advance a ride to the next lifecycle state")
    public ResponseEntity<ApiResponse<RideResponse>> updateStatus(
            @CurrentUser PbUserEntity user,
            @PathVariable String rideEntityId,
            @Valid @RequestBody UpdateRideStatusRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(ResponseMessages.RIDE_STATUS_UPDATED,
                rideService.updateStatus(user, rideEntityId, req)));
    }

    @PostMapping(ApiMapping.RIDE_CANCEL)
    @ValidSession(reason = "Cancelling a ride requires an active session")
    @Operation(summary = "Cancel a non-terminal ride")
    public ResponseEntity<ApiResponse<RideResponse>> cancel(
            @CurrentUser PbUserEntity user,
            @PathVariable String rideEntityId,
            @Valid @RequestBody(required = false) CancelRideRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(ResponseMessages.RIDE_CANCELLED,
                rideService.cancel(user, rideEntityId, req)));
    }

    @PostMapping(ApiMapping.FARE_SPLIT)
    @ValidSession(reason = "Fare split update requires an active session")
    @Operation(summary = "Store final provider fare and calculate distance-based rider shares")
    public ResponseEntity<ApiResponse<RideResponse>> fareSplit(
            @CurrentUser PbUserEntity user,
            @PathVariable String rideEntityId,
            @Valid @RequestBody UpdateFareSplitRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(ResponseMessages.FARE_SPLIT_UPDATED,
                rideService.updateFareSplit(user, rideEntityId, req)));
    }

    @PostMapping(ApiMapping.ARRIVE)
    @ValidSession(reason = "Arrival confirmation requires an active session")
    @Operation(summary = "Confirm physical arrival; cab handoff unlocks after both riders confirm")
    public ResponseEntity<ApiResponse<ArrivalConfirmationResponse>> arrive(
            @CurrentUser PbUserEntity user,
            @PathVariable String rideEntityId,
            @Valid @RequestBody(required = false) ConfirmArrivalRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(rideService.confirmArrival(user, rideEntityId, req)));
    }

    @GetMapping(ApiMapping.PAYMENT_QR_STATUS)
    @Operation(summary = "Get optional payment QR availability and ride-scoped sharing status")
    public ResponseEntity<ApiResponse<PaymentQrShareStatusResponse>> paymentQrStatus(
            @CurrentUser PbUserEntity user,
            @PathVariable String rideEntityId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentQrSharingService.status(user, rideEntityId)));
    }

    @PostMapping(ApiMapping.PAYMENT_QR_SHARE)
    @ValidSession(reason = "Sharing a payment QR requires an active session")
    @Operation(summary = "Share the current user's payment QR with the matched rider")
    public ResponseEntity<ApiResponse<PaymentQrShareStatusResponse>> sharePaymentQr(
            @CurrentUser PbUserEntity user,
            @PathVariable String rideEntityId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentQrSharingService.share(user, rideEntityId)));
    }

    @DeleteMapping(ApiMapping.PAYMENT_QR_SHARE)
    @ValidSession(reason = "Revoking a payment QR share requires an active session")
    @Operation(summary = "Revoke payment QR access for the matched rider")
    public ResponseEntity<ApiResponse<PaymentQrShareStatusResponse>> revokePaymentQr(
            @CurrentUser PbUserEntity user,
            @PathVariable String rideEntityId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentQrSharingService.revoke(user, rideEntityId)));
    }

    @GetMapping(ApiMapping.PAYMENT_QR_DOWNLOAD)
    @Operation(summary = "Get a short-lived QR URL as the owner or authorized matched rider")
    public ResponseEntity<ApiResponse<PaymentQrDownloadResponse>> paymentQrDownload(
            @CurrentUser PbUserEntity user,
            @PathVariable String rideEntityId,
            @RequestParam(required = false) String ownerEntityId) {
        return ResponseEntity.ok(ApiResponse.ok(
                paymentQrSharingService.download(user, rideEntityId, ownerEntityId)));
    }
}
