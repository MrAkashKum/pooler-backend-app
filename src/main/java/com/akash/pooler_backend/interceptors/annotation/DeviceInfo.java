package com.akash.pooler_backend.interceptors.annotation;

import java.lang.annotation.*;

/**
 * Injects a populated {@link com.akash.pooler_backend.dto.request.DeviceInfoRequest}
 * into controller method parameters from incoming mobile request headers.
 * Headers read:
 *   X-Device-Id    → deviceId
 *   X-Platform     → platform  (ANDROID | IOS | WEB)
 *   X-App-Version  → appVersion
 *   X-FCM-Token    → fcmToken  (Firebase push notification token)
 * Usage:
 *   @PostMapping("/login")
 *   public ResponseEntity<?> login(
 *       @RequestBody LoginRequest req,
 *       @DeviceInfo DeviceInfoRequest device) { ... }
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DeviceInfo {}
