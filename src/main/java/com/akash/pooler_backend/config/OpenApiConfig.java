package com.akash.pooler_backend.config;

import com.akash.pooler_backend.constants.ApiMapping;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.1 / Swagger UI configuration for the Pooler backend.
 *
 * <p>Effective URLs (context path = {@code /pooler-backend}, port = 8888):
 * <ul>
 *   <li>Swagger UI &nbsp;&nbsp;&rarr; {@code http://localhost:8888/pooler-backend/swagger-ui/index.html}</li>
 *   <li>OpenAPI JSON &rarr; {@code http://localhost:8888/pooler-backend/v3/api-docs}</li>
 *   <li>OpenAPI YAML &rarr; {@code http://localhost:8888/pooler-backend/v3/api-docs.yaml}</li>
 * </ul>
 *
 * <p>The single bearer security scheme is declared once here and referenced
 * per-controller via {@code @SecurityRequirement(name = "bearerAuth")}.
 *
 * @author Akash Kumar
 */
@Configuration
@SecurityScheme(
        name         = "bearerAuth",
        type         = SecuritySchemeType.HTTP,
        scheme       = "bearer",
        bearerFormat = "JWT",
        in           = SecuritySchemeIn.HEADER,
        description  = "Paste the access-token returned by `POST /api/v1/auth/login` (without the `Bearer ` prefix)."
)
public class OpenApiConfig {

        @Value("${app.version:1.0.0}")
        private String appVersion;

        @Value("${server.servlet.contextPath:/pooler-backend}")
        private String contextPath;

        /* ──────────────────────────── Root OpenAPI document ──────────────────────────── */

        @Bean
        public OpenAPI poolerOpenAPI() {
                return new OpenAPI()
                        .info(buildInfo())
                        .servers(List.of(
                                new Server().url("http://localhost:8888" + contextPath).description("Local Dev (default profile, port 8888)"),
                                new Server().url("http://localhost:8080" + contextPath).description("Local (alt port 8080)"),
                                new Server().url("https://staging.pooler.com" + contextPath).description("Staging"),
                                new Server().url("https://api.pooler.com" + contextPath).description("Production")
                        ))
                        .tags(List.of(
                                tag("Public",            "No-auth endpoints — health & version probes used by mobile apps on cold start."),
                                tag("Authentication",    "Register, login, token refresh, password reset, logout."),
                                tag("User Profile",      "Self-service profile, password change and account deletion."),
                                tag("Sessions",          "Device-session management — list active devices, revoke a session, decode token metadata."),
                                tag("Admin",             "Privileged endpoints — list/suspend/activate users (ROLE_ADMIN only)."),
                                tag("Audit Logs",        "Append-only security audit trail for the current user (or any user, for admins)."),
                                tag("Saved Locations",   "Bookmarked places — HOME, WORK and free-form CUSTOM points."),
                                tag("Contacts",          "Friend-list / quick-invite contact book."),
                                tag("Discovery",         "Per-user ride-sharing toggle and nearby-user search."),
                                tag("Geo Math",          "Stateless helpers — Haversine distance, midpoint hub, route-overlap rule."),
                                tag("Ride Invitations",  "Two-party invitation lifecycle — send, accept, decline, confirm-pickup, cancel."),
                                tag("Rides",             "Confirmed ride lifecycle — get, active, history, status transitions, cancel."),
                                tag("Live Location",     "Real-time GPS pings during an active ride.")
                        ))
                        .components(new Components());
        }

        private Info buildInfo() {
                return new Info()
                        .title("Pooler API")
                        .version(appVersion)
                        .description("""
                        Production-grade Spring Boot backend for the **Pooler** Android / iOS cab-sharing app.
                        
                        ### Standard response envelope
                        Every endpoint (success **and** error) returns:
                        ```json
                        {
                          "success":   true,
                          "message":   "Human-readable status",
                          "errorCode": null,
                          "data":      { ... },
                          "timestamp": "2026-05-07T10:00:00Z"
                        }
                        ```
                        
                        ### Auth flow
                        1. `POST /api/v1/auth/register` &nbsp;— create pending account and send email verification
                        
                        2. `POST /api/v1/auth/verify-email` &nbsp;— activate account from the email link
                        
                        3. `POST /api/v1/auth/login` &nbsp;— get access + refresh + session tokens
                        
                        4. Send `Authorization: Bearer <accessToken>` on every protected call
                        
                        5. `POST /api/v1/auth/refresh` &nbsp;— rotate the access token before it expires
                        
                        6. `POST /api/v1/auth/logout` &nbsp;— revoke the current device session
                        
                        ### Sensitive endpoints
                        Mutating cab-share endpoints additionally require the `X-Session-Token` header
                        (returned by `/auth/login`). They are guarded by the `@ValidSession` aspect.
                        
                        ### Mobile headers
                        - `X-Device-Id` — UUID / Android-ID
                        - `X-Platform` — `ANDROID` | `IOS` | `WEB`
                        - `X-App-Version` — semver, e.g. `1.0.0`
                        - `X-FCM-Token` — Firebase push token
                        - `X-Session-Token` — current session token (for `@ValidSession` routes)
                        
                        ### Quick links
                        - **Swagger UI:** `/pooler-backend/swagger-ui/index.html`
                        - **OpenAPI JSON:** `/pooler-backend/v3/api-docs`
                        - **OpenAPI YAML:** `/pooler-backend/v3/api-docs.yaml`
                        """)
                        .contact(new Contact()
                                .name("Pooler Backend Team")
                                .email("dev@pooler.com")
                                .url("https://github.com/mrakashkumar"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT"));
        }

        private static Tag tag(String name, String description) {
                return new Tag().name(name).description(description);
        }

        /* ─────────────────────── Grouped APIs (Swagger-UI dropdown) ─────────────────────── */

        /** Everything under {@code /api/v1/**}. */
        @Bean
        public GroupedOpenApi allApi() {
                return GroupedOpenApi.builder()
                        .group("00-all")
                        .displayName("All Endpoints")
                        .pathsToMatch(ApiMapping.API_V1 + ApiMapping.ALL)
                        .build();
        }

        /** Auth, profile, sessions, admin, audit, public probes. */
        @Bean
        public GroupedOpenApi authAndUsersApi() {
                return GroupedOpenApi.builder()
                        .group("01-auth-and-users")
                        .displayName("Auth & Users")
                        .pathsToMatch(
                                ApiMapping.AUTH_API + ApiMapping.ALL,
                                ApiMapping.USERS_API + ApiMapping.ALL,
                                ApiMapping.SESSIONS_API + ApiMapping.ALL,
                                ApiMapping.ADMIN_API + ApiMapping.ALL,
                                ApiMapping.AUDIT_API + ApiMapping.ALL,
                                ApiMapping.PUBLIC_API + ApiMapping.ALL
                        )
                        .build();
        }

        /** Saved locations, contacts, discovery, geo math, invitations, rides, live location. */
        @Bean
        public GroupedOpenApi cabShareApi() {
                return GroupedOpenApi.builder()
                        .group("02-cab-share")
                        .displayName("Cab-Share Domain")
                        .pathsToMatch(
                                ApiMapping.LOCATIONS_API + ApiMapping.ALL,
                                ApiMapping.CONTACTS_API + ApiMapping.ALL,
                                ApiMapping.DISCOVERY_API + ApiMapping.ALL,
                                ApiMapping.GEO_API + ApiMapping.ALL,
                                ApiMapping.INVITATIONS_API + ApiMapping.ALL,
                                ApiMapping.RIDES_API + ApiMapping.ALL
                        )
                        .build();
        }
}
