package com.example.blog.statistics.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.blog.interaction.service.VisitorContext.Visitor;
import com.example.blog.shared.config.RuntimeProperties;
import com.example.blog.shared.error.ApiException;
import com.example.blog.statistics.dto.PublicSiteStatistics;
import com.example.blog.statistics.dto.SiteStatisticsUpdateRequest;
import com.example.blog.statistics.mapper.ViewStatisticsMapper;
import org.springframework.http.HttpStatus;

@Service
public class SiteStatisticsService {

    private static final Duration ONLINE_WINDOW = Duration.ofMinutes(5);
    private static final Duration ONLINE_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;
    private final ViewStatisticsMapper mapper;
    private final RuntimeProperties runtime;
    private final SiteStatisticsOverrideService overrides;

    public SiteStatisticsService(
            StringRedisTemplate redis,
            ViewStatisticsMapper mapper,
            RuntimeProperties runtime,
            SiteStatisticsOverrideService overrides
    ) {
        this.redis = redis;
        this.mapper = mapper;
        this.runtime = runtime;
        this.overrides = overrides;
    }

    public PublicSiteStatistics snapshot(Visitor visitor) {
        return overrides.apply(rawSnapshot(visitor, true));
    }

    public PublicSiteStatistics adminSnapshot() {
        return overrides.apply(rawSnapshot(null, false));
    }

    public PublicSiteStatistics updateTargets(SiteStatisticsUpdateRequest request) {
        if (request.totalViews() < request.todayViews()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "总浏览量不能小于今日浏览量");
        }
        PublicSiteStatistics targets = new PublicSiteStatistics(
                request.onlineVisitors(),
                request.todayViews(),
                request.totalViews(),
                request.totalVisitors()
        );
        return overrides.updateTargets(targets, rawSnapshot(null, false));
    }

    private PublicSiteStatistics rawSnapshot(Visitor visitor, boolean recordOnline) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String dateValue = today.toString();

        long persistedTodayViews = mapper.findSiteViews(today);
        long persistedTodayVisitors = mapper.findSiteVisitors(today);
        long pendingViews = 0;
        long currentTodayVisitors = persistedTodayVisitors;
        long onlineVisitors = 0;

        try {
            pendingViews = readCounter(sitePvKey(dateValue));
            currentTodayVisitors = Math.max(
                    persistedTodayVisitors,
                    setSize(siteUniqueKey(dateValue))
            );
            onlineVisitors = recordOnline ? touchOnline(visitor.anonymousKeyHash()) : onlineCount();
        } catch (DataAccessException ignored) {
            // Persisted statistics remain available if Redis is temporarily unavailable.
        }

        long todayViews = persistedTodayViews + pendingViews;
        return new PublicSiteStatistics(
                onlineVisitors,
                todayViews,
                mapper.sumSiteViewsBefore(today) + todayViews,
                mapper.sumSiteVisitorsBefore(today) + currentTodayVisitors
        );
    }

    private long touchOnline(String visitorHash) {
        long now = Instant.now().toEpochMilli();
        String key = runtime.key("counter", "site:online");
        var online = redis.opsForZSet();
        online.add(key, visitorHash, now);
        online.removeRangeByScore(key, 0, now - ONLINE_WINDOW.toMillis());
        redis.expire(key, ONLINE_TTL);
        Long count = online.zCard(key);
        return count == null ? 0 : count;
    }

    private long onlineCount() {
        Long count = redis.opsForZSet().zCard(runtime.key("counter", "site:online"));
        return count == null ? 0 : count;
    }

    private long readCounter(String key) {
        String value = redis.opsForValue().get(key);
        if (value == null) return 0;
        try {
            return Math.max(0, Long.parseLong(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private long setSize(String key) {
        Long size = redis.opsForSet().size(key);
        return size == null ? 0 : size;
    }

    private String sitePvKey(String date) {
        return runtime.key("counter", "site:pv:" + date);
    }

    private String siteUniqueKey(String date) {
        return runtime.key("counter", "site:uv:" + date);
    }
}
