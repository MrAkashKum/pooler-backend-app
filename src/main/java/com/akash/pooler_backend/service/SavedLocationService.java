package com.akash.pooler_backend.service;

import com.akash.pooler_backend.dto.request.SaveLocationRequest;
import com.akash.pooler_backend.dto.request.UpdateLocationRequest;
import com.akash.pooler_backend.dto.response.SavedLocationResponse;
import com.akash.pooler_backend.entity.PbUserEntity;

import java.util.List;

/**
 * @author Akash Kumar
 */
public interface SavedLocationService {

    SavedLocationResponse create(PbUserEntity user, SaveLocationRequest req);

    List<SavedLocationResponse> listForUser(PbUserEntity user);

    SavedLocationResponse get(PbUserEntity user, String locationEntityId);

    SavedLocationResponse update(PbUserEntity user, String locationEntityId, UpdateLocationRequest req);

    void delete(PbUserEntity user, String locationEntityId);
}
