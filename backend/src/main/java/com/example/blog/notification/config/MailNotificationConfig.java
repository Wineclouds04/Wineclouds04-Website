package com.example.blog.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MailNotificationProperties.class)
public class MailNotificationConfig {
}
