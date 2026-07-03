package com.example.blog.statistics.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.blog.interaction.service.VisitorContext.Visitor;
import com.example.blog.shared.cache.PublicContentCache;
import com.example.blog.shared.config.RuntimeProperties;
import com.example.blog.shared.error.ApiException;
import com.example.blog.statistics.dto.ViewCountResponse;
import com.example.blog.statistics.mapper.ViewStatisticsMapper;

@Service
public class ViewCounterService {

    private static final Duration COUNTER_TTL = Duration.ofDays(3);
    private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redis;
    private final ViewStatisticsMapper mapper;
    private final RuntimeProperties runtime;
    private final PublicContentCache publicCache;

    public ViewCounterService(
            StringRedisTemplate redis,
            ViewStatisticsMapper mapper,
            RuntimeProperties runtime,
            PublicContentCache publicCache
    ) {
        this.redis = redis;
        this.mapper = mapper;
        this.runtime = runtime;
        this.publicCache = publicCache;
    }

    public ViewCountResponse record(Long articleId, Visitor visitor, String referer) {
        if (!mapper.articleIsPublic(articleId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "文章不存在");
        }
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        String dateValue = date.toString();
        try {
            increment(articlePvKey(articleId, dateValue));
            addUnique(articleUniqueKey(articleId, dateValue), visitor.anonymousKeyHash());
            redis.opsForSet().add(articleDirtyKey(dateValue), articleId.toString());
            redis.expire(articleDirtyKey(dateValue), COUNTER_TTL.plusDays(1));

            increment(sitePvKey(dateValue));
            addUnique(siteUniqueKey(dateValue), visitor.anonymousKeyHash());
            increment(siteSourceKey(dateValue, source(referer)));
            redis.opsForSet().add(siteDirtyDatesKey(), dateValue);
        } catch (DataAccessException ignored) {
            // Statistics are best-effort and never block reading the article.
            return new ViewCountResponse(false);
        }
        return new ViewCountResponse(true);
    }

    @Scheduled(fixedDelayString = "${blog.statistics.flush-interval:60000}")
    public void flush() {
        String lockKey = runtime.key("lock", "stats-flush");
        String token = UUID.randomUUID().toString();
        Boolean acquired;
        try {
            acquired = redis.opsForValue().setIfAbsent(lockKey, token, Duration.ofSeconds(55));
        } catch (DataAccessException exception) {
            return;
        }
        if (!Boolean.TRUE.equals(acquired)) return;
        try {
            flushDirtyDates();
        } finally {
            try {
                redis.execute(RELEASE_LOCK, List.of(lockKey), token);
            } catch (DataAccessException ignored) {
                // The lock has a short TTL and will self-heal.
            }
        }
    }

    protected void flushDirtyDates() {
        Set<String> dates = redis.opsForSet().members(siteDirtyDatesKey());
        if (dates == null) return;
        boolean contentChanged = false;
        LocalDate cutoff = LocalDate.now(ZoneOffset.UTC).minusDays(3);
        for (String dateValue : dates) {
            LocalDate date;
            try {
                date = LocalDate.parse(dateValue);
            } catch (RuntimeException exception) {
                redis.opsForSet().remove(siteDirtyDatesKey(), dateValue);
                continue;
            }
            contentChanged |= flushArticles(date, dateValue);
            flushSite(date, dateValue);
            if (date.isBefore(cutoff)) {
                redis.opsForSet().remove(siteDirtyDatesKey(), dateValue);
            }
        }
        if (contentChanged) publicCache.invalidateAll();
    }

    private boolean flushArticles(LocalDate date, String dateValue) {
        Set<String> ids = redis.opsForSet().members(articleDirtyKey(dateValue));
        if (ids == null) return false;
        boolean changed = false;
        for (String value : ids) {
            Long articleId;
            try {
                articleId = Long.valueOf(value);
            } catch (NumberFormatException exception) {
                redis.opsForSet().remove(articleDirtyKey(dateValue), value);
                continue;
            }
            String pvKey = articlePvKey(articleId, dateValue);
            long delta = drain(pvKey);
            long unique = setSize(articleUniqueKey(articleId, dateValue));
            if (delta == 0) continue;
            try {
                mapper.addArticleViews(articleId, date, delta, unique);
                mapper.addArticleTotal(articleId, delta);
                changed = true;
            } catch (RuntimeException exception) {
                restore(pvKey, delta);
            }
        }
        return changed;
    }

    private void flushSite(LocalDate date, String dateValue) {
        String pvKey = sitePvKey(dateValue);
        String directKey = siteSourceKey(dateValue, "direct");
        String searchKey = siteSourceKey(dateValue, "search");
        String referralKey = siteSourceKey(dateValue, "referral");
        long views = drain(pvKey);
        long direct = drain(directKey);
        long search = drain(searchKey);
        long referral = drain(referralKey);
        long unique = setSize(siteUniqueKey(dateValue));
        if (views == 0) return;
        try {
            mapper.addSiteViews(date, views, unique, direct, search, referral);
        } catch (RuntimeException exception) {
            restore(pvKey, views);
            restore(directKey, direct);
            restore(searchKey, search);
            restore(referralKey, referral);
        }
    }

    private void increment(String key) {
        redis.opsForValue().increment(key);
        redis.expire(key, COUNTER_TTL);
    }

    private void addUnique(String key, String visitorHash) {
        redis.opsForSet().add(key, visitorHash);
        redis.expire(key, COUNTER_TTL);
    }

    private long drain(String key) {
        String value = redis.opsForValue().getAndSet(key, "0");
        if (value == null) return 0;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void restore(String key, long value) {
        if (value > 0) redis.opsForValue().increment(key, value);
    }

    private long setSize(String key) {
        Long size = redis.opsForSet().size(key);
        return size == null ? 0 : size;
    }

    private String source(String referer) {
        if (referer == null || referer.isBlank()) return "direct";
        String value = referer.toLowerCase();
        if (value.contains("google.") || value.contains("bing.")
                || value.contains("baidu.") || value.contains("duckduckgo.")) {
            return "search";
        }
        return "referral";
    }

    private String articlePvKey(Long id, String date) {
        return runtime.key("counter", "view:pv:" + id + ":" + date);
    }

    private String articleUniqueKey(Long id, String date) {
        return runtime.key("counter", "view:uv:" + id + ":" + date);
    }

    private String articleDirtyKey(String date) {
        return runtime.key("counter", "view:dirty:" + date);
    }

    private String sitePvKey(String date) {
        return runtime.key("counter", "site:pv:" + date);
    }

    private String siteUniqueKey(String date) {
        return runtime.key("counter", "site:uv:" + date);
    }

    private String siteSourceKey(String date, String source) {
        return runtime.key("counter", "site:source:" + source + ":" + date);
    }

    private String siteDirtyDatesKey() {
        return runtime.key("counter", "site:dirty-dates");
    }
}
