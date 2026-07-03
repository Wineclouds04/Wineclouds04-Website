package com.example.blog.interaction.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class VisitorContext {

    private static final String COOKIE_NAME = "blog_visitor";
    private final SecretKey secretKey;
    private final SecureRandom random = new SecureRandom();

    public VisitorContext(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public Visitor resolve(HttpServletRequest request, HttpServletResponse response) {
        String anonymousId = cookie(request, COOKIE_NAME);
        if (anonymousId == null || !anonymousId.matches("[A-Za-z0-9_-]{32,64}")) {
            byte[] bytes = new byte[24];
            random.nextBytes(bytes);
            anonymousId = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, anonymousId)
                    .httpOnly(true)
                    .secure(request.isSecure())
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(java.time.Duration.ofDays(365))
                    .build();
            response.addHeader("Set-Cookie", cookie.toString());
        }
        String userAgent = request.getHeader("User-Agent");
        return new Visitor(
                hmac(anonymousId),
                ipHash(request),
                summarize(userAgent)
        );
    }

    public String ipHash(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr()
                : forwarded.split(",")[0].trim();
        return hmac(ip == null ? "unknown" : ip);
    }

    public String subjectHash(String value) {
        return hmac(value);
    }

    private String cookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create visitor digest", exception);
        }
    }

    private String summarize(String value) {
        if (value == null || value.isBlank()) return "unknown";
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() <= 200 ? compact : compact.substring(0, 200);
    }

    public record Visitor(String anonymousKeyHash, String ipHash, String userAgentSummary) {
    }
}
