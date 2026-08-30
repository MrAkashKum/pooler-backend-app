package com.akash.pooler_backend.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe binding of all `app.*` properties.
 * Fails fast at startup if required properties are missing.
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    @NotBlank
    private String name;

    @NotBlank
    private String version;

    @NotBlank
    private String baseUrl;

    @NotBlank
    private String frontendBaseUrl;

    @NotNull
    private Jwt jwt = new Jwt();

    @NotNull
    private Security security = new Security();

    @NotNull
    private Mail mail = new Mail();

    @NotNull
    private ResendSettings resend = new ResendSettings();

    @NotNull
    private Auth auth = new Auth();

    @NotNull
    private PasswordReset passwordReset = new PasswordReset();

    @NotNull
    private EmailVerification emailVerification = new EmailVerification();

    @NotNull
    private Invitation invitation = new Invitation();

    @NotNull
    private Async async = new Async();

    // ─── Nested config classes ────────────────────────────────────

    @Getter @Setter
    public static class Jwt {
        @NotBlank
        private String secret;
        @Positive
        private long accessTokenExpiryMs  = 900_000L;       // 15 min
        @Positive
        private long refreshTokenExpiryMs = 604_800_000L;   // 7 days
        @Positive
        private long sessionTokenExpiryMs = 1_800_000L;     // 30 min
        @NotBlank
        private String issuer = "pooler-auth";
        @NotBlank
        private String audience = "pooler-mobile-app";
    }

    @Getter
    @Setter
    public static class Security {
        private int maxFailedAttempts = 5;
        private int lockDurationMinutes = 30;
        private int bcryptStrength = 12;
        private Cors cors = new Cors();

        @Getter
        @Setter
        public static class Cors {
            private String allowedOrigins = "*";
            private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS,PATCH";
        }
    }

    @Getter
    @Setter
    public static class Mail {
        @NotBlank
        private String from;
        @NotBlank
        private String fromName;
    }

    @Getter
    @Setter
    public static class ResendSettings {
        @NotBlank
        private String apiKey;
    }

    @Getter
    @Setter
    public static class Auth {
        @NotNull
        private Google google = new Google();
        @NotNull
        private Apple apple = new Apple();

        @Getter
        @Setter
        public static class Google {
            @NotBlank
            private String tokenInfoUrl;
            private String clientIds;
            @Positive
            private int requestTimeoutSeconds = 5;
            @Positive
            private int connectTimeoutSeconds = 3;
        }

        @Getter
        @Setter
        public static class Apple {
            private String clientIds;
            @NotBlank
            private String issuer = "https://appleid.apple.com";
            @NotBlank
            private String jwksUrl = "https://appleid.apple.com/auth/keys";
            @Positive
            private int requestTimeoutSeconds = 5;
            @Positive
            private int connectTimeoutSeconds = 3;
            @Positive
            private int jwksCacheMinutes = 60;
            private int allowedClockSkewSeconds = 60;
        }
    }

    @Getter
    @Setter
    public static class PasswordReset {
        private int tokenExpiryMinutes = 30;
        private int maxAttempts = 3;
    }

    @Getter
    @Setter
    public static class EmailVerification {
        private int tokenExpiryMinutes = 60;
    }

    @Getter
    @Setter
    public static class Invitation {
        @Positive
        private int defaultTtlSeconds = 300;
        @Positive
        private int declineRetryLockHours = 8;
    }

    @Getter
    @Setter
    public static class Async {
        @NotNull
        private ExecutorPool defaults = new ExecutorPool(4, 8, 100, 60);
        @NotNull
        private ExecutorPool mail = new ExecutorPool(2, 6, 100, 60);
        @NotNull
        private ExecutorPool audit = new ExecutorPool(2, 4, 500, 30);
        @NotNull
        private ExecutorPool notification = new ExecutorPool(2, 6, 200, 60);
        private int shutdownAwaitSeconds = 30;

        @Getter
        @Setter
        public static class ExecutorPool {
            private int corePoolSize;
            private int maxPoolSize;
            private int queueCapacity;
            private int keepAliveSeconds;

            public ExecutorPool() {
            }

            public ExecutorPool(int corePoolSize, int maxPoolSize, int queueCapacity, int keepAliveSeconds) {
                this.corePoolSize = corePoolSize;
                this.maxPoolSize = maxPoolSize;
                this.queueCapacity = queueCapacity;
                this.keepAliveSeconds = keepAliveSeconds;
            }
        }
    }
}
