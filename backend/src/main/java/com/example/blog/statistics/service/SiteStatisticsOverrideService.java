package com.example.blog.statistics.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.blog.statistics.dto.PublicSiteStatistics;
import com.example.blog.statistics.mapper.SiteStatisticsOverrideMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SiteStatisticsOverrideService {

    private final SiteStatisticsOverrideMapper mapper;
    private final ObjectMapper objectMapper;

    public SiteStatisticsOverrideService(SiteStatisticsOverrideMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public PublicSiteStatistics apply(PublicSiteStatistics raw) {
        Adjustment adjustment = mapper.findOverrideJson()
                .flatMap(this::readAdjustment)
                .orElseGet(Adjustment::empty);
        return new PublicSiteStatistics(
                add(raw.onlineVisitors(), adjustment.onlineVisitors()),
                add(raw.todayViews(), adjustment.todayViews()),
                add(raw.totalViews(), adjustment.totalViews()),
                add(raw.totalVisitors(), adjustment.totalVisitors())
        );
    }

    public PublicSiteStatistics updateTargets(PublicSiteStatistics targets, PublicSiteStatistics raw) {
        Adjustment adjustment = new Adjustment(
                targets.onlineVisitors() - raw.onlineVisitors(),
                targets.todayViews() - raw.todayViews(),
                targets.totalViews() - raw.totalViews(),
                targets.totalVisitors() - raw.totalVisitors()
        );
        mapper.upsertOverrideJson(writeAdjustment(adjustment));
        return targets;
    }

    private Optional<Adjustment> readAdjustment(String json) {
        try {
            Adjustment adjustment = objectMapper.readValue(json, Adjustment.class);
            return Optional.ofNullable(adjustment);
        } catch (JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    private String writeAdjustment(Adjustment adjustment) {
        try {
            return objectMapper.writeValueAsString(adjustment);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法保存站点统计调整值", exception);
        }
    }

    private long add(long value, long adjustment) {
        try {
            return Math.max(0, Math.addExact(value, adjustment));
        } catch (ArithmeticException exception) {
            return adjustment > 0 ? Long.MAX_VALUE : 0;
        }
    }

    private record Adjustment(
            long onlineVisitors,
            long todayViews,
            long totalViews,
            long totalVisitors
    ) {
        static Adjustment empty() {
            return new Adjustment(0, 0, 0, 0);
        }
    }
}
