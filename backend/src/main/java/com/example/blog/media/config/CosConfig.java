package com.example.blog.media.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.blog.media.storage.ObjectStorage;
import com.example.blog.media.storage.TencentCosObjectStorage;

@Configuration
@EnableConfigurationProperties({CosProperties.class, MediaLifecycleProperties.class})
public class CosConfig {

    @Bean
    ObjectStorage objectStorage(CosProperties properties) {
        return new TencentCosObjectStorage(properties);
    }
}
