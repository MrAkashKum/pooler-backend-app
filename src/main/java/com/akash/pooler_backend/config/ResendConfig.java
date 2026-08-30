package com.akash.pooler_backend.config;

import com.resend.Resend;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResendConfig {

    @Bean
    public Resend resendClient(AppProperties properties) {
        return new Resend(properties.getResend().getApiKey());
    }
}
