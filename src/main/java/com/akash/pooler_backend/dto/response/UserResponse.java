package com.akash.pooler_backend.dto.response;

import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.Gender;
import com.akash.pooler_backend.enums.MatchPreference;
import com.akash.pooler_backend.enums.Role;
import com.akash.pooler_backend.enums.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserResponse {

    private String entityId;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private Role role;
    private UserStatus status;
    private Instant createdAt;
    private Instant lastLoginAt;
    private String profilePictureUrl;
    private String paymentQrCodeUrl;
    private boolean paymentQrCodeConfigured;
    private Gender gender;
    private MatchPreference matchPreference;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyMessage;

    public static UserResponse from(PbUserEntity pbUserEntity) {
        return UserResponse.builder()
                .entityId(pbUserEntity.getEntityId()).email(pbUserEntity.getEmail())
                .firstName(pbUserEntity.getFirstName()).lastName(pbUserEntity.getLastName())
                .fullName(pbUserEntity.getFullName()).role(pbUserEntity.getRole())
                .status(pbUserEntity.getStatus()).createdAt(pbUserEntity.getCreatedAt())
                .lastLoginAt(pbUserEntity.getLastLoginAt())
                .profilePictureUrl(pbUserEntity.getProfilePictureUrl())
                // Payment QR storage references are private. The owner requests a
                // short-lived URL from GET /users/me/media?purpose=PAYMENT_QR.
                .paymentQrCodeUrl(null)
                .paymentQrCodeConfigured(pbUserEntity.getPaymentQrCodeUrl() != null
                        && pbUserEntity.getPaymentQrCodeUrl().startsWith("s3://"))
                .gender(pbUserEntity.getGender())
                .matchPreference(MatchPreference.normalized(pbUserEntity.getMatchPreference()))
                .emergencyContactName(pbUserEntity.getEmergencyContactName())
                .emergencyContactPhone(pbUserEntity.getEmergencyContactPhone())
                .emergencyMessage(pbUserEntity.getEmergencyMessage())
                .build();
    }

}
