package com.example.blog.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties("blog.cos")
public record CosProperties(
        String region,
        String bucket,
        String secretId,
        String secretKey,
        String cdnDomain,
        String objectPrefix,
        long maxImageSize,
        int maxImageWidth,
        int maxImageHeight
) {
    public boolean configured() {
        return StringUtils.hasText(region)
                && StringUtils.hasText(bucket)
                && StringUtils.hasText(secretId)
                && StringUtils.hasText(secretKey);
    }
}
