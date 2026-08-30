package com.akash.pooler_backend.aspect;

import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.exception.AuthenticationException;
import com.akash.pooler_backend.interceptors.annotation.CurrentUser;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves @CurrentUser method parameters in controllers.
 * Register in WebMvcConfig.addArgumentResolvers().
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter param) {
        return param.hasParameterAnnotation(CurrentUser.class)
                && PbUserEntity.class.isAssignableFrom(param.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter param, ModelAndViewContainer mvc,
                                  NativeWebRequest req, WebDataBinderFactory factory) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof PbUserEntity)) {
            throw new AuthenticationException(ResponseMessages.AUTH_CONTEXT_MISSING);
        }
        return auth.getPrincipal();
    }
}
