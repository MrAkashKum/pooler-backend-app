package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.dto.request.AcceptInvitationRequest;
import com.akash.pooler_backend.dto.request.SendRideInvitationRequest;
import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.dto.response.RideInvitationResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.interceptors.annotation.CurrentUser;
import com.akash.pooler_backend.interceptors.annotation.ValidSession;
import com.akash.pooler_backend.service.RideInvitationService;
import com.akash.pooler_backend.service.RideInvitationService.ConfirmResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Ride-share invitation flow:
 *   send -> accept (with destination) -> confirm pickup -> ride created.
 *
 * @author Akash Kumar
 */
@RestController
@RequestMapping(ApiMapping.INVITATIONS_API)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Ride Invitations", description = "Two-party invitation lifecycle for shared rides")
public class RideInvitationController {

    private final RideInvitationService invitationService;

    @PostMapping
    @ValidSession(reason = "Sending an invitation requires an active session")
    @Operation(summary = "Send a ride-share invitation to another user")
    public ResponseEntity<ApiResponse<RideInvitationResponse>> send(
            @CurrentUser PbUserEntity sender,
            @Valid @RequestBody SendRideInvitationRequest req) {
        return ResponseEntity.ok(ApiResponse.created(invitationService.send(sender, req)));
    }

    @PostMapping(ApiMapping.INVITATION_ACCEPT)
    @ValidSession(reason = "Accepting an invitation requires an active session")
    @Operation(summary = "Accept an invitation and compute the common pickup hub")
    public ResponseEntity<ApiResponse<RideInvitationResponse>> accept(
            @CurrentUser PbUserEntity receiver,
            @PathVariable String invitationEntityId,
            @Valid @RequestBody AcceptInvitationRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(ResponseMessages.INVITATION_ACCEPTED,
                invitationService.accept(receiver, invitationEntityId, req)));
    }

    @PostMapping(ApiMapping.INVITATION_DECLINE)
    @Operation(summary = "Decline an invitation")
    public ResponseEntity<ApiResponse<RideInvitationResponse>> decline(
            @CurrentUser PbUserEntity receiver,
            @PathVariable String invitationEntityId) {
        return ResponseEntity.ok(ApiResponse.ok(ResponseMessages.INVITATION_DECLINED,
                invitationService.decline(receiver, invitationEntityId)));
    }

    @PostMapping(ApiMapping.INVITATION_CONFIRM_PICKUP)
    @ValidSession(reason = "Confirming pickup creates a ride and requires an active session")
    @Operation(summary = "Confirm the suggested pickup hub; ride is created when both parties confirm")
    public ResponseEntity<ApiResponse<ConfirmResult>> confirmPickup(
            @CurrentUser PbUserEntity user,
            @PathVariable String invitationEntityId) {
        ConfirmResult result = invitationService.confirmPickup(user, invitationEntityId);
        String message = result.ride() != null
                ? ResponseMessages.INVITATION_RIDE_CREATED
                : ResponseMessages.INVITATION_PICKUP_CONFIRMED_WAITING;
        return ResponseEntity.ok(ApiResponse.ok(message, result));
    }

    @PostMapping(ApiMapping.INVITATION_CANCEL)
    @Operation(summary = "Cancel an invitation (either party)")
    public ResponseEntity<ApiResponse<RideInvitationResponse>> cancel(
            @CurrentUser PbUserEntity user,
            @PathVariable String invitationEntityId) {
        return ResponseEntity.ok(ApiResponse.ok(ResponseMessages.INVITATION_CANCELLED,
                invitationService.cancel(user, invitationEntityId)));
    }

    @GetMapping(ApiMapping.INVITATION_ID)
    @Operation(summary = "Fetch a single invitation")
    public ResponseEntity<ApiResponse<RideInvitationResponse>> get(
            @CurrentUser PbUserEntity user,
            @PathVariable String invitationEntityId) {
        return ResponseEntity.ok(ApiResponse.ok(invitationService.get(user, invitationEntityId)));
    }

    @GetMapping(ApiMapping.INBOX)
    @Operation(summary = "List invitations received")
    public ResponseEntity<ApiResponse<List<RideInvitationResponse>>> inbox(@CurrentUser PbUserEntity user) {
        return ResponseEntity.ok(ApiResponse.ok(invitationService.inbox(user)));
    }

    @GetMapping(ApiMapping.OUTBOX)
    @Operation(summary = "List invitations sent")
    public ResponseEntity<ApiResponse<List<RideInvitationResponse>>> outbox(@CurrentUser PbUserEntity user) {
        return ResponseEntity.ok(ApiResponse.ok(invitationService.outbox(user)));
    }
}
