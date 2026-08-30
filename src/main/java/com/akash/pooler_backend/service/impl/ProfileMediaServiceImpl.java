package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.config.ProfileMediaProperties;
import com.akash.pooler_backend.dto.response.UserResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.ErrorCode;
import com.akash.pooler_backend.enums.ProfileMediaPurpose;
import com.akash.pooler_backend.exception.FileUploadException;
import com.akash.pooler_backend.interceptors.annotation.AuditAction;
import com.akash.pooler_backend.repository.PbUserRepository;
import com.akash.pooler_backend.service.ProfileMediaService;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.URLEncoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileMediaServiceImpl implements ProfileMediaService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final String METHOD_UPLOAD_PROFILE_MEDIA = "uploadProfileMedia";

    private final PbUserRepository userRepository;
    private final ProfileMediaProperties properties;
    private final Storage profileMediaStorage;

    @Override
    @AuditAction("PROFILE_MEDIA_UPLOAD")
    public UserResponse uploadProfileMedia(PbUserEntity user, ProfileMediaPurpose purpose, MultipartFile file) {
        validate(file);
        String bucket = bucketFor(purpose);
        if (bucket == null || bucket.isBlank()) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR, ResponseMessages.GCS_BUCKET_NOT_CONFIGURED);
        }
        if (properties.getKeyPrefix() == null || properties.getKeyPrefix().isBlank()) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR, ResponseMessages.GCS_KEY_PREFIX_NOT_CONFIGURED);
        }

        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        String key = buildKey(user.getEntityId(), purpose, contentType);
        try {
            profileMediaStorage.createFrom(
                    BlobInfo.newBuilder(BlobId.of(bucket, key)).setContentType(contentType).build(),
                    file.getInputStream());
        } catch (IOException exception) {
            log.error("profileMediaReadFailed className={} methodName={} userId={} purpose={} exceptionType={}",
                    getClass().getSimpleName(), METHOD_UPLOAD_PROFILE_MEDIA, user.getEntityId(), purpose,
                    exception.getClass().getSimpleName(), exception);
            throw new FileUploadException(ResponseMessages.PROFILE_MEDIA_READ_FAILED);
        } catch (RuntimeException exception) {
            log.error("profileMediaUploadFailed className={} methodName={} userId={} purpose={} exceptionType={}",
                    getClass().getSimpleName(), METHOD_UPLOAD_PROFILE_MEDIA, user.getEntityId(), purpose,
                    exception.getClass().getSimpleName(), exception);
            throw new FileUploadException(ResponseMessages.PROFILE_MEDIA_GCS_UPLOAD_FAILED);
        }

        String mediaUrl = purpose == ProfileMediaPurpose.PAYMENT_QR
                ? privateObjectReference(key)
                : publicUrl(key);
        if (purpose == ProfileMediaPurpose.PROFILE_PHOTO) {
            user.setProfilePictureUrl(mediaUrl);
        } else {
            user.setPaymentQrCodeUrl(mediaUrl);
        }
        log.info("profileMediaUploaded className={} methodName={} userId={} purpose={} contentType={} sizeBytes={}",
                getClass().getSimpleName(), METHOD_UPLOAD_PROFILE_MEDIA, user.getEntityId(), purpose, contentType, file.getSize());
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    public String createOwnerDownloadUrl(PbUserEntity user, ProfileMediaPurpose purpose) {
        String storedReference = purpose == ProfileMediaPurpose.PAYMENT_QR
                ? user.getPaymentQrCodeUrl()
                : user.getProfilePictureUrl();
        if (storedReference == null || storedReference.isBlank()) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR, ResponseMessages.PROFILE_MEDIA_REQUIRED);
        }
        if (purpose != ProfileMediaPurpose.PAYMENT_QR) {
            return storedReference;
        }
        String key = keyFromPrivateReference(storedReference);
        return profileMediaStorage.signUrl(
                        BlobInfo.newBuilder(BlobId.of(properties.getPrivateBucket(), key)).build(),
                        properties.getPrivateUrlExpiryMinutes(),
                        TimeUnit.MINUTES,
                        Storage.SignUrlOption.withV4Signature())
                .toString();
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR, ResponseMessages.PROFILE_MEDIA_REQUIRED);
        }
        if (file.getSize() > properties.getMaxSizeMb() * 1024 * 1024) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR,
                    ResponseMessages.profileMediaMaxSize(properties.getMaxSizeMb()));
        }
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR, ResponseMessages.PROFILE_MEDIA_IMAGE_ONLY);
        }
    }

    private String buildKey(String userEntityId, ProfileMediaPurpose purpose, String contentType) {
        String extension = switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
        return "%s/%s/%s/%s.%s".formatted(
                trimSlashes(properties.getKeyPrefix()),
                userEntityId,
                purpose.name().toLowerCase(Locale.ROOT).replace('_', '-'),
                UUID.randomUUID().toString().replace("-", ""),
                extension);
    }

    private String publicUrl(String key) {
        if (properties.getPublicBaseUrl() != null && !properties.getPublicBaseUrl().isBlank()) {
            return trimTrailingSlash(properties.getPublicBaseUrl()) + "/" + key;
        }
        return "https://storage.googleapis.com/%s/%s".formatted(properties.getBucket(), encodeKey(key));
    }

    private String privateObjectReference(String key) {
        return "gs://%s/%s".formatted(properties.getPrivateBucket(), key);
    }

    private String keyFromPrivateReference(String reference) {
        URI uri;
        try {
            uri = URI.create(reference);
        } catch (IllegalArgumentException exception) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR, ResponseMessages.PROFILE_MEDIA_REQUIRED);
        }
        String expectedHost = properties.getPrivateBucket();
        if (!"gs".equals(uri.getScheme()) || !expectedHost.equals(uri.getHost())) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR, ResponseMessages.PROFILE_MEDIA_REQUIRED);
        }
        String key = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        String expectedUserSegment = "/" + "payment-qr/";
        if (key.isBlank() || !key.contains(expectedUserSegment)) {
            throw new FileUploadException(ErrorCode.VALIDATION_ERROR, ResponseMessages.PROFILE_MEDIA_REQUIRED);
        }
        return key;
    }

    private String bucketFor(ProfileMediaPurpose purpose) {
        return purpose == ProfileMediaPurpose.PAYMENT_QR
                ? properties.getPrivateBucket()
                : properties.getBucket();
    }

    private static String encodeKey(String key) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
    }

    private static String trimSlashes(String value) {
        String trimmed = value.trim();
        trimmed = trimmed.replaceAll("^/+", "").replaceAll("/+$", "");
        return trimmed;
    }

    private static String trimTrailingSlash(String value) {
        return value.replaceAll("/+$", "");
    }
}
