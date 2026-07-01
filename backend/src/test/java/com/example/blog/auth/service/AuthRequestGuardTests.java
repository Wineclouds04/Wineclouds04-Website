package com.example.blog.auth.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.example.blog.auth.config.AuthProperties;
import com.example.blog.shared.error.ApiException;

class AuthRequestGuardTests {

    private AuthRequestGuard guard;

    @BeforeEach
    void setUp() {
        var properties = new AuthProperties(
                "personal-blog",
                "personal-blog-admin",
                "a-secret-long-enough-for-the-test-suite",
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                false,
                List.of("http://admin.localhost", "http://localhost:5173"),
                new AuthProperties.InitialAdmin("", "", "")
        );
        guard = new AuthRequestGuard(properties);
    }

    @Test
    void acceptsConfiguredOrigin() {
        var request = new MockHttpServletRequest();
        request.addHeader("Origin", "http://localhost:5173");

        assertDoesNotThrow(() -> guard.validateOrigin(request));
    }

    @Test
    void rejectsUnknownOrigin() {
        var request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://attacker.example");

        assertThrows(ApiException.class, () -> guard.validateOrigin(request));
    }

    @Test
    void rejectsMissingOriginAndReferer() {
        assertThrows(ApiException.class, () -> guard.validateOrigin(new MockHttpServletRequest()));
    }

    @Test
    void requiresMatchingCsrfTokens() {
        assertDoesNotThrow(() -> guard.validateCsrf("same-token", "same-token"));
        assertThrows(ApiException.class, () -> guard.validateCsrf("one-token", "another-token"));
        assertThrows(ApiException.class, () -> guard.validateCsrf(null, "cookie-token"));
    }
}
