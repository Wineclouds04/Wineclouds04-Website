package com.example.blog.shared.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.blog.shared.config.RuntimeProperties;
import com.example.blog.shared.error.ApiException;

@Service
public class RateLimitService {

    private final StringRedisTemplate redis;
    private final RuntimeProperties runtime;
    private final SecretKey secretKey;

    public RateLimitService(
            StringRedisTemplate redis,
            RuntimeProperties runtime,
            SecretKey secretKey
    ) {
        this.redis = redis;
        this.runtime = runtime;
        this.secretKey = secretKey;
    }

    public void enforce(String scope, String subject, long limit, Duration window) {
        long windowSeconds = Math.max(1, window.toSeconds());
        long bucket = Instant.now().getEpochSecond() / windowSeconds;
        String key = runtime.key("rate", scope + ":" + digest(subject) + ":" + bucket);
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                redis.expire(key, window.plusSeconds(5));
            }
            if (count != null && count > limit) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "操作过于频繁，请稍后再试");
            }
        } catch (DataAccessException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "风控服务暂时不可用");
        }
    }

    public String digest(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
            return HexFormat.of().formatHex(
                    mac.doFinal(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create security digest", exception);
        }
    }
}
