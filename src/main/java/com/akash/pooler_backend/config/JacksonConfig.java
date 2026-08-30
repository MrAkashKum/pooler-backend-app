package com.akash.pooler_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 3 configuration for Spring Boot 4.
 *
 * IMPORTANT MIGRATION NOTE
 * ────────────────────────
 * Spring Boot 4 ships Jackson 3, which lives in the {@code tools.jackson.*}
 * package (NOT {@code com.fasterxml.jackson.*} which was Jackson 2).
 *
 * The previous version of this class created a Jackson 2 {@code ObjectMapper}
 * bean — Spring Boot 4's {@code AbstractJacksonHttpMessageConverter} ignores
 * that bean entirely because it expects a Jackson 3 {@code JsonMapper},
 * leaving the auto-configured mapper untouched and unconfigured.
 *
 * Instead of replacing the auto-configured mapper, we contribute to it via a
 * {@link JsonMapperBuilderCustomizer}. Spring Boot will pick this up while
 * building the default {@code JsonMapper} bean.
 *
 * Combined with the explicit {@code @NoArgsConstructor} on every request DTO,
 * this resolves the Jackson 3 error:
 *   "Cannot construct instance of ... (no Creators, like default constructor, exist)"
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
        // You can also add .findAndRegisterModules() to support Java 8 Dates
    }
}
