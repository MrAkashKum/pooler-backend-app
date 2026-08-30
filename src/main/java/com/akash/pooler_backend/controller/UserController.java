package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.dto.request.ChangePasswordRequest;
import com.akash.pooler_backend.dto.request.UpdateProfileRequest;
import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.dto.response.UserResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.ProfileMediaPurpose;
import com.akash.pooler_backend.interceptors.annotation.CurrentUser;
import com.akash.pooler_backend.interceptors.annotation.ValidSession;
import com.akash.pooler_backend.service.ProfileMediaService;
import com.akash.pooler_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Akash Kumar
 */
@RestController
@RequestMapping(ApiMapping.USERS_API)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Profile", description = "Profile management for authenticated users")
public class UserController {

    private final UserService userService;
    private final ProfileMediaService profileMediaService;

    @GetMapping(ApiMapping.ME)
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@CurrentUser PbUserEntity pbUserEntity) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(pbUserEntity)));
    }

    @PutMapping(ApiMapping.ME)
    @Operation(summary = "Update profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @CurrentUser PbUserEntity pbUserEntity,
            @Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(ResponseMessages.PROFILE_UPDATED, userService.updateProfile(pbUserEntity, req)));
    }

    @PostMapping(value = ApiMapping.MEDIA, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ValidSession(reason = "Updating profile media requires an active session")
    @Operation(summary = "Upload optional profile photo or payment QR image to S3")
    public ResponseEntity<ApiResponse<UserResponse>> uploadProfileMedia(
            @CurrentUser PbUserEntity pbUserEntity,
            @RequestParam ProfileMediaPurpose purpose,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(ResponseMessages.PROFILE_MEDIA_UPDATED,
                profileMediaService.uploadProfileMedia(pbUserEntity, purpose, file)));
    }

    @GetMapping(ApiMapping.MEDIA)
    @Operation(summary = "Create a short-lived owner-only URL for private profile media")
    public ResponseEntity<ApiResponse<String>> getPrivateProfileMedia(
            @CurrentUser PbUserEntity pbUserEntity,
            @RequestParam ProfileMediaPurpose purpose) {
        return ResponseEntity.ok(ApiResponse.ok(
                profileMediaService.createOwnerDownloadUrl(pbUserEntity, purpose)));
    }

    @PutMapping(ApiMapping.CHANGE_PASSWORD)
    @Operation(summary = "Change password (requires current password)")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @CurrentUser PbUserEntity pbUserEntity,
            @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(pbUserEntity, req);
        return ResponseEntity.ok(ApiResponse.message(ResponseMessages.PASSWORD_CHANGED_LOGIN_AGAIN));
    }

    @DeleteMapping(ApiMapping.ME)
    @Operation(summary = "Delete account permanently")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@CurrentUser PbUserEntity pbUserEntity) {
        userService.deleteAccount(pbUserEntity);
        return ResponseEntity.ok(ApiResponse.message(ResponseMessages.ACCOUNT_DELETED));
    }
}
