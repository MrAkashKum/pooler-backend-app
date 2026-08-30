package com.akash.pooler_backend.dto.request;

import com.akash.pooler_backend.enums.PlatformType;
import lombok.Builder;
import lombok.Getter;

/**
 * Immutable value object carrying mobile device metadata extracted
 * from HTTP request headers by {@link com.akash.pooler_backend.aspect.DeviceInfoArgumentResolver}.
 *
 * Populated from:
 *   X-Device-Id    — unique device identifier (UUID or Android ID)
 *   X-Platform     — ANDROID | IOS | WEB
 *   X-App-Version  — semver string, e.g. "2.1.0"
 *   X-FCM-Token    — Firebase Cloud Messaging push token
 */
@Getter
@Builder
public class DeviceInfoRequest {

    private final String deviceId;
    private final String platform;
    private final String appVersion;
    private final String fcmToken;

    public boolean isAndroid() {
        return PlatformType.ANDROID.name().equalsIgnoreCase(platform);
    }
    public boolean isIos() {
        return PlatformType.IOS.name().equalsIgnoreCase(platform);
    }
    public boolean isWeb() {
        return PlatformType.WEB.name().equalsIgnoreCase(platform);
    }

    /** Returns true if essential mobile device metadata is present. */
    public boolean isValid() {
        return deviceId != null && platform != null;
    }
}
