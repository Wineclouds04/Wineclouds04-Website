package com.example.blog.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties("blog.oss")
public record OssProperties(
        String region,
        String endpoint,
        String bucket,
        String cdnDomain,
        String objectPrefix,
        long maxImageSize,
        int maxImageWidth,
        int maxImageHeight
) {
    public boolean configured() {
        return StringUtils.hasText(region)
                && StringUtils.hasText(bucket)
                && StringUtils.hasText(System.getenv("OSS_ACCESS_KEY_ID"))
                && StringUtils.hasText(System.getenv("OSS_ACCESS_KEY_SECRET"));
    }
}
