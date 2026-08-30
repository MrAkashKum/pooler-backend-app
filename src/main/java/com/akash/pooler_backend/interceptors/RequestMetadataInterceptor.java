package com.akash.pooler_backend.interceptors;

import com.akash.pooler_backend.utils.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Extracts mobile client metadata from headers and attaches to request attributes.
 * Downstream services can read req.getAttribute("platform") etc.
 */
@Slf4j
@Component
public class RequestMetadataInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest req,
                             @NonNull HttpServletResponse res,
                             @NonNull Object handler) {
        req.setAttribute("deviceId",   req.getHeader("X-Device-Id"));
        req.setAttribute("platform",   RequestUtil.getPlatform(req));    // ANDROID | IOS | WEB
        req.setAttribute("appVersion", req.getHeader("X-App-Version"));
        req.setAttribute("fcmToken",   req.getHeader("X-FCM-Token"));
        return true;
    }
}
