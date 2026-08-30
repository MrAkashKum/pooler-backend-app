package com.akash.pooler_backend.service;

import com.akash.pooler_backend.dto.request.AcceptInvitationRequest;
import com.akash.pooler_backend.dto.request.SendRideInvitationRequest;
import com.akash.pooler_backend.dto.response.RideInvitationResponse;
import com.akash.pooler_backend.dto.response.RideResponse;
import com.akash.pooler_backend.entity.PbUserEntity;

import java.util.List;

/**
 * Manages the invitation flow for the "Ride-Sharing Toggle".
 *
 * @author Akash Kumar
 */
public interface RideInvitationService {

    RideInvitationResponse send(PbUserEntity sender, SendRideInvitationRequest req);

    RideInvitationResponse accept(PbUserEntity receiver, String invitationEntityId, AcceptInvitationRequest req);

    RideInvitationResponse decline(PbUserEntity receiver, String invitationEntityId);

    /** Either user (sender or receiver) confirms the suggested pickup hub. */
    ConfirmResult confirmPickup(PbUserEntity user, String invitationEntityId);

    RideInvitationResponse cancel(PbUserEntity user, String invitationEntityId);

    RideInvitationResponse get(PbUserEntity user, String invitationEntityId);

    List<RideInvitationResponse> inbox(PbUserEntity user);

    List<RideInvitationResponse> outbox(PbUserEntity user);

    /**
     * Returned wrapper after a confirm-pickup call: either the latest
     * invitation snapshot, or the freshly-created ride if both sides
     * have now confirmed.
     */
    record ConfirmResult(RideInvitationResponse invitation, RideResponse ride) {}
}
