package com.akash.pooler_backend.config;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.security.CustomAccessDeniedHandler;
import com.akash.pooler_backend.security.CustomAuthEntryPoint;
import com.akash.pooler_backend.security.filter.RequestLoggingFilter;
import com.akash.pooler_backend.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  SecurityConfig — Central Spring Security Configuration          │
 * │                                                                  │
 * │  Design decisions:                                               │
 * │  ① STATELESS session — all state lives in JWT tokens           │
 * │  ② CSRF disabled — not needed for stateless REST + mobile      │
 * │  ③ Dual-layer auth: JWT filter + AuthInterceptor (session DB)  │
 * │  ④ Custom 401/403 JSON handlers for mobile clients             │
 * │  ⑤ CORS configured for Android/iOS origins                    │
 * │  ⑥ @PreAuthorize + @RequiresAuth for method-level security     │
 * │                                                                  │
 * │  Filter execution order:                                         │
 * │  RequestLoggingFilter → JwtAuthenticationFilter                 │
 * │       → UsernamePasswordAuthenticationFilter (unused)           │
 * │       → SecurityFilterChain rules                               │
 * │       → AuthInterceptor (MVC HandlerInterceptor)               │
 * │       → Controller                                              │
 * └─────────────────────────────────────────────────────────────────┘
 *
 */

/**
 * @author Akash kumar
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) /* enables @PreAuthorize / @PostAuthorize */
@RequiredArgsConstructor
public class SecurityConfig {

    // ─── Injected dependencies ─────────────────────────────────────────
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RequestLoggingFilter requestLoggingFilter;
    private final UserDetailsService userDetailsService;
    private final CustomAuthEntryPoint authEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final AppProperties appProps;

    // ─── Public routes (no token required) ────────────────────────────
    // Spring Security matches paths inside the configured servlet context;
    // controllers carry the /api/v1 prefix.
    private static final String[] PUBLIC_MATCHERS = {
            // Auth lifecycle
            // as per requirement, need then do versioning
            ApiMapping.AUTH_REGISTER_MATCHER,
            ApiMapping.AUTH_VERIFY_EMAIL_MATCHER,
            ApiMapping.AUTH_RESEND_VERIFICATION_MATCHER,
            ApiMapping.AUTH_LOGIN_MATCHER,
            ApiMapping.AUTH_GOOGLE_MATCHER,
            ApiMapping.AUTH_APPLE_MATCHER,
            ApiMapping.AUTH_REFRESH_MATCHER,
            ApiMapping.AUTH_FORGOT_PASSWORD_MATCHER,
            ApiMapping.AUTH_RESET_PASSWORD_MATCHER,
            // Public info (health, version)
            ApiMapping.PUBLIC_API + ApiMapping.ALL,
            // API documentation
            ApiMapping.V3_API_DOCS,
            ApiMapping.V3_API_DOCS_ALL,
            ApiMapping.V3_API_DOCS_YAML,
            ApiMapping.SWAGGER_UI,
            ApiMapping.SWAGGER_UI_HTML,
            ApiMapping.SWAGGER_RESOURCES,
            ApiMapping.WEBJARS
    };

    // ─── Main security filter chain ────────────────────────────────────
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                //  Disable CSRF — stateless REST API, no cookie sessions
                .csrf(AbstractHttpConfigurer::disable)

                //  CORS — configured for mobile client origins
                //  CORS — handled by CorsConfig with highest precedence filter
                //  This enables Spring Security's CORS support to work with our CorsFilter
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                //  No HttpSession — every request must carry a JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                //  Custom error handlers for mobile JSON clients
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)   // 401
                        .accessDeniedHandler(accessDeniedHandler))  // 403

                //  Authorization rules
                .authorizeHttpRequests(auth -> auth
                        //  PathRequest.toH2Console() — works correctly for H2 servlet
                        // in Spring Security 6; MvcRequestMatcher(ApiMapping.H2_CONSOLE_ALL) does NOT.
                        .requestMatchers(PathRequest.toH2Console()).permitAll()
                        // Public — no token needed
                        .requestMatchers(PUBLIC_MATCHERS).permitAll()
                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, ApiMapping.ALL).permitAll()
                        // Monitoring endpoints are exposed for Postman/ops only.
                        .requestMatchers(ApiMapping.ACTUATOR_API + ApiMapping.ALL).hasRole("ADMIN")
                        // Role-restricted admin routes
                        .requestMatchers(ApiMapping.ADMIN_SECURITY_API + ApiMapping.ALL).hasRole("ADMIN")
                        .requestMatchers(ApiMapping.MODERATOR_SECURITY_API + ApiMapping.ALL).hasAnyRole("ADMIN", "MODERATOR")
                        // Everything else requires a valid JWT
                        .anyRequest().authenticated())

                //     Allow H2 console iframes — sameOrigin needed so H2's embedded
                //    iframe can render inside the same browser origin. The matcher
                //    mirrors ⑤ above so the header rule covers exactly the same paths.
                .headers(headers ->
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

                // AuthenticationProvider is NOT a @Bean anymore.
                //    It is wired directly into the ProviderManager inside
                //    authenticationManager() below. This eliminates the Spring Boot WARN:
                //    "Global AuthenticationManager configured with an AuthenticationProvider
                //     bean. UserDetailsService beans will not be used…"
                .authenticationManager(authenticationManager())

                // ⑧ Filter chain order:
                //    1st — RequestLoggingFilter (attaches correlation ID, logs request)
                //    2nd — JwtAuthenticationFilter (validates token, sets SecurityContext)
                .addFilterBefore(requestLoggingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthFilter, RequestLoggingFilter.class)

                .build();
    }

    // ─── Authentication manager ────────────────────────────────────────

    /**
     * This bean is still injectable by AuthServiceImpl via @Autowired /
     * constructor injection as it is a @Bean here.
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        // Do NOT hide UserNotFoundException — we handle generic messages ourselves
        provider.setHideUserNotFoundExceptions(false);
        return new ProviderManager(provider);
    }

    /**
     * BCrypt password encoder.
     * Strength is profile-driven:
     *   dev/test → 4 (fast)
     *   staging   → 10
     *   prod      → 12 (OWASP recommended minimum)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(appProps.getSecurity().getBcryptStrength());
    }

    // ─── CORS configuration ────────────────────────────────────────────

    /**
     * CORS configuration source for Spring Security.
     * Works in conjunction with CorsConfig's filter which runs at highest precedence.
     *
     * This ensures:
     * 1. Spring Security honors CORS headers set by our filter
     * 2. Swagger UI at localhost can call /v3/api-docs and /api/v1/** endpoints
     * 3. Mobile apps from any origin can access the API in dev mode
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        String originsProperty = appProps.getSecurity().getCors().getAllowedOrigins();
        String methodsProperty = appProps.getSecurity().getCors().getAllowedMethods();

        // Use origin patterns so local Expo dev ports such as localhost:8081 are accepted.
        config.setAllowedOriginPatterns(parseCsv(originsProperty));

        // HTTP methods - include all REST methods
        config.setAllowedMethods(parseCsv(methodsProperty));

        // Standard + mobile-specific headers
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "X-Device-Id",
                "X-Platform",
                "X-App-Version",
                "X-FCM-Token",
                "X-Session-Token",
                "X-Correlation-ID"
        ));

        //Check if any-other happen

        // Headers visible to clients (Swagger UI needs to read some of these)
        config.setExposedHeaders(List.of(
                "Authorization",
                "X-Correlation-ID",
                "X-Refresh-Token",
                "Content-Disposition",
                "X-Total-Count"
        ));
        // Credentials - only if not using wildcard origins

        // config.setAllowCredentials(false);
        config.setAllowCredentials(!"*".equals(originsProperty));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(ApiMapping.ALL, config);
        return source;
    }

    private static List<String> parseCsv(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
