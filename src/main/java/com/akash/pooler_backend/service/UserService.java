package com.akash.pooler_backend.service;

import com.akash.pooler_backend.dto.request.ChangePasswordRequest;
import com.akash.pooler_backend.dto.request.UpdateProfileRequest;
import com.akash.pooler_backend.dto.response.UserResponse;
import com.akash.pooler_backend.entity.PbUserEntity;

/**
 * @author Akash Kumar
 */
public interface UserService {

    UserResponse getProfile(PbUserEntity pbUserEntity);
    UserResponse updateProfile(PbUserEntity pbUserEntity, UpdateProfileRequest req);
    void changePassword(PbUserEntity pbUserEntity, ChangePasswordRequest req);
    void deleteAccount(PbUserEntity pbUserEntity);
    PbUserEntity getUserEntity(String entityId);
}
