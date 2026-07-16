package com.example.blog.statistics.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SiteStatisticsUpdateRequest(
        @Min(0) @Max(1_000_000_000_000L) long onlineVisitors,
        @Min(0) @Max(1_000_000_000_000L) long todayViews,
        @Min(0) @Max(1_000_000_000_000L) long totalViews,
        @Min(0) @Max(1_000_000_000_000L) long totalVisitors
) {
}
