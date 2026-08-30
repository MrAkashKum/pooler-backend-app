package com.akash.pooler_backend.config;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "profile-media")
public class ProfileMediaProperties {
    private String bucket;
    private String privateBucket;
    private String keyPrefix;
    private String publicBaseUrl;
    private long privateUrlExpiryMinutes = 5;

    @Positive
    private long maxSizeMb;
}
