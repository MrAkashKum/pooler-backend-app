package com.akash.pooler_backend.utils;

import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.enums.PlatformType;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
/*
Request util
 */
public final class RequestUtil {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For","Proxy-Client-IP","WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR","HTTP_X_FORWARDED","HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP","HTTP_FORWARDED_FOR","HTTP_FORWARDED","HTTP_VIA",
            "REMOTE_ADDR"
    };

    private RequestUtil(){
        throw new IllegalStateException(ResponseMessages.UTILITY_CLASS);
    }

    public static String extractBearerToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (StringUtils.isNotBlank(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    public static String getClientIp(HttpServletRequest req) {
        for (String header : IP_HEADERS) {
            String ip = req.getHeader(header);
            if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return req.getRemoteAddr();
    }

    public static String getDeviceId(HttpServletRequest req)   {
        return req.getHeader("X-Device-Id");
    }

    public static String getPlatform(HttpServletRequest req)   {
        return resolvePlatform(req);
    }

    public static String getAppVersion(HttpServletRequest req) {
        return req.getHeader("X-App-Version");
    }

    public static HttpServletRequest currentRequest() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attrs.getRequest();
    }

    public static String resolvePlatform(HttpServletRequest req) {
        String explicitPlatform = normalizePlatform(req.getHeader("X-Platform"));
        if (explicitPlatform != null) {
            return explicitPlatform;
        }

        String clientHintPlatform = normalizePlatform(req.getHeader("Sec-CH-UA-Platform"));
        if (clientHintPlatform != null) {
            return clientHintPlatform;
        }

        String userAgent = req.getHeader("User-Agent");
        if (StringUtils.isBlank(userAgent)) {
            return PlatformType.WEB.name();
        }

        String normalizedUserAgent = userAgent.toLowerCase();
        if (normalizedUserAgent.contains("android")) {
            return PlatformType.ANDROID.name();
        }
        if (normalizedUserAgent.contains("iphone") || normalizedUserAgent.contains("ipad") || normalizedUserAgent.contains("ios")) {
            return PlatformType.IOS.name();
        }
        return PlatformType.WEB.name();
    }

    private static String normalizePlatform(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String normalized = value.replace("\"", "").trim().toUpperCase();
        if ("IPHONE".equals(normalized) || "IPAD".equals(normalized) || "MACOS".equals(normalized)) {
            return PlatformType.IOS.name();
        }
        for (PlatformType platformType : PlatformType.values()) {
            if (platformType.name().equals(normalized)) {
                return platformType.name();
            }
        }
        return null;
    }
}
