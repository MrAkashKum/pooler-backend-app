package com.akash.pooler_backend.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Global CORS configuration that runs BEFORE Spring Security.
 *
 * This ensures:
 * 1. Swagger UI can fetch /v3/api-docs without CORS errors
 * 2. Preflight OPTIONS requests are handled correctly
 * 3. All API endpoints allow cross-origin requests from configured origins
 *
 * @author Akash kumar
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final AppProperties appProperties;

    /**
     * CORS filter registered with highest precedence to run before Spring Security.
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistrationBean() {
        CorsConfiguration config = new CorsConfiguration();

        String originsProperty = appProperties.getSecurity().getCors().getAllowedOrigins();
        String methodsProperty = appProperties.getSecurity().getCors().getAllowedMethods();

        List<String> originPatterns = parseCsv(originsProperty);
        config.setAllowedOriginPatterns(originPatterns);
        log.info("CORS: Allowing origin patterns: {}", originPatterns);

        // Configure allowed methods
        config.setAllowedMethods(parseCsv(methodsProperty));

        // Configure allowed headers - be generous for API clients
        config.setAllowedHeaders(List.of(
                "*"  // Allow all headers for simplicity
        ));

        // Expose headers that clients may need to read
        config.setExposedHeaders(List.of(
                "Authorization",
                "X-Correlation-ID",
                "X-Refresh-Token",
                "Content-Disposition",
                "X-Total-Count",
                "X-Page-Number",
                "X-Page-Size"
        ));

        // Allow credentials (cookies, authorization headers)
        // Note: When allowCredentials is true, allowedOrigins cannot contain "*"
        if (!"*".equals(originsProperty)) {
            config.setAllowCredentials(true);
        } else {
            config.setAllowCredentials(false);
        }

        // Cache preflight response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply CORS configuration to ALL paths including swagger
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        // Run BEFORE Spring Security filter chain
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        bean.setName("corsFilter");

        log.info("CORS filter registered with highest precedence");
        return bean;
    }

    /**
     * Additional filter to handle CORS preflight for paths that might bypass the CorsFilter.
     * This is a fallback for edge cases.
     */
    @Bean
    public FilterRegistrationBean<Filter> additionalCorsFilter() {
        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletRequest req = (HttpServletRequest) request;
                HttpServletResponse res = (HttpServletResponse) response;

                String origin = req.getHeader("Origin");
                if (origin != null) {
                    CorsConfiguration corsConfiguration = buildCorsConfiguration();
                    String allowedOrigin = corsConfiguration.checkOrigin(origin);
                    if (allowedOrigin != null) {
                        String requestedHeaders = req.getHeader("Access-Control-Request-Headers");

                        res.setHeader("Access-Control-Allow-Origin", allowedOrigin);
                        res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD");
                        res.setHeader("Access-Control-Allow-Headers", requestedHeaders != null ? requestedHeaders : "*");
                        res.setHeader("Access-Control-Expose-Headers", "Authorization, X-Correlation-ID, X-Refresh-Token");
                        res.setHeader("Access-Control-Max-Age", "3600");
                        res.addHeader("Vary", "Origin");
                        res.addHeader("Vary", "Access-Control-Request-Method");
                        res.addHeader("Vary", "Access-Control-Request-Headers");

                        if (!"*".equals(appProperties.getSecurity().getCors().getAllowedOrigins())) {
                            res.setHeader("Access-Control-Allow-Credentials", "true");
                        }
                    }
                }

                // Handle preflight
                if ("OPTIONS".equalsIgnoreCase(req.getMethod()) && res.getHeader("Access-Control-Allow-Origin") != null) {
                    res.setStatus(HttpServletResponse.SC_OK);
                    return;
                }

                chain.doFilter(request, response);
            }
        });
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        bean.setName("additionalCorsFilter");
        return bean;
    }

    private CorsConfiguration buildCorsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();
        String originsProperty = appProperties.getSecurity().getCors().getAllowedOrigins();
        config.setAllowedOriginPatterns(parseCsv(originsProperty));
        config.setAllowCredentials(!"*".equals(originsProperty));
        return config;
    }

    private static List<String> parseCsv(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
