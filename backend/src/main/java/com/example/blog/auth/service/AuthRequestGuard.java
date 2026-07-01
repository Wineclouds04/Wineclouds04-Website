package com.example.blog.auth.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.example.blog.auth.config.AuthProperties;
import com.example.blog.shared.error.ApiException;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class AuthRequestGuard {

    private final AuthProperties properties;

    public AuthRequestGuard(AuthProperties properties) {
        this.properties = properties;
    }

    public void validateOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            String referer = request.getHeader("Referer");
            origin = referer == null || referer.isBlank() ? null : originOf(referer);
        }
        if (origin == null || properties.allowedOrigins().stream().noneMatch(origin::equals)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "请求来源不受信任");
        }
    }

    public void validateCsrf(String headerToken, String cookieToken) {
        if (headerToken == null || cookieToken == null || !MessageDigest.isEqual(
                headerToken.getBytes(StandardCharsets.UTF_8),
                cookieToken.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CSRF令牌无效");
        }
    }

    private String originOf(String value) {
        try {
            URI uri = URI.create(value);
            int port = uri.getPort();
            return uri.getScheme() + "://" + uri.getHost() + (port < 0 ? "" : ":" + port);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.FORBIDDEN, "请求来源格式无效");
        }
    }
}
