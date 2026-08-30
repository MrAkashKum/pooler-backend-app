package com.akash.pooler_backend.dto.response;

import com.akash.pooler_backend.entity.PbContactEntity;
import com.akash.pooler_backend.entity.PbUserEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * @author Akash Kumar
 */
@Getter
@Builder
public class ContactResponse {

    private String entityId;
    private String contactUserEntityId;
    private String contactEmail;
    private String contactFullName;
    private String contactProfilePictureUrl;
    private String nickname;
    private boolean favorite;
    private Instant createdAt;

    public static ContactResponse from(PbContactEntity c, PbUserEntity contactUser) {
        ContactResponseBuilder b = ContactResponse.builder()
                .entityId(c.getEntityId())
                .contactUserEntityId(c.getContactUserEntityId())
                .nickname(c.getNickname())
                .favorite(c.isFavorite())
                .createdAt(c.getCreatedAt());

        if (contactUser != null) {
            b.contactEmail(contactUser.getEmail())
             .contactFullName(contactUser.getFullName())
             .contactProfilePictureUrl(contactUser.getProfilePictureUrl());
        }
        return b.build();
    }
}
