package com.example.blog.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.auth.config.AuthProperties;
import com.example.blog.auth.dto.AuthResponse;
import com.example.blog.auth.dto.PasswordChangeRequest;
import com.example.blog.auth.dto.UserResponse;
import com.example.blog.auth.mapper.AuthMapper;
import com.example.blog.auth.model.UserAccount;
import com.example.blog.shared.error.ApiException;

@Service
public class AuthService {

    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";
    private static final String USER_SESSIONS_PREFIX = "auth:user:";

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder refreshJwtDecoder;
    private final StringRedisTemplate redis;
    private final AuthProperties properties;

    public AuthService(
            AuthMapper authMapper,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Qualifier("refreshJwtDecoder") JwtDecoder refreshJwtDecoder,
            StringRedisTemplate redis,
            AuthProperties properties
    ) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.refreshJwtDecoder = refreshJwtDecoder;
        this.redis = redis;
        this.properties = properties;
    }

    @Transactional
    public AuthResult login(String username, String password, String userAgent) {
        UserAccount user = authMapper.findByUsername(username.trim())
                .filter(UserAccount::enabled)
                .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw invalidCredentials();
        }

        authMapper.updateLastLogin(user.id(), LocalDateTime.now(ZoneOffset.UTC));
        return issueSession(user, UUID.randomUUID().toString(), summarizeDevice(userAgent));
    }

    public AuthResult refresh(String rawRefreshToken, String userAgent) {
        Jwt refresh = decodeRefresh(rawRefreshToken);
        String userId = refresh.getSubject();
        String jti = refresh.getId();
        String sessionId = refresh.getClaimAsString("session_id");

        String stored;
        try {
            stored = redis.opsForValue().getAndDelete(refreshKey(jti));
            redis.opsForSet().remove(userSessionsKey(userId), jti);
        } catch (DataAccessException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "会话服务暂时不可用");
        }

        if (stored == null || !constantTimeEquals(stored, sessionValue(userId, sessionId, rawRefreshToken))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "刷新会话已失效");
        }

        UserAccount user = findUser(Long.valueOf(userId));
        return issueSession(user, sessionId, summarizeDevice(userAgent));
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        try {
            Jwt refresh = decodeRefresh(rawRefreshToken);
            redis.delete(refreshKey(refresh.getId()));
            redis.opsForSet().remove(userSessionsKey(refresh.getSubject()), refresh.getId());
        } catch (ApiException ignored) {
            // Logout remains idempotent even when the presented session is already invalid.
        }
    }

    public UserResponse me(String userId) {
        return UserResponse.from(findUser(Long.valueOf(userId)));
    }

    @Transactional
    public void changePassword(String userId, PasswordChangeRequest request) {
        UserAccount user = findUser(Long.valueOf(userId));
        if (!passwordEncoder.matches(request.currentPassword(), user.passwordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "当前密码不正确");
        }
        if (passwordEncoder.matches(request.newPassword(), user.passwordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "新密码不能与当前密码相同");
        }
        authMapper.updatePassword(
                user.id(),
                passwordEncoder.encode(request.newPassword()),
                LocalDateTime.now(ZoneOffset.UTC)
        );
        revokeAll(userId);
    }

    public void revokeAll(String userId) {
        String setKey = userSessionsKey(userId);
        try {
            var sessionIds = redis.opsForSet().members(setKey);
            if (sessionIds != null && !sessionIds.isEmpty()) {
                redis.delete(sessionIds.stream().map(this::refreshKey).toList());
            }
            redis.delete(setKey);
        } catch (DataAccessException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "会话服务暂时不可用");
        }
    }

    private AuthResult issueSession(UserAccount user, String sessionId, String deviceSummary) {
        Instant now = Instant.now();
        Instant accessExpiresAt = now.plus(properties.accessTtl());
        Instant refreshExpiresAt = now.plus(properties.refreshTtl());
        String refreshJti = UUID.randomUUID().toString();

        String accessToken = encode(user, now, accessExpiresAt, UUID.randomUUID().toString(), "access", sessionId);
        String refreshToken = encode(user, now, refreshExpiresAt, refreshJti, "refresh", sessionId);
        String csrfToken = randomToken();
        String value = sessionValue(user.id().toString(), sessionId, refreshToken) + "|" + deviceSummary;

        try {
            redis.opsForValue().set(
                    refreshKey(refreshJti),
                    value,
                    properties.refreshTtl()
            );
            String userSessionsKey = userSessionsKey(user.id().toString());
            redis.opsForSet().add(userSessionsKey, refreshJti);
            redis.expire(userSessionsKey, properties.refreshTtl());
        } catch (DataAccessException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "会话服务暂时不可用");
        }

        AuthResponse response = new AuthResponse(accessToken, accessExpiresAt, UserResponse.from(user));
        return new AuthResult(response, refreshToken, refreshExpiresAt, csrfToken);
    }

    private String encode(
            UserAccount user,
            Instant issuedAt,
            Instant expiresAt,
            String jti,
            String tokenType,
            String sessionId
    ) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .audience(java.util.List.of(properties.audience()))
                .subject(user.id().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(jti)
                .claim("role", user.role())
                .claim("token_type", tokenType)
                .claim("session_id", sessionId)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private Jwt decodeRefresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "缺少刷新会话");
        }
        try {
            return refreshJwtDecoder.decode(rawToken);
        } catch (JwtException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "刷新会话无效或已过期");
        }
    }

    private UserAccount findUser(Long id) {
        return authMapper.findById(id)
                .filter(UserAccount::enabled)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "管理员账号不可用"));
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
    }

    private String refreshKey(String jti) {
        return REFRESH_KEY_PREFIX + jti;
    }

    private String userSessionsKey(String userId) {
        return USER_SESSIONS_PREFIX + userId + ":sessions";
    }

    private String sessionValue(String userId, String sessionId, String token) {
        return userId + "|" + sessionId + "|" + sha256(token);
    }

    private String summarizeDevice(String userAgent) {
        return sha256(userAgent == null ? "unknown" : userAgent).substring(0, 16);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private boolean constantTimeEquals(String stored, String expectedPrefix) {
        String storedPrefix = stored.substring(0, stored.lastIndexOf('|'));
        return MessageDigest.isEqual(
                storedPrefix.getBytes(StandardCharsets.UTF_8),
                expectedPrefix.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String randomToken() {
        byte[] bytes = new byte[24];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record AuthResult(
            AuthResponse response,
            String refreshToken,
            Instant refreshExpiresAt,
            String csrfToken
    ) {
    }
}
