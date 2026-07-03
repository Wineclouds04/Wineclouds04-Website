package com.example.blog.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("blog.mail")
public record MailNotificationProperties(
        boolean enabled,
        String from,
        String siteUrl,
        int maxRetries
) {
}
