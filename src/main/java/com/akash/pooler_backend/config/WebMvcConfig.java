package com.akash.pooler_backend.config;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.aspect.CurrentUserArgumentResolver;
import com.akash.pooler_backend.aspect.DeviceInfoArgumentResolver;
import com.akash.pooler_backend.interceptors.AuthInterceptor;
import com.akash.pooler_backend.interceptors.RequestMetadataInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * @author Akash kumar
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final RequestMetadataInterceptor requestMetadataInterceptor;
    private final CurrentUserArgumentResolver currentUserResolver;
    private final DeviceInfoArgumentResolver deviceInfoResolver;
    private final AppProperties appProperties;

    /**
     * Configure CORS at the WebMvc level as an additional layer.
     * This works alongside CorsConfig and SecurityConfig for comprehensive CORS support.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String originsProperty = appProperties.getSecurity().getCors().getAllowedOrigins();
        String methodsProperty = appProperties.getSecurity().getCors().getAllowedMethods();

        String[] origins = "*".equals(originsProperty)
                ? new String[]{"*"}
                : Arrays.stream(originsProperty.split(",")).map(String::trim).toArray(String[]::new);

        String[] methods = methodsProperty.split(",");

        registry.addMapping(ApiMapping.ALL)
                .allowedOriginPatterns(origins)
                .allowedMethods(methods)
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "X-Correlation-ID", "X-Refresh-Token")
                .allowCredentials(!"*".equals(originsProperty))
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestMetadataInterceptor).addPathPatterns(ApiMapping.ALL);
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(ApiMapping.API_ROOT_MATCHER)
                .excludePathPatterns(ApiMapping.AUTH_API + ApiMapping.ALL, ApiMapping.PUBLIC_API + ApiMapping.ALL,
                        ApiMapping.V3_API_DOCS_ALL, ApiMapping.SWAGGER_UI, ApiMapping.H2_CONSOLE_ALL);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserResolver);
        resolvers.add(deviceInfoResolver);
    }
}
