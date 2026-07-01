package com.example.blog.auth.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("blog.auth")
public record AuthProperties(
        String issuer,
        String audience,
        String jwtSecret,
        Duration accessTtl,
        Duration refreshTtl,
        boolean secureCookies,
        List<String> allowedOrigins,
        InitialAdmin initialAdmin
) {
    public record InitialAdmin(String username, String password, String nickname) {
    }
}
