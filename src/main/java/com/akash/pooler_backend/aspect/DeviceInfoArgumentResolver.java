package com.akash.pooler_backend.aspect;

import com.akash.pooler_backend.dto.request.DeviceInfoRequest;
import com.akash.pooler_backend.interceptors.annotation.DeviceInfo;
import com.akash.pooler_backend.utils.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@code @DeviceInfoRequest DeviceInfoRequest} parameters in controller methods.
 * Extracts device metadata from standard mobile request headers.
 * Register in {@link com.akash.pooler_backend.config.WebMvcConfig#addArgumentResolvers}.
 */
@Component
public class DeviceInfoArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter param) {
        return param.hasParameterAnnotation(DeviceInfo.class)
                && DeviceInfoRequest.class.isAssignableFrom(param.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter param,
                                  ModelAndViewContainer mvc,
                                  NativeWebRequest webReq,
                                  WebDataBinderFactory factory) {
        HttpServletRequest req = (HttpServletRequest) webReq.getNativeRequest();
        return DeviceInfoRequest.builder()
                .deviceId(req.getHeader("X-Device-Id"))
                .platform(RequestUtil.getPlatform(req))
                .appVersion(req.getHeader("X-App-Version"))
                .fcmToken(req.getHeader("X-FCM-Token"))
                .build();
    }
}
