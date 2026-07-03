package com.example.blog.shared.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.blog.shared.config.RuntimeProperties;
import com.example.blog.shared.error.ApiException;

@Service
public class CaptchaService {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redis;
    private final RuntimeProperties runtime;
    private final RateLimitService security;
    private final SecureRandom random = new SecureRandom();

    public CaptchaService(
            StringRedisTemplate redis,
            RuntimeProperties runtime,
            RateLimitService security
    ) {
        this.redis = redis;
        this.runtime = runtime;
        this.security = security;
    }

    public CaptchaChallenge issue(String subject) {
        security.enforce("captcha", subject, 20, Duration.ofMinutes(10));
        int left = random.nextInt(2, 10);
        int right = random.nextInt(2, 10);
        String id = UUID.randomUUID().toString();
        try {
            redis.opsForValue().set(
                    key(id),
                    security.digest(id + ":" + (left + right)),
                    TTL
            );
        } catch (DataAccessException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "验证码服务暂时不可用");
        }
        return new CaptchaChallenge(id, "%d + %d = ?".formatted(left, right), Instant.now().plus(TTL));
    }

    public void verify(String id, String answer) {
        if (id == null || id.isBlank() || answer == null || answer.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请完成安全验证");
        }
        String expected;
        try {
            expected = redis.opsForValue().getAndDelete(key(id));
        } catch (DataAccessException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "验证码服务暂时不可用");
        }
        String actual = security.digest(id + ":" + answer.trim());
        if (expected == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "验证码无效或已过期");
        }
    }

    private String key(String id) {
        return runtime.key("captcha", id);
    }
}
