package com.akash.pooler_backend.service;

import com.akash.pooler_backend.dto.response.UserResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.ProfileMediaPurpose;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileMediaService {

    UserResponse uploadProfileMedia(PbUserEntity user, ProfileMediaPurpose purpose, MultipartFile file);

    String createOwnerDownloadUrl(PbUserEntity user, ProfileMediaPurpose purpose);
}
