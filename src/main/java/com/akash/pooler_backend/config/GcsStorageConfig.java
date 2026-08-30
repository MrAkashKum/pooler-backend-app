package com.akash.pooler_backend.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcsStorageConfig {

    @Bean
    public Storage profileMediaStorage() {
        return StorageOptions.getDefaultInstance().getService();
    }
}
