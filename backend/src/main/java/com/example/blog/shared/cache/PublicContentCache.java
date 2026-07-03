package com.example.blog.shared.cache;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.blog.shared.config.RuntimeProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class PublicContentCache {

    private static final int VERSION = 1;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final RuntimeProperties properties;
    private final CacheMetrics metrics;

    public PublicContentCache(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            RuntimeProperties properties,
            CacheMetrics metrics
    ) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.metrics = metrics;
    }

    public <T> T get(
            String businessKey,
            Duration ttl,
            Class<T> type,
            Supplier<T> loader
    ) {
        return getInternal(
                businessKey,
                ttl,
                node -> objectMapper.treeToValue(node, type),
                loader
        );
    }

    public <T> T get(
            String businessKey,
            Duration ttl,
            TypeReference<T> type,
            Supplier<T> loader
    ) {
        return getInternal(
                businessKey,
                ttl,
                node -> objectMapper.convertValue(node, type),
                loader
        );
    }

    public void invalidateAll() {
        if (!properties.cacheEnabled()) return;
        String pattern = properties.key("cache", "v" + VERSION + ":*");
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redis.scan(
                ScanOptions.scanOptions().match(pattern).count(200).build()
        )) {
            cursor.forEachRemaining(keys::add);
            if (!keys.isEmpty()) redis.delete(keys);
        } catch (DataAccessException exception) {
            metrics.error();
        }
    }

    private <T> T getInternal(
            String businessKey,
            Duration ttl,
            JsonReader<T> reader,
            Supplier<T> loader
    ) {
        if (!properties.cacheEnabled()) return loader.get();
        String key = properties.key("cache", "v" + VERSION + ":" + businessKey);
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                JsonNode envelope = objectMapper.readTree(cached);
                if (envelope.path("version").asInt() == VERSION && envelope.has("data")) {
                    metrics.hit();
                    return reader.read(envelope.get("data"));
                }
                redis.delete(key);
            }
        } catch (Exception exception) {
            metrics.error();
            try {
                redis.delete(key);
            } catch (DataAccessException ignored) {
                // Cache failure never prevents serving public content from MySQL.
            }
        }

        metrics.miss();
        T loaded = loader.get();
        try {
            ObjectNode envelope = objectMapper.createObjectNode();
            envelope.put("version", VERSION);
            envelope.set("data", objectMapper.valueToTree(loaded));
            redis.opsForValue().set(key, objectMapper.writeValueAsString(envelope), jitter(ttl));
        } catch (Exception exception) {
            metrics.error();
        }
        return loaded;
    }

    private Duration jitter(Duration base) {
        long millis = Math.max(1000, base.toMillis());
        long spread = Math.max(1, millis / 10);
        return Duration.ofMillis(millis + ThreadLocalRandom.current().nextLong(-spread, spread + 1));
    }

    @FunctionalInterface
    private interface JsonReader<T> {
        T read(JsonNode node) throws Exception;
    }
}
