package com.example.blog.media.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("blog.media-lifecycle")
public record MediaLifecycleProperties(
        Duration orphanGracePeriod,
        Duration deleteRetryBaseDelay,
        Duration staleDeleteTimeout,
        int cleanupBatchSize
) {
    public Duration safeOrphanGracePeriod() {
        return positive(orphanGracePeriod, Duration.ofHours(24));
    }

    public Duration safeDeleteRetryBaseDelay() {
        return positive(deleteRetryBaseDelay, Duration.ofMinutes(1));
    }

    public Duration safeStaleDeleteTimeout() {
        return positive(staleDeleteTimeout, Duration.ofMinutes(15));
    }

    public int safeCleanupBatchSize() {
        return cleanupBatchSize <= 0 ? 50 : Math.clamp(cleanupBatchSize, 1, 200);
    }

    private Duration positive(Duration value, Duration fallback) {
        return value == null || value.isZero() || value.isNegative() ? fallback : value;
    }
}
