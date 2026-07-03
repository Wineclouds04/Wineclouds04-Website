package com.example.blog.shared.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("blog.runtime")
public record RuntimeProperties(
        String project,
        String environment,
        boolean cacheEnabled,
        Duration articleCacheTtl,
        Duration listCacheTtl,
        Duration taxonomyCacheTtl
) {
    public String key(String module, String businessKey) {
        return "%s:%s:%s:%s".formatted(project, environment, module, businessKey);
    }
}
