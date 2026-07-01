package com.example.blog.auth.controller;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.auth.config.AuthProperties;
import com.example.blog.auth.dto.AuthResponse;
import com.example.blog.auth.dto.LoginRequest;
import com.example.blog.auth.dto.PasswordChangeRequest;
import com.example.blog.auth.dto.UserResponse;
import com.example.blog.auth.service.AuthRequestGuard;
import com.example.blog.auth.service.AuthService;
import com.example.blog.auth.service.AuthService.AuthResult;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    public static final String REFRESH_COOKIE = "blog_refresh";
    public static final String CSRF_COOKIE = "blog_csrf";

    private final AuthService authService;
    private final AuthRequestGuard requestGuard;
    private final AuthProperties properties;

    public AuthController(
            AuthService authService,
            AuthRequestGuard requestGuard,
            AuthProperties properties
    ) {
        this.authService = authService;
        this.requestGuard = requestGuard;
        this.properties = properties;
    }

    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        requestGuard.validateOrigin(servletRequest);
        AuthResult result = authService.login(
                request.username(),
                request.password(),
                servletRequest.getHeader("User-Agent")
        );
        return withSessionCookies(result);
    }

    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
            @CookieValue(value = CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(value = "X-CSRF-Token", required = false) String csrfHeader,
            HttpServletRequest request
    ) {
        requestGuard.validateOrigin(request);
        requestGuard.validateCsrf(csrfHeader, csrfCookie);
        AuthResult result = authService.refresh(refreshToken, request.getHeader("User-Agent"));
        return withSessionCookies(result);
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
            @CookieValue(value = CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(value = "X-CSRF-Token", required = false) String csrfHeader,
            HttpServletRequest request
    ) {
        requestGuard.validateOrigin(request);
        requestGuard.validateCsrf(csrfHeader, csrfCookie);
        authService.logout(refreshToken);
        return clearSessionCookies();
    }

    @PostMapping("/logout-all")
    ResponseEntity<Void> logoutAll(
            @AuthenticationPrincipal Jwt jwt,
            @CookieValue(value = CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(value = "X-CSRF-Token", required = false) String csrfHeader,
            HttpServletRequest request
    ) {
        requestGuard.validateOrigin(request);
        requestGuard.validateCsrf(csrfHeader, csrfCookie);
        authService.revokeAll(jwt.getSubject());
        return clearSessionCookies();
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(jwt.getSubject());
    }

    @PutMapping("/password")
    ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PasswordChangeRequest request
    ) {
        authService.changePassword(jwt.getSubject(), request);
        return clearSessionCookies();
    }

    private ResponseEntity<AuthResponse> withSessionCookies(AuthResult result) {
        Duration maxAge = Duration.between(Instant.now(), result.refreshExpiresAt());
        ResponseCookie refresh = ResponseCookie.from(REFRESH_COOKIE, result.refreshToken())
                .httpOnly(true)
                .secure(properties.secureCookies())
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
        ResponseCookie csrf = ResponseCookie.from(CSRF_COOKIE, result.csrfToken())
                .httpOnly(false)
                .secure(properties.secureCookies())
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refresh.toString())
                .header(HttpHeaders.SET_COOKIE, csrf.toString())
                .body(result.response());
    }

    private ResponseEntity<Void> clearSessionCookies() {
        ResponseCookie refresh = expiredCookie(REFRESH_COOKIE, true, "/api/v1/auth");
        ResponseCookie csrf = expiredCookie(CSRF_COOKIE, false, "/");
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refresh.toString())
                .header(HttpHeaders.SET_COOKIE, csrf.toString())
                .build();
    }

    private ResponseCookie expiredCookie(String name, boolean httpOnly, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(httpOnly)
                .secure(properties.secureCookies())
                .sameSite("Strict")
                .path(path)
                .maxAge(Duration.ZERO)
                .build();
    }
}
