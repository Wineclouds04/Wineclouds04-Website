package com.example.blog.media.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.blog.media.storage.AliyunOssObjectStorage;
import com.example.blog.media.storage.ObjectStorage;

@Configuration
@EnableConfigurationProperties({OssProperties.class, MediaLifecycleProperties.class})
public class OssConfig {

    @Bean
    ObjectStorage objectStorage(OssProperties properties) {
        return new AliyunOssObjectStorage(properties);
    }
}
